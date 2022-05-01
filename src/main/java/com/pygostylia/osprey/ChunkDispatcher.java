package com.pygostylia.osprey;

import com.pygostylia.osprey.entities.Player;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

public class ChunkDispatcher implements Runnable {
    static record Pair(Player player, ChunkLocation chunkLocation) {
    }

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
            pair.player().sendChunk(pair.chunkLocation());
        }
    }

    public void dispatch(Player player, Stream<ChunkLocation> locations) {
        locations.forEachOrdered((location) -> queue.add(new Pair(player, location)));
    }
}
