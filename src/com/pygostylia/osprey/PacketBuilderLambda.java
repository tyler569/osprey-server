package com.pygostylia.osprey;

import java.io.IOException;

@FunctionalInterface
public interface PacketBuilderLambda {
    void apply(PacketBuilder p) throws IOException;
}
