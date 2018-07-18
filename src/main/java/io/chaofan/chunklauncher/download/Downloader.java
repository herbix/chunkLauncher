package io.chaofan.chunklauncher.download;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Downloader extends Thread {

    private static boolean downloadStop = false;

    private boolean stopAfterAllDone = false;

    private boolean forceStopped = false;

    private final LinkedList<Downloadable> downloading = new LinkedList<Downloadable>();

    @Override
    public void run() {
        ThreadPoolExecutor exec = new ThreadPoolExecutor(10, 20, 20,
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(5), new ThreadPoolExecutor.CallerRunsPolicy());
        while(!downloadStop && !forceStopped && !(stopAfterAllDone && downloading.isEmpty())) {
            if(!downloading.isEmpty()) {
                final Downloadable todown = downloading.poll();
                exec.execute(new Runnable() {
                    @Override
                    public void run() {
                        todown.download(Downloader.this);
                    }
                });
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }

        try {
            exec.awaitTermination(100000, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        exec.shutdown();
    }

    public void addDownload(Downloadable d) {
        synchronized(downloading) {
            downloading.add(d);
        }
    }

    public void stopAfterAllDone() {
        stopAfterAllDone = true;
    }

    public void forceStop() {
        forceStopped = true;
    }

    public boolean queueEmpty() {
        return downloading.isEmpty();
    }

    public static void stopAll() {
        downloadStop = true;
    }

    public int downloadCount() {
        return downloading.size();
    }
}

