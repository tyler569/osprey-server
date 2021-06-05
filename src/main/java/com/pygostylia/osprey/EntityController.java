package com.pygostylia.osprey;

import java.util.concurrent.*;

public class EntityController implements Runnable {
    DelayQueue<ScheduledFuture<?>> events = new DelayQueue<>();
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public void submit(Runnable event, int time, TimeUnit unit) {
        ScheduledFuture<?> future = executor.schedule(event, time, unit);
        events.add(future);
    }

    public ScheduledFuture<?> submitForEachTick(Runnable event) {
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(event, 50, 50, TimeUnit.MILLISECONDS);
        events.add(future);
        return future;
    }

    public void run() {
        for (var event : events) {
            try {
                event.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
