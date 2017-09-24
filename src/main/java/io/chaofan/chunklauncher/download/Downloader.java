package io.chaofan.chunklauncher.download;

import java.util.LinkedList;
import java.util.Queue;

public class Downloader extends Thread {

    private static boolean downloadStop = false;

    private boolean stopAfterAllDone = false;

    private boolean forceStopped = false;

    private Queue<Downloadable> downloading = new LinkedList<Downloadable>();

    @Override
    public void run() {
        while(!downloadStop && !forceStopped && !(stopAfterAllDone && downloading.isEmpty())) {
            if(!downloading.isEmpty()) {
                Downloadable todown = downloading.poll();
                todown.download(this);
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
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

