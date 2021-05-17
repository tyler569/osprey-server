package io.philbrick.minecraft;

import java.io.IOException;
import java.io.OutputStream;

@FunctionalInterface
public interface PacketBuilder {
    void apply(OutputStream t) throws IOException;
}
