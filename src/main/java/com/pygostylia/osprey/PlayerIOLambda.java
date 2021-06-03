package com.pygostylia.osprey;

import java.io.IOException;

@FunctionalInterface
public interface PlayerIOLambda {
    void apply(Player player) throws IOException;
}
