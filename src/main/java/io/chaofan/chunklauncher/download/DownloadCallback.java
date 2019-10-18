package io.chaofan.chunklauncher.download;

public interface DownloadCallback {

    void downloadDone(Downloadable d, boolean succeed, boolean queueEmpty);

    void downloadStart(Downloadable d);

}
