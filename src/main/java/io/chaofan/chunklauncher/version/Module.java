package io.chaofan.chunklauncher.version;

import io.chaofan.chunklauncher.download.Downloader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Module {

    protected Downloader moduleDownloader;
    protected ModuleInstallCallback installCallback;
    protected ModuleUninstallCallback uninstallCallback;

    public Module(ModuleInstallCallback icallback, ModuleUninstallCallback ucallback) {
        this.installCallback = icallback;
        this.uninstallCallback = ucallback;
    }

    public abstract void install();

    public abstract void uninstall();

    public abstract String getName();

    public abstract boolean isInstalled();

    public abstract String getReleaseTime();

    public abstract String getActualReleaseTime();

    public abstract String getTime();

    public abstract String getState();

    public abstract String getType();

    public boolean isDownloading() {
        return moduleDownloader != null && moduleDownloader.getState() <= Downloader.RUNNING;
    }

    public String getFormattedReleaseTime() {
        Date releaseTime = null;
        try {
            releaseTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(getActualReleaseTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return "Unknown";
        }

        return new SimpleDateFormat("yyyy-MM-dd").format(releaseTime);
    }
}
