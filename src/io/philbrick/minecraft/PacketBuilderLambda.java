package io.philbrick.minecraft;

import java.io.IOException;

@FunctionalInterface
public interface PacketBuilderLambda {
    void apply(PacketBuilder p) throws IOException;
}
