package io.chaofan.chunklauncher.version;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import io.chaofan.chunklauncher.Launcher;
import io.chaofan.chunklauncher.util.EasyZipAccess;
import org.json.JSONObject;
import io.chaofan.chunklauncher.Config;
import io.chaofan.chunklauncher.download.DownloadCallbackAdapter;
import io.chaofan.chunklauncher.download.Downloadable;
import io.chaofan.chunklauncher.download.Downloader;
import io.chaofan.chunklauncher.util.EasyFileAccess;
import io.chaofan.chunklauncher.util.Lang;

public class RunnableModule extends Module {

    protected Version version = null;

    private int installState = -1;

    private boolean isUninstalling = false;

    private RunnableModuleInfo moduleInfo = null;
    private RunnableModuleAssets moduleAssets = null;

    private JProgressBar progress;

    public RunnableModule(ModuleInstallCallback icallback,    ModuleUninstallCallback ucallback) {
        super(icallback, ucallback);
    }

    public void install() {
        install(null);
    }

    public void install(final JProgressBar progress) {
        if(isUninstalling) {
            System.out.println(Lang.getString("msg.module.isuninstalling"));
            return;
        }
        if(isDownloading()) {
            System.out.println(Lang.getString("msg.module.isinstalling"));
            return;
        }

        if(moduleInfo != null && !moduleInfo.canRunInThisOS()) {
            System.out.println(Lang.getString("msg.module.notallowed"));
            System.out.println(Lang.getString("msg.module.failed") + "[" + getName() + "] " +
                    Lang.getString("msg.module.reason") + moduleInfo.incompatibilityReason);
            return;
        }

        moduleDownloader = new Downloader();

        System.out.println(Lang.getString("msg.module.start") + "[" + getName() + "]");

        if(!tryLoadModuleInfo()) {
            moduleDownloader.addDownload(
                new Downloadable(getModuleJsonUrl(),
                new GameDownloadCallback("json", null)));
        } else {
            if(!checkInherit(moduleInfo, true)) {
                checkModuleAssets();
            }
        }

        if(progress != null) {
            this.progress = progress;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    progress.setValue(0);
                    progress.setMaximum(moduleDownloader.downloadCount() * 100);
                }
            });
        }

        moduleDownloader.stopAfterAllDone();
        moduleDownloader.start();
    }

    class GameDownloadCallback extends DownloadCallbackAdapter {
        private String type;
        private Library lib;

        public GameDownloadCallback(String type, Library lib) {
            this.type = type;
            this.lib = lib;
        }

        @Override
        public void downloadDone(Downloadable d, boolean succeed, boolean queueEmpty) {

            if(succeed) {
                if(type.equals("json")) {
                    downloadDoneJson(d);
                } else if(type.equals("json-inhert")) {
                    downloadDoneJsonInhert(d);
                } else if(type.equals("bin")) {
                    downloadDoneBin(d, queueEmpty);
                } else if(type.equals("sha")) {
                    downloadDoneSha(d, queueEmpty);
                }
            } else {
                if(progress != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            progress.setValue(0);
                        }
                    });
                }
                moduleDownloader.forceStop();
                System.out.println(Lang.getString("msg.module.failed") + "[" + getName() + "]");
            }
        }

        private void downloadDoneJson(Downloadable d) {
            JSONObject json = new JSONObject(d.getDownloaded());
            moduleInfo = new RunnableModuleInfo(json);

            if(!moduleInfo.canRunInThisOS()) {
                System.out.println(Lang.getString("msg.module.notallowed"));
                System.out.println(Lang.getString("msg.module.reason") + moduleInfo.incompatibilityReason);
                moduleDownloader.forceStop();
                System.out.println(Lang.getString("msg.module.failed") + "[" + getName() + "]");
                return;
            }

            new File(getModuleJsonPath()).getParentFile().mkdirs();
            EasyFileAccess.saveFile(getModuleJsonPath(), d.getDownloaded());

            if(checkInherit(moduleInfo, true)) {
                return;
            }
            checkModuleAssets();
        }

        private void downloadDoneJsonInhert(Downloadable d) {
            JSONObject json = new JSONObject(d.getDownloaded());
            RunnableModule currentModule = moduleInfo.inhertStack.peek();
            currentModule.moduleInfo = new RunnableModuleInfo(json);
            new File(currentModule.getModuleJsonPath()).getParentFile().mkdirs();
            EasyFileAccess.saveFile(currentModule.getModuleJsonPath(), d.getDownloaded());

            if(checkInherit(currentModule.moduleInfo, true)) {
                return;
            }

            moduleInfo.inhertStack.pop();
            RunnableModule stackModule = currentModule;
            while(!moduleInfo.inhertStack.empty()) {
                RunnableModule t = moduleInfo.inhertStack.pop();
                t.moduleInfo.addInheritedInfo(stackModule.moduleInfo);
                stackModule = t;
            }
            moduleInfo.addInheritedInfo(stackModule.moduleInfo);

            checkModuleAssets();
        }

        private void downloadDoneBin(Downloadable d, boolean queueEmpty) {
            File file = new File(d.getSavedFile());
            File fileReal;

            if(lib != null) {
                fileReal = new File(lib.getRealFilePath());
            } else {
                fileReal = new File(getModuleJarPath());
            }

            fileReal.delete();
            file.renameTo(fileReal);

            if(queueEmpty) {
                finishInstall();
            }
        }

        private void downloadDoneSha(Downloadable d, boolean queueEmpty) {
            File file = new File(d.getSavedFile());
            File fileReal;

            fileReal = new File(lib.getRealShaPath());

            fileReal.delete();
            file.renameTo(fileReal);

            if(queueEmpty) {
                finishInstall();
            }
        }
    }

    private void extractLib(Library lib, File fileReal, String toWhere) {
        System.out.println(Lang.getString("msg.zip.unzip") + lib.getKey());

        List<String> excludes = lib.getExtractExclude();
        new File(toWhere).mkdirs();

        EasyZipAccess.extractZip(fileReal.getPath(),
            lib.getExtractTempPath() + "/", toWhere, excludes, "");
    }

    public boolean checkInherit() {
        return checkInherit(moduleInfo, false);
    }

    private boolean checkInherit(RunnableModuleInfo info, boolean needDownload) {
        if(info.inheritsFrom != null) {
            Module m = ModuleManager.getModuleFromName(info.inheritsFrom);
            if(m != null && m instanceof RunnableModule) {
                RunnableModule rm = (RunnableModule)m;
                if(!rm.tryLoadModuleInfo() || rm.checkInherit()) {
                    if(needDownload) {
                        moduleInfo.inhertStack.push(rm);
                        moduleDownloader.addDownload(
                            new Downloadable(rm.getModuleJsonUrl(),
                            new GameDownloadCallback("json-inhert", null)));

                        if(progress != null) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    progress.setValue(0);
                                    progress.setMaximum(moduleDownloader.downloadCount() * 100);
                                }
                            });
                        }
                    }
                    return true;
                } else {
                    info.addInheritedInfo(rm.moduleInfo);
                }
            }
        }
        return false;
    }

    private void checkModuleAssets() {
        if(!tryLoadModuleAssets()) {
            moduleDownloader.addDownload(
                new Downloadable(getModuleAssetsIndexUrl(),
                new AssetDownloadCallback("json", null)));

            if(progress != null) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        progress.setValue(0);
                        progress.setMaximum(moduleDownloader.downloadCount() * 100);
                    }
                });
            }
        } else {
            addDownloadList();
        }
    }

    private void addDownloadList() {

        installCallback.installStart();

        int addCount = 0;

        if(!gameJarDownloaded()) {

            new File(getModuleJarTempPath()).getParentFile().mkdirs();
            new File(getModuleJarPath()).getParentFile().mkdirs();

            moduleDownloader.addDownload(
                new Downloadable(getModuleJarUrl(), getModuleJarTempPath(),
                new GameDownloadCallback("bin", null)));

            addCount++;
        }

        for(Library lib : moduleInfo.libraries) {
            if(!lib.needDownloadInOS())
                continue;

            if(lib.downloaded()) {
                continue;
            }

            new File(lib.getTempFilePath()).getParentFile().mkdirs();
            new File(lib.getRealFilePath()).getParentFile().mkdirs();

            String shaUrl = lib.getShaUrl();
            if (shaUrl != null) {
                moduleDownloader.addDownload(
                    new Downloadable(shaUrl, lib.getTempShaPath(),
                    new GameDownloadCallback("sha", lib)));
            }

            moduleDownloader.addDownload(
                new Downloadable(lib.getFullUrl(), lib.getTempFilePath(),
                new GameDownloadCallback("bin", lib)));

            addCount++;
        }

        for(AssetItem asset : moduleAssets.objects) {
            if(asset.downloaded()) {
                continue;
            }

            new File(asset.getTempFilePath()).getParentFile().mkdirs();
            new File(asset.getRealFilePath()).getParentFile().mkdirs();

            moduleDownloader.addDownload(
                    new Downloadable(asset.getFullUrl(), asset.getTempFilePath(),
                    new AssetDownloadCallback("bin", asset)));

            addCount++;
        }

        if(addCount == 0) {
            new Thread() {
                @Override
                public void run() {
                    finishInstall();
                }
            }.start();
        }

        if(progress != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    progress.setValue(0);
                    progress.setMaximum(moduleDownloader.downloadCount() * 100);
                }
            });
        }
    }

    private boolean gameJarDownloaded() {
        if(Config.enableChecksum && tryLoadModuleInfo() && moduleInfo.downloads != null) {
            DownloadInfo info = moduleInfo.downloads.get("client");
            if(info != null && info.sha1 != null) {
                return EasyFileAccess.doSha1Checksum2(info.sha1, getModuleJarPath());
            }
        }
        return new File(getModuleJarPath()).isFile();
    }

    class AssetDownloadCallback extends DownloadCallbackAdapter {
        private String type;
        private AssetItem asset;

        public AssetDownloadCallback(String type, AssetItem asset) {
            this.type = type;
            this.asset = asset;
        }

        @Override
        public void downloadDone(Downloadable d, boolean succeed, boolean queueEmpty) {

            if(succeed) {
                if(type.equals("json")) {
                    JSONObject json = new JSONObject(d.getDownloaded());
                    moduleAssets = new RunnableModuleAssets(json, getAssetsIndex());

                    new File(getModuleAssetsIndexPath()).getParentFile().mkdirs();
                    EasyFileAccess.saveFile(getModuleAssetsIndexPath(), d.getDownloaded());

                    addDownloadList();

                } else if(type.equals("bin")) {

                    File file = new File(d.getSavedFile());
                    File fileReal;

                    fileReal = new File(asset.getRealFilePath());

                    fileReal.delete();
                    file.renameTo(fileReal);

                    if(queueEmpty) {
                        finishInstall();
                    }
                }
            } else {
                moduleDownloader.forceStop();
                System.out.println(Lang.getString("msg.module.failed") + "[" + getName() + "]");
            }
        }
    }

    private void finishInstall() {
        System.out.println(Lang.getString("msg.module.succeeded") + "[" + getName() + "]");
        installState = 1;
        if(installCallback != null)
            installCallback.installDone();
    }

    public void uninstall() {
        if(isUninstalling) {
            System.out.println(Lang.getString("msg.module.isuninstalling"));
            return;
        }
        if(isDownloading()) {
            System.out.println(Lang.getString("msg.module.isinstalling"));
            return;
        }
        if(!tryLoadModuleInfo()) {
            System.out.println(Lang.getString("msg.module.notinstalled"));
            return;
        }

        System.out.println(Lang.getString("msg.module.startuninstall") + "[" + getName() + "]");

        isUninstalling = true;
        installState = 0;
        uninstallCallback.uninstallStart();

        new Thread() {
            @Override
            public void run() {

                File versionDir = new File(getModuleJsonPath()).getParentFile();
                System.out.println(Lang.getString("msg.module.delete") + versionDir.getPath());
                EasyFileAccess.deleteFileForce(versionDir);

                List<Library> toRemove = new ArrayList<Library>();
                if(tryLoadModuleInfo()) {
                    toRemove.addAll(moduleInfo.libraries);
                }

                for(Module m : ModuleManager.modules) {
                    if(!(m instanceof RunnableModule))
                        continue;
                    if(!m.isInstalled())
                        continue;
                    if(m == RunnableModule.this)
                        continue;
                    toRemove.removeAll(((RunnableModule)m).moduleInfo.libraries);
                }

                for(Library l : toRemove) {
                    if(!l.needDownloadInOS())
                        continue;

                    File libFile = new File(l.getRealFilePath());
                    System.out.println(Lang.getString("msg.module.delete") + libFile.getPath());
                    libFile.delete();

                    do {
                        libFile = libFile.getParentFile();
                    } while(!libFile.getName().equals("libraries") && libFile.delete());
                }

                isUninstalling = false;
                System.out.println(Lang.getString("msg.module.uninstallsucceeded") + "[" + RunnableModule.this.getName() + "]");
                moduleInfo = null;
                uninstallCallback.uninstallDone();
            }
        }.start();
    }

    public String getName() {
        return version.id;
    }

    public boolean isInstalled() {

        if(installState == -1) {

            if(!gameJarDownloaded()) {
                installState = 0;
                return false;
            }

            if(!tryLoadModuleInfo()) {
                installState = 0;
                return false;
            }

            if(checkInherit()) {
                installState = 0;
                return false;
            }

            if(!tryLoadModuleAssets()) {
                installState = 0;
                return false;
            }

            try {

                for(Library lib : moduleInfo.libraries) {
                    if(!lib.needDownloadInOS())
                        continue;

                    if(!lib.downloaded()) {
                        installState = 0;
                        return false;
                    }
                }

            } catch(Exception e) {
                installState = 0;
                return false;
            }

            try {

                for(AssetItem asset : moduleAssets.objects) {
                    String path = asset.getRealFilePath();

                    if(!new File(path).isFile()) {
                        installState = 0;
                        return false;
                    }
                }

            } catch(Exception e) {
                installState = 0;
                return false;
            }

            installState = 1;
        }

        return installState == 1;
    }

    public String[] getRunningParams() {
        return moduleInfo.minecraftArguments.clone();
    }

    public String[] getJvmParams() {
        return moduleInfo.jvmArguments.clone();
    }

    public String getMainClass() {
        return moduleInfo.mainClass;
    }

    public String getClassPath() {
        StringBuilder sb = new StringBuilder();
        String separator = System.getProperty("path.separator");

        for(int i=0; i<moduleInfo.libraries.size(); i++) {
            Library lib = moduleInfo.libraries.get(i);
            if(lib.needExtract())
                continue;
            if(!lib.needDownloadInOS())
                continue;
            sb.append(lib.getRealFilePath().replace('/', System.getProperty("file.separator").charAt(0)));
            sb.append(separator);
        }

        sb.append(getModuleJarPath().replace('/', System.getProperty("file.separator").charAt(0)));

        sb.append(separator);

        if(sb.length() > 0)
            sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public String getNativePath(String arch) {
        String extractTarget = getModuleNativePath();

        for(int i=0; i<moduleInfo.libraries.size(); i++) {
            Library lib = moduleInfo.libraries.get(i);
            if(!lib.needDownloadInOS() || !lib.needExtract() ||
                    !lib.isCompatibleForArch(arch))
                continue;

            List<String> excludes = lib.getExtractExclude();
            String extractBase = extractTarget + "/";
            File realFile = new File(lib.getRealFilePath());
            if(!EasyZipAccess.checkHasAll(realFile.getPath(),
                    extractBase, excludes, "")) {
                extractLib(lib, realFile, extractBase);
            }
        }

        return extractTarget.replace('/', System.getProperty("file.separator").charAt(0));
    }

    public String getTime() {
        if(version.time != null)
            return version.time;
        if(tryLoadModuleInfo())
            return moduleInfo.time;
        return " ";
    }

    public String getReleaseTime() {
        if(version.releaseTime != null)
            return version.releaseTime;
        if(tryLoadModuleInfo())
            return moduleInfo.releaseTime;
        return " ";
    }

    public String getActualReleaseTime() {
        return getName().toLowerCase().contains("forge") ? getTime() : getReleaseTime();
    }

    public String getType() {
        if(version.type != null)
            return version.type;
        if(tryLoadModuleInfo())
            return moduleInfo.type;
        return "unknown";
    }

    public String getState() {
        if(isInstalled()) {
            return Lang.getString("ui.module.installed");
        }
        if(tryLoadModuleInfo() || tryLoadModuleInfo()) {
            return Lang.getString("ui.module.notfinished");
        }
        return Lang.getString("ui.module.notinstalled");
    }

    public String getAssetsIndex() {
        if(tryLoadModuleInfo()) {
            return moduleInfo.assets;
        }
        return "legacy";
    }

    public boolean isAssetsVirtual() {
        return tryLoadModuleAssets() && moduleAssets.virtual;
    }

    public boolean copyAssetsToVirtual() {
        File virtualDir = new File(Config.gamePath + Config.MINECRAFT_VIRTUAL_PATH);
        virtualDir.mkdirs();

        for(AssetItem asset : moduleAssets.objects) {
            String path = asset.getRealFilePath();
            File file = new File(path);
            if(!file.isFile()) {
                return false;
            }
            File targetFile = new File(asset.getVirtualPath());
            if(targetFile.isFile()) {
                continue;
            }
            if(!EasyFileAccess.copyFile(file, targetFile)) {
                return false;
            }
        }

        return true;
    }

    private String getModuleJsonUrl() {
        if (version.url != null) {
            return version.url;
        }
        return Config.MINECRAFT_DOWNLOAD_BASE + String.format(Config.MINECRAFT_VERSION_FORMAT, getName(), getName());
    }

    private String getModuleJsonPath() {
        return Config.gamePath + String.format(Config.MINECRAFT_VERSION_FORMAT, getName(), getName());
    }

    private String getModuleJarUrl() {
        String jarVersion = getName();
        if(tryLoadModuleInfo()) {
            if(moduleInfo.downloads != null) {
                DownloadInfo info = moduleInfo.downloads.get("client");
                if(info != null && info.url != null) {
                    return info.url;
                }
            }
            jarVersion = moduleInfo.jar;
        }
        return Config.MINECRAFT_DOWNLOAD_BASE + String.format(Config.MINECRAFT_VERSION_GAME_FORMAT, jarVersion, jarVersion);
    }

    private String getModuleJarPath() {
        return Config.gamePath + String.format(Config.MINECRAFT_VERSION_GAME_FORMAT, getName(), getName());
    }

    private String getModuleJarTempPath() {
        return Config.TEMP_DIR + String.format(Config.MINECRAFT_VERSION_GAME_FORMAT, getName(), getName());
    }

    private String getModuleNativePath() {
        return Config.gamePath + String.format(Config.MINECRAFT_VERSION_NATIVE_PATH_FORMAT, getName(), getName());
    }

    private String getModuleAssetsIndexUrl() {
        if(tryLoadModuleInfo()) {
            if(moduleInfo.assetIndex != null) {
                if(moduleInfo.assetIndex.url != null) {
                    return moduleInfo.assetIndex.url;
                }
            }
        }
        return Config.MINECRAFT_DOWNLOAD_BASE + "/indexes/" + getAssetsIndex() + ".json";
    }

    private String getModuleAssetsIndexPath() {
        return Config.gamePath + Config.MINECRAFT_INDEXES_PATH + "/" + getAssetsIndex() + ".json";
    }

    private boolean tryLoadModuleInfo() {
        if(moduleInfo != null)
            return true;

        String resourceStr = EasyFileAccess.loadFile(getModuleJsonPath());
        if(resourceStr == null) {
            return false;
        }

        try {
            moduleInfo = new RunnableModuleInfo(new JSONObject(resourceStr));
        } catch(Exception e) {
            e.printStackTrace();
            Launcher.exceptionReport(e);
            return false;
        }
        return true;
    }

    private boolean tryLoadModuleAssets() {
        if(moduleAssets != null) {
            return true;
        }

        String resourceStr = EasyFileAccess.loadFile(getModuleAssetsIndexPath());
        if(resourceStr == null) {
            return false;
        }

        if(moduleInfo.assetIndex != null && moduleInfo.assetIndex.sha1 != null) {
            if(Config.enableChecksum && !EasyFileAccess.doSha1Checksum2(moduleInfo.assetIndex.sha1, getModuleAssetsIndexPath())) {
                return false;
            }
        }

        try {
            moduleAssets = new RunnableModuleAssets(new JSONObject(resourceStr), getAssetsIndex());
        } catch(Exception e) {
            e.printStackTrace();
            Launcher.exceptionReport(e);
            return false;
        }
        return true;
    }

}

