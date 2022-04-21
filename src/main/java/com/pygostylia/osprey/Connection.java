package com.pygostylia.osprey;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class Connection {
    private final Socket socket;
    private InputStream instream;
    private OutputStream outstream;
    private boolean encryptionEnabled;
    private int maxUncompressedPacket;
    private boolean compressionEnabled;
    private long lastKeepAlive;
    private boolean established;
    private Instant lastKeepAliveTime = Instant.now();
    private final static Duration keepAliveInterval = Duration.ofSeconds(5);
    private final static Random rng = new Random();
    boolean debug;
    int lastPacketType;

    Connection(Socket s) throws IOException {
        socket = s;
        instream = new BufferedInputStream(socket.getInputStream());
        outstream = new BufferedOutputStream(socket.getOutputStream());
        socket.setSoTimeout(5050);
    }

    boolean isClosed() {
        return socket.isClosed();
    }

    Packet readPacket() throws IOException {
        byte[] data;
        int originalLen;
        int compressedLen;

        while (true) {
            if (shouldKeepAlive()) {
                sendKeepAlive();
            }
            try {
                originalLen = VarInt.read(instream);
                data = new byte[originalLen];
                instream.readNBytes(data, 0, originalLen);
                break;
            } catch (SocketTimeoutException ignored) {
            }
        }
        if (compressionEnabled) {
            compressedLen = originalLen;
            var stream = new ByteArrayInputStream(data);
            originalLen = VarInt.read(stream);
            if (originalLen == 0) {
                data = stream.readAllBytes();
                originalLen = compressedLen;
            } else {
                var inflater = new InflaterInputStream(stream);
                data = inflater.readAllBytes();
                if (data.length != originalLen) {
                    System.err.printf("Packet had unexpected length: %d %d%n", originalLen, data.length);
                }
            }
        }
        return new Packet(data, originalLen);
    }

    void sendPacket(int type, PacketBuilderLambda closure) throws IOException {
        var m = new MinecraftOutputStream();
        m.writeVarInt(type);
        closure.apply(m);
        if (!compressionEnabled) {
            var data = m.toByteArray();
            synchronized (socket) {
                VarInt.write(outstream, data.length);
                outstream.write(data);
                outstream.flush();
            }
        } else {
            if (m.size() > maxUncompressedPacket) {
                var originalSize = m.size();
                var inner = new ByteArrayOutputStream();
                var stream = new DeflaterOutputStream(inner);
                stream.write(m.toByteArray());
                stream.finish();
                var compressedSize = inner.size();
                synchronized (socket) {
                    Protocol.writeVarInt(outstream, compressedSize + VarInt.len(originalSize));
                    Protocol.writeVarInt(outstream, originalSize);

                    outstream.write(inner.toByteArray());
                    outstream.flush();
                    lastPacketType = type;
                }
            } else {
                var data = m.toByteArray();
                synchronized (socket) {
                    VarInt.write(outstream, data.length + VarInt.len(0));
                    VarInt.write(outstream, 0);
                    outstream.write(data);
                    outstream.flush();
                    lastPacketType = type;
                }
            }
        }
    }


    void setEncryption(byte[] secret)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException {
        var protocolKey = new SecretKeySpec(secret, "AES");
        var protocolIv = new IvParameterSpec(secret);
        var protocolCipherOut = Cipher.getInstance("AES/CFB8/NoPadding");
        protocolCipherOut.init(Cipher.ENCRYPT_MODE, protocolKey, protocolIv);
        var protocolCipherIn = Cipher.getInstance("AES/CFB8/NoPadding");
        protocolCipherIn.init(Cipher.DECRYPT_MODE, protocolKey, protocolIv);

        instream = new CipherInputStream(instream, protocolCipherIn);
        outstream = new CipherOutputStream(outstream, protocolCipherOut);
        encryptionEnabled = true;
        established = true;
    }

    void setCompression(int maxPacket) {
        maxUncompressedPacket = maxPacket;
        compressionEnabled = true;
    }

    void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    // ================ persistence

    void sendKeepAlive() throws IOException {
        lastKeepAlive = rng.nextLong();
        sendPacket(0x1F, (p) -> p.writeLong(lastKeepAlive));
        lastKeepAliveTime = Instant.now();
    }

    boolean validateKeepAlive(long value) {
        return lastKeepAlive == value;
    }

    Duration pingTime() {
        return Duration.between(lastKeepAliveTime, Instant.now());
    }

    boolean shouldKeepAlive() {
        return established && pingTime().compareTo(keepAliveInterval) > 0;
    }

    boolean isEstablished() {
        return established;
    }

    // ================= packets
}
