package com.pygostylia.osprey;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.RunnableScheduledFuture;

public class EntityController implements Runnable {
    DelayQueue<RunnableScheduledFuture<Void>> events;

    public void run() {
        for (var event : events) {
            event.run();
        }
    }

    public void submit(RunnableScheduledFuture<Void> event) {
        events.add(event);
    }
}
