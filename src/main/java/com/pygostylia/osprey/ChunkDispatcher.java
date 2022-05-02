package com.pygostylia.osprey;

import com.pygostylia.osprey.entities.Player;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

public class ChunkDispatcher implements Runnable {
    record Pair(Player player, ChunkPosition chunkPosition) {}

    BlockingQueue<Pair> queue = new LinkedBlockingQueue<>();

    public void run() {
        Pair pair;
        while (true) {
            try {
                pair = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            pair.player().sendChunk(pair.chunkPosition());
        }
    }

    public void dispatch(Player player, Stream<ChunkPosition> locations) {
        locations.forEachOrdered((location) -> queue.add(new Pair(player, location)));
    }
}
