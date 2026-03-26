package edu.hitsz.aircraftwar.android.network;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NetworkExecutor {
    private static final ExecutorService IO = Executors.newCachedThreadPool();

    private NetworkExecutor() {
    }

    public static void run(Runnable task) {
        IO.execute(task);
    }
}
