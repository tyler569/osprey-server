package com.pygostylia.osprey;

import com.pygostylia.osprey.streams.MinecraftOutputStream;

import java.io.IOException;

@FunctionalInterface
public interface PacketBuilderLambda {
    void apply(MinecraftOutputStream p) throws IOException;
}
