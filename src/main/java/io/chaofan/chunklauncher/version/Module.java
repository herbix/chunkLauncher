package io.chaofan.chunklauncher.version;

import io.chaofan.chunklauncher.download.Downloader;

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

    public abstract String getTime();

    public abstract String getState();

    public abstract String getType();

    public boolean isDownloading() {
        return moduleDownloader != null && moduleDownloader.getState() != Thread.State.TERMINATED;
    }
}
