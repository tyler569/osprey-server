package io.philbrick.minecraft;

import java.util.*;
import java.util.concurrent.*;

public class Reaper {
    private final BlockingQueue<Thread> deadThreads;

    Reaper() {
        deadThreads = new LinkedBlockingDeque<>();
        new Thread(this::reap).start();
    }

    private void reap() {
        while (true) {
            try {
                var deadThread = deadThreads.take();
                System.out.format("reap %s%n", deadThread);
                deadThread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void appendDeadThread(Thread thread) {
        deadThreads.add(thread);
    }
}
