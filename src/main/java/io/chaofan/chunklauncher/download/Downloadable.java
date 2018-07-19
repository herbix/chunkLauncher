package io.chaofan.chunklauncher.download;

import java.io.IOException;

import io.chaofan.chunklauncher.util.HttpFetcher;
import io.chaofan.chunklauncher.util.Lang;

public class Downloadable {
    private String url = null;
    private String saveFilePath = null;
    private DownloadCallback callback = null;
    private String downloaded = null;

    public Downloadable(String url, String saveFilePath) {
        this(url, saveFilePath, null);
    }

    public Downloadable(String url) {
        this(url, null, null);
    }

    public Downloadable(String url, DownloadCallback callback) {
        this(url, null, callback);
    }

    public Downloadable(String url, String saveFilePath, DownloadCallback callback) {
        this.url = url;
        this.saveFilePath = saveFilePath;
        this.callback = callback;
    }

    public String getDownloaded() {
        return downloaded;
    }

    public String getSavedFile() {
        return saveFilePath;
    }

    public void download(Downloader downloader) {
        if(url == null) {
            return;
        }
        boolean succeed;
        if(callback != null) {
            callback.downloadStart(this);
        }
        System.out.println(Lang.getString("msg.download.start") + url);
        if(saveFilePath == null) {
            downloaded = HttpFetcher.fetch(url);
            succeed = (downloaded != null);
        } else {
            try {
                succeed = HttpFetcher.fetchAndSave(url, saveFilePath);
            } catch (IOException e) {
                succeed = false;
            }
        }
        if(succeed)
            System.out.println(Lang.getString("msg.download.succeeded") + url);
        else
            System.out.println(Lang.getString("msg.download.failed") + url);
        if(callback != null) {
            callback.downloadDone(this, succeed, downloader.unfinishedTaskCount() == 1);
        }
    }
}
