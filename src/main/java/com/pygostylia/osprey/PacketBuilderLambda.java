package com.pygostylia.osprey;

import java.io.IOException;

@FunctionalInterface
public interface PacketBuilderLambda {
    void apply(MinecraftOutputStream p) throws IOException;
}
