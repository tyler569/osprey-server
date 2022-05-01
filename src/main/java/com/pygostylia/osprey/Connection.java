package com.pygostylia.osprey;

import com.pygostylia.osprey.clientboundpacket.ClientBoundPacket;

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
    public int lastPacketType;

    public Connection(Socket s) throws IOException {
        socket = s;
        instream = new BufferedInputStream(socket.getInputStream());
        outstream = new BufferedOutputStream(socket.getOutputStream());
        socket.setSoTimeout(5050);
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public MinecraftInputStream readPacket() throws IOException {
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
                    System.err.printf("MinecraftInputStream had unexpected length: %d %d%n", originalLen, data.length);
                }
            }
        }
        MinecraftInputStream packet = new MinecraftInputStream(data, originalLen);
        packet.type = packet.readVarInt();
        return packet;
    }

    private void sendPacketRaw(byte[] data) throws IOException {
        if (!compressionEnabled) {
            synchronized (socket) {
                VarInt.write(outstream, data.length);
                outstream.write(data);
                outstream.flush();
            }
        } else {
            if (data.length > maxUncompressedPacket) {
                var originalSize = data.length;
                var inner = new ByteArrayOutputStream();
                var stream = new DeflaterOutputStream(inner);
                stream.write(data);
                stream.finish();
                var compressedSize = inner.size();
                synchronized (socket) {
                    Protocol.writeVarInt(outstream, compressedSize + VarInt.len(originalSize));
                    Protocol.writeVarInt(outstream, originalSize);

                    outstream.write(inner.toByteArray());
                    outstream.flush();
                }
            } else {
                synchronized (socket) {
                    VarInt.write(outstream, data.length + VarInt.len(0));
                    VarInt.write(outstream, 0);
                    outstream.write(data);
                    outstream.flush();
                }
            }
        }

    }

    public void sendPacket(int type, PacketBuilderLambda closure) throws IOException {
        var m = new MinecraftOutputStream();
        m.writeVarInt(type);
        closure.apply(m);
        sendPacketRaw(m.toByteArray());
        lastPacketType = type;
    }

    public void sendPacket(int type, ClientBoundPacket p) throws IOException {
        var m = new MinecraftOutputStream();
        m.writeVarInt(type);
        p.encode(m);
        sendPacketRaw(m.toByteArray());
        lastPacketType = type;
    }


    public void setEncryption(byte[] secret)
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

    public void setCompression(int maxPacket) {
        maxUncompressedPacket = maxPacket;
        compressionEnabled = true;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    // ================ persistence

    public void sendKeepAlive() throws IOException {
        lastKeepAlive = rng.nextLong();
        sendPacket(0x1F, (p) -> p.writeLong(lastKeepAlive));
        lastKeepAliveTime = Instant.now();
    }

    public boolean validateKeepAlive(long value) {
        return lastKeepAlive == value;
    }

    public Duration pingTime() {
        return Duration.between(lastKeepAliveTime, Instant.now());
    }

    public boolean shouldKeepAlive() {
        return established && pingTime().compareTo(keepAliveInterval) > 0;
    }

    public boolean isEstablished() {
        return established;
    }

    // ================= packets
}
