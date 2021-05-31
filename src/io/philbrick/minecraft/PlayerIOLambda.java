package io.philbrick.minecraft;

import java.io.*;

@FunctionalInterface
public interface PlayerIOLambda {
    void apply(Player player) throws IOException;
}
