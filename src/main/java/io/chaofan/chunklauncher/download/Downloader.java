package io.chaofan.chunklauncher.download;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Downloader implements Runnable {

    public static final int NOT_START = 0;
    public static final int RUNNING = 1;
    public static final int STOPPING = 2;
    public static final int STOPPED = 2;

    private static boolean downloadStop = false;

    private boolean stopAfterAllDone = false;

    private final LinkedList<Downloadable> downloading = new LinkedList<Downloadable>();
    private final List<Thread> threads = new ArrayList<Thread>();
    private volatile int state = NOT_START;

    private final int threadCountLimit;

    public Downloader() {
        threadCountLimit = 20;
    }

    @Override
    public void run() {
        while(!downloadStop && state == RUNNING) {
            Downloadable todown;
            synchronized (downloading) {
                if (downloading.isEmpty()) {
                    break;
                }
                todown = downloading.poll();
            }

            todown.download(this);
        }

        if (downloadStop) {
            state = STOPPING;
        }
    }

    public void start() {
        if (state == NOT_START) {
            state = RUNNING;
            synchronized (downloading) {
                if (!downloading.isEmpty()) {
                    startThreadIfNeeded();
                }
            }
        } else {
            throw new IllegalStateException("state != NOT_START");
        }
    }

    public void addDownload(Downloadable d) {
        if (state > RUNNING) {
            throw new IllegalStateException("state != RUNNING | NOT_START");
        }

        synchronized(downloading) {
            downloading.add(d);
            if (state == RUNNING) {
                startThreadIfNeeded();
            }
        }
    }

    private void startThreadIfNeeded() {
        synchronized (threads) {
            if (threads.size() < threadCountLimit) {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        Downloader.this.run();
                        synchronized (downloading) {
                            synchronized (threads) {
                                threads.remove(this);
                                if (state == STOPPING && threads.isEmpty()) {
                                    state = STOPPED;
                                }
                                if (stopAfterAllDone && downloading.isEmpty() && threads.isEmpty()) {
                                    state = STOPPED;
                                }
                            }
                        }
                    }
                };
                threads.add(thread);
                thread.start();
            }
        }
    }

    public void stopAfterAllDone() {
        stopAfterAllDone = true;
    }

    public void forceStop() {
        if (state <= STOPPING) {
            state = STOPPING;
        } else {
            throw new IllegalStateException("state == STOPPED");
        }
    }

    public int unfinishedTaskCount() {
        synchronized (downloading) {
            synchronized (threads) {
                return downloading.size() + threads.size();
            }
        }
    }

    public static void stopAll() {
        downloadStop = true;
    }

    public int downloadCount() {
        return downloading.size() + threads.size();
    }

    public int getState() {
        return state;
    }
}

