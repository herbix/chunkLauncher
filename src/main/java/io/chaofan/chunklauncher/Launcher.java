package io.chaofan.chunklauncher;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import io.chaofan.chunklauncher.auth.AuthDoneCallback;
import io.chaofan.chunklauncher.auth.AuthType;
import io.chaofan.chunklauncher.auth.ServerAuth;
import io.chaofan.chunklauncher.download.DownloadCallbackAdapter;
import io.chaofan.chunklauncher.download.Downloadable;
import io.chaofan.chunklauncher.download.Downloader;
import io.chaofan.chunklauncher.process.Runner;
import io.chaofan.chunklauncher.util.EasyFileAccess;
import io.chaofan.chunklauncher.util.HttpFetcher;
import io.chaofan.chunklauncher.util.Lang;
import io.chaofan.chunklauncher.version.Module;
import io.chaofan.chunklauncher.version.ModuleCallbackAdapter;
import io.chaofan.chunklauncher.version.ModuleManager;
import io.chaofan.chunklauncher.version.RunnableModule;
import io.chaofan.chunklauncher.version.VersionManager;
import io.chaofan.chunklauncher.version.Version;

public class Launcher {

    public static final String VERSION = Lang.getString("app.version");

    private static final String helpWords = "Chunk Launcher V" + VERSION + " " + Lang.getString("msg.help");

    private static final Launcher instance = new Launcher();
    private static Thread shutdownHook;

    private boolean showFrame = true;
    private LauncherFrame frame = null;

    private Downloader mainDownloader = null;

    private ModuleCallbackAdapter mcallback = new ModuleCallbackAdapter() {
        @Override
        public void installStart() {
            refreshComponentsList();
        }
        @Override
        public void installDone() {
            refreshComponentsList();
        }
        @Override
        public void uninstallStart() {
            refreshComponentsList();
        }
        @Override
        public void uninstallDone() {
            refreshComponentsList();
        }
    };

    private void initFrame() {
        frame = new LauncherFrame();
        Config.updateToFrame(frame);
        frame.setStdOut();
        selectSetting(frame.profileSetting);
        synchronized (this) {
            if(!showFrame) {
                return;
            }
            frame.setVisible(true);
        }
    }

    private void initMainDownloader() {
        mainDownloader = new Downloader();
        mainDownloader.start();
    }

    private void initGameDirs() {
        File gameFolder = new File(Config.gamePath);
        if(!gameFolder.isDirectory()) {
            if(!gameFolder.mkdirs())
                System.out.println(Lang.getString("msg.gamepath.error"));
        }
        new File(Config.gamePath + Config.MINECRAFT_VERSION_PATH).mkdirs();
        new File(Config.gamePath + Config.MINECRAFT_ASSET_PATH).mkdirs();
        new File(Config.gamePath + Config.MINECRAFT_LIBRARY_PATH).mkdirs();
        new File(Config.TEMP_DIR).mkdirs();
    }

    private void initGameComponentsList() {
        VersionManager.initVersionInfo(Config.gamePath + Config.MINECRAFT_VERSION_FILE);
        Map<String, Version> versionList = VersionManager.getVersionList();
        if(versionList != null) {
            ModuleManager.initModules(versionList, mcallback, mcallback);
            refreshComponentsList();
        }
        mainDownloader.addDownload(
            new Downloadable(Config.MINECRAFT_VERSION_DOWNLOAD_URL,
            Config.TEMP_DIR + "/version_temp", new VersionDownloadCallback())
            );
    }

    private void refreshComponentsList() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int i = frame.modules.getSelectedRow();
                ModuleManager.showModules(frame.modulesModel);
                frame.modules.getSelectionModel().setSelectionInterval(i, i);

                Object s = frame.gameVersion.getSelectedItem();
                if(s == null)
                    s = Config.currentProfile.version;
                ModuleManager.showModules(frame.gameVersion);
                frame.gameVersion.setSelectedItem(s);
            }
        });
    }

    class VersionDownloadCallback extends DownloadCallbackAdapter {
        @Override
        public void downloadStart(Downloadable d) {
            System.out.println(Lang.getString("msg.version.downloading"));
        }
        @Override
        public void downloadDone(Downloadable d, boolean succeed, boolean queueEmpty) {
            if(succeed) {
                System.out.println(Lang.getString("msg.version.succeeded"));
                File versionFile = new File(Config.gamePath + Config.MINECRAFT_VERSION_FILE);
                File versionFileNew = new File(Config.TEMP_DIR + "/version_temp");
                versionFile.getParentFile().mkdirs();
                versionFile.delete();
                versionFileNew.renameTo(versionFile);
                VersionManager.initVersionInfo(Config.gamePath + Config.MINECRAFT_VERSION_FILE);
                Map<String, Version> versionList = VersionManager.getVersionList();
                if(versionList == null)
                    return;
                ModuleManager.initModules(versionList, mcallback, mcallback);
                refreshComponentsList();
            } else {
                System.out.println(Lang.getString("msg.version.failed"));
            }
        }
    }

    private void initListenersFirst() {
        SelectTabListener listener = new SelectTabListener();
        frame.profileSetting.addActionListener(listener);
        frame.moduleSetting.addActionListener(listener);
        frame.directorySetting.addActionListener(listener);
        frame.systemSetting.addActionListener(listener);
    }

    private void initListeners() {
        frame.installModules.addActionListener(new ModuleActionListener());
        frame.uninstallModules.addActionListener(new ModuleActionListener());
        frame.launch.addActionListener(new LaunchActionListener());

        frame.addProfile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String name = JOptionPane.showInputDialog(frame, Lang.getString("msg.profile.inputname"),
                    "ChunkLauncher", JOptionPane.QUESTION_MESSAGE);
                if(name == null || name.equals("")) {
                    return;
                }
                if(Config.profiles.containsKey(name)) {
                    System.out.println(Lang.getString("msg.profile.exists"));
                    return;
                }
                Profile profile = new Profile(name, null);

                Config.profiles.put(name, profile);
                frame.profiles.addItem(profile);
                frame.profiles.setSelectedItem(profile);
            }
        });

        frame.removeProfile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(Config.currentProfile.profileName.equals(Config.DEFAULT)) {
                    System.out.println(Lang.getString("msg.profile.cannotremovedefault"));
                    return;
                }
                int r = JOptionPane.showConfirmDialog(frame, Lang.getString("msg.profile.removeconfirm"),
                    "ChunkLauncher", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if(r != JOptionPane.YES_OPTION) {
                    return;
                }
                Config.profiles.remove(Config.currentProfile.profileName);
                frame.profiles.removeItem(Config.currentProfile);
            }
        });

        frame.profiles.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Config.currentProfile.updateFromFrame(frame);
                Config.currentProfile = (Profile)frame.profiles.getSelectedItem();
                if(Config.currentProfile == null) {
                    Config.currentProfile = Config.profiles.get(Config.DEFAULT);
                }
                Config.currentProfile.updateToFrame(frame);
            }
        });

        frame.showOld.addActionListener(new ShowInModuleListListener());
        frame.showSnapshot.addActionListener(new ShowInModuleListListener());

        frame.directories.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    RunningDirectory deselected = (RunningDirectory) e.getItem();
                    if (deselected != null) {
                        deselected.directory = frame.directoryPath.getText();
                    }
                } else if (e.getStateChange() == ItemEvent.SELECTED) {
                    RunningDirectory selected = (RunningDirectory) e.getItem();
                    if (selected != null) {
                        frame.directoryPath.setText(selected.directory);
                    }
                }
            }
        });

        frame.addDirectory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String name = JOptionPane.showInputDialog(frame, Lang.getString("msg.directory.inputname"),
                        "ChunkLauncher", JOptionPane.QUESTION_MESSAGE);
                if(name == null || name.equals("")) {
                    return;
                }
                if(Config.directories.containsKey(name)) {
                    System.out.println(Lang.getString("msg.directory.exists"));
                    return;
                }
                RunningDirectory directory = new RunningDirectory(name, ".");

                Config.directories.put(name, directory);
                frame.directories.addItem(directory);
                frame.runPathDirectories.addItem(directory);
                frame.directories.setSelectedItem(directory);
            }
        });

        frame.removeDirectory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RunningDirectory selectedDirectory = (RunningDirectory)frame.directories.getSelectedItem();
                if(selectedDirectory == null) {
                    System.out.println(Lang.getString("msg.directory.pleaseselectadirectory"));
                    return;
                }
                if(selectedDirectory.name.equals(Config.DEFAULT)) {
                    System.out.println(Lang.getString("msg.directory.cannotremovedefault"));
                    return;
                }
                int r = JOptionPane.showConfirmDialog(frame, Lang.getString("msg.directory.removeconfirm"),
                        "ChunkLauncher", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if(r != JOptionPane.YES_OPTION) {
                    return;
                }
                Config.directories.remove(selectedDirectory.name);
                frame.directories.removeItem(selectedDirectory);
                frame.runPathDirectories.removeItem(selectedDirectory);
                for (Profile profile : Config.profiles.values()) {
                    if (profile.runPath == selectedDirectory) {
                        profile.runPath = Config.directories.get(Config.DEFAULT);
                        if (profile == Config.currentProfile) {
                            frame.runPathDirectories.setSelectedItem(profile.runPath);
                        }
                    }
                }
            }
        });
    }

    class SelectTabListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            selectSetting((JButton)e.getSource());
        }
    }

    class ModuleActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Module m = ModuleManager.getSelectedModule(frame.modules);
            if(m == null) {
                System.out.println(Lang.getString("msg.module.noselection"));
                return;
            }
            if(e.getSource() == frame.installModules) {
                if(m.isInstalled()) {
                    System.out.println(Lang.getString("msg.module.alreadyinstalled"));
                } else {
                    Config.updateFromFrame(frame);
                    ((RunnableModule)m).install(frame.progress);
                }
            } else {
                m.uninstall();
            }
        }
    }

    class LaunchActionListener implements ActionListener {

        private boolean isLoggingIn = false;

        public void actionPerformed(ActionEvent e) {

            if(isLoggingIn) {
                System.out.println(Lang.getString("msg.game.isloggingin"));
                return;
            }

            if(frame.gameVersion.getSelectedIndex() == -1) {
                System.out.println(Lang.getString("msg.game.basicrequire"));
                return;
            }

            if(frame.user.getText().equals("")) {
                System.out.println(Lang.getString("msg.game.nousername"));
                return;
            }

            final ServerAuth auth;

            if(frame.authType.getSelectedItem() != null) {
                auth = ((AuthType)frame.authType.getSelectedItem()).
                    newInstance(frame.user.getText(), frame.pass.getText());
            } else {
                System.out.println("msg.auth.noselection");
                return;
            }

            isLoggingIn = true;

            new Thread() {
                @Override
                public void run() {
                    auth.login(new AuthDoneCallback() {
                        public void authDone(ServerAuth auth, boolean succeed) {
                            if(succeed) {
                                System.out.println(Lang.getString("msg.auth.succeeded"));
                                Config.updateFromFrame(frame);
                                Runner runner = new Runner(ModuleManager.getSelectedModule(frame.gameVersion), auth);
                                if(!runner.prepare()) {
                                    isLoggingIn = false;
                                    return;
                                }
                                frame.setVisible(false);
                                frame.setStdOut();
                                Config.saveConfig();
                                Downloader.stopAll();
                                runner.start();
                                frame.dispose();
                            } else {
                                System.out.println(Lang.getString("msg.auth.failed"));
                                isLoggingIn = false;
                            }
                        }
                    });
                }
            }.start();
        }
    }

    class ShowInModuleListListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Config.showOld = frame.showOld.isSelected();
            Config.showSnapshot = frame.showSnapshot.isSelected();

            ModuleManager.showModules(frame.modulesModel);
        }
    }

    private void run() {

        initFrame();
        System.out.println(helpWords);

        if(Config.proxy != null) {
            System.out.println(Lang.getString("msg.useproxy") + Config.getProxyString());
        }

        initListenersFirst();

        initMainDownloader();

        initGameDirs();

        initGameComponentsList();

        initListeners();
    }

    public void selectSetting(JButton source) {
        String targetCard;
        if(source == frame.profileSetting) {
            targetCard = "profile";
        } else if(source == frame.moduleSetting) {
            targetCard = "module";
        } else if(source == frame.directorySetting) {
            targetCard = "directory";
        } else if(source == frame.systemSetting) {
            targetCard = "system";
        } else {
            return;
        }
        frame.selectedCard = targetCard;
        frame.cards.show(frame.cardPanel, targetCard);
        frame.profileSetting.setSelected(false);
        frame.moduleSetting.setSelected(false);
        frame.directorySetting.setSelected(false);
        frame.systemSetting.setSelected(false);
        source.setSelected(true);
    }

    public static void exceptionReport(Throwable t) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream(out));
        String str = out.toString();
        exceptionReport(str);
    }

    public static void exceptionReport(String str) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("version", VERSION);
        params.put("message", str);
        HttpFetcher.fetchUsePostMethod("http://bugreport.herbix.me/chunkLauncher.php", params);
    }

    public static void removeShutdownHook() {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }

    public static void hideFrame() {
        synchronized (instance) {
            if(instance.frame != null) {
                instance.frame.setVisible(false);
            }
            instance.showFrame = false;
        }
    }

    public static void unhideFrame() {
        synchronized (instance) {
            if(instance.frame != null) {
                instance.frame.setVisible(true);
            }
            instance.showFrame = true;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Updater().checkUpdate();

        Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread(){
            @Override
            public void run() {
                EasyFileAccess.deleteFileForce(new File(Config.TEMP_DIR));
            }
        });

        try {
            instance.run();
        } catch (Throwable t) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(out));
            String str = out.toString();
            JOptionPane.showMessageDialog(null, Lang.getString("msg.main.error") + str,
                    Lang.getString("msg.main.error.title"), JOptionPane.ERROR_MESSAGE);
            exceptionReport(str);
        }
    }

}
