package io.chaofan.chunklauncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.chaofan.chunklauncher.util.Language;
import net.minecraft.bootstrap.Util;

import javax.swing.*;

public class Config {

    public static final String LANGUAGE_SYSTEM =
            System.getProperty("user.language", "en") + "-" +
                    System.getProperty("user.country", "US");

    public static final String CONFIG_FILE = "Launcher.properties";

    public static final String MINECRAFT_DOWNLOAD_BASE =
            "https://s3.amazonaws.com/Minecraft.Download";
    public static final String MINECRAFT_DOWNLOAD_LIBRARY =
            "https://libraries.minecraft.net";
    public static final String MINECRAFT_RESOURCE_BASE =
            "http://resources.download.minecraft.net";
    public static final String MINECRAFT_VERSION_DOWNLOAD_URL =
            "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    public static final String MINECRAFT_VERSION_FILE =
            "/versions/versions.json";
    public static final String MINECRAFT_VERSION_PATH =
            "/versions";
    public static final String MINECRAFT_LIBRARY_PATH =
            "/libraries";
    public static final String MINECRAFT_ASSET_PATH =
            "/assets";
    public static final String MINECRAFT_OBJECTS_PATH =
            "/assets/objects";
    public static final String MINECRAFT_INDEXES_PATH =
            "/assets/indexes";
    public static final String MINECRAFT_VIRTUAL_PATH =
            "/assets/virtual";
    public static final String MINECRAFT_VERSION_FORMAT =
            "/versions/%s/%s.json";
    public static final String MINECRAFT_VERSION_GAME_FORMAT =
            "/versions/%s/%s.jar";
    public static final String MINECRAFT_VERSION_NATIVE_PATH_FORMAT =
            "/versions/%s/%s-natives";

    public static final String TEMP_DIR = new File(new File(System.getProperty("java.io.tmpdir")), "ChunkLauncher").getPath();

    public static final String DEFAULT = "(Default)";

    public static Profile currentProfile = null;
    public static Map<String, Profile> profiles = new HashMap<>();
    public static String jrePath = System.getProperty("java.home");
    public static boolean d64 = false;
    public static boolean d32 = false;
    public static int memory = 1024;
    public static String gamePath = Util.getWorkingDirectory().getPath();
    public static String gamePathOld = gamePath;
    public static String currentETag = "";
    public static long dontUpdateUntil = Long.MIN_VALUE;
    public static boolean showDebugInfo = false;
    public static boolean showOld = false;
    public static boolean showSnapshot = false;
    public static boolean enableProxy = false;
    public static Proxy proxy = null;
    public static String proxyType = "HTTP";
    private static String proxyString;
    public static String proxyHost;
    public static int proxyPort;
    public static boolean enableChecksum = false;
    public static RunningDirectory currentDirectory = null;
    public static String language = LANGUAGE_SYSTEM;
    public static Map<String, RunningDirectory> directories = new HashMap<>();

    public static void saveConfig() {
        Properties p = new Properties();
        p.setProperty("jre-path", jrePath);
        p.setProperty("d64", String.valueOf(d64));
        p.setProperty("d32", String.valueOf(d32));
        p.setProperty("memory", String.valueOf(memory));
        p.setProperty("game-path", gamePathOld);
        p.setProperty("current-etag", currentETag);
        p.setProperty("dont-update-until", String.valueOf(dontUpdateUntil));
        StringBuilder profileList = new StringBuilder();
        for (String profileName : profiles.keySet()) {
            profileList.append(profileName);
            profileList.append(";");
            p.setProperty("profile-" + profileName, profiles.get(profileName).toSavedString());
        }
        p.setProperty("profiles", profileList.toString());
        p.setProperty("current-profile", currentProfile.profileName);
        p.setProperty("show-debug", String.valueOf(showDebugInfo));
        p.setProperty("show-old", String.valueOf(showOld));
        p.setProperty("show-snapshot", String.valueOf(showSnapshot));
        p.setProperty("proxy", getProxyString());
        p.setProperty("proxy-enabled", String.valueOf(enableProxy));
        p.setProperty("proxy-type", proxyType);
        p.setProperty("enable-checksum", String.valueOf(enableChecksum));
        p.setProperty("language", language);
        StringBuilder directoryList = new StringBuilder();
        for (Map.Entry<String, RunningDirectory> entry : directories.entrySet()) {
            directoryList.append(entry.getKey());
            directoryList.append(";");
            directoryList.append(entry.getValue().directory);
            directoryList.append(";");
        }
        p.setProperty("directories", directoryList.toString());
        if (currentDirectory != null) {
            p.setProperty("current-directory", currentDirectory.name);
        }

        try {
            FileOutputStream out = new FileOutputStream(CONFIG_FILE);
            p.store(out, "Created by ChunkLauncher");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadConfig() {
        InputStream in;
        Properties p = new Properties();

        // Keep it here in case no property file loaded
        directories.put(DEFAULT, new RunningDirectory(DEFAULT, "."));
        currentDirectory = directories.get(DEFAULT);

        profiles.put(DEFAULT, new Profile(DEFAULT, null));
        currentProfile = profiles.get(DEFAULT);

        try {
            in = new FileInputStream(CONFIG_FILE);
            p.load(in);
            jrePath = p.getProperty("jre-path", System.getProperty("java.home"));
            if (jrePath.equals("")) {
                jrePath = System.getProperty("java.home");
            }
            try {
                d64 = Boolean.parseBoolean(p.getProperty("d64", "false"));
            } catch (Exception ignored) {
            }
            try {
                d32 = Boolean.parseBoolean(p.getProperty("d32", "false"));
            } catch (Exception ignored) {
            }
            try {
                showDebugInfo = Boolean.parseBoolean(p.getProperty("show-debug", "false"));
            } catch (Exception ignored) {
            }
            try {
                showOld = Boolean.parseBoolean(p.getProperty("show-old", "false"));
            } catch (Exception ignored) {
            }
            try {
                showSnapshot = Boolean.parseBoolean(p.getProperty("show-snapshot", "false"));
            } catch (Exception ignored) {
            }
            try {
                memory = Integer.parseInt(p.getProperty("memory", "1024"));
            } catch (Exception ignored) {
            }
            gamePathOld = p.getProperty("game-path", Util.getWorkingDirectory().getPath());
            gamePath = new File(gamePathOld).getAbsolutePath();
            try {
                currentETag = p.getProperty("current-etag", "");
            } catch (Exception ignored) {
            }
            try {
                dontUpdateUntil = Long.parseLong(p.getProperty("dont-update-until", String.valueOf(Long.MIN_VALUE)));
            } catch (Exception ignored) {
            }

            try {
                setProxyString(p.getProperty("proxy", null));
            } catch (Exception ignored) {
            }
            try {
                enableProxy = Boolean.parseBoolean(p.getProperty("proxy-enabled", "false"));
            } catch (Exception ignored) {
            }
            proxyType = p.getProperty("proxy-type", "HTTP");
            try {
                enableChecksum = Boolean.parseBoolean(p.getProperty("enable-checksum", "false"));
            } catch (Exception ignored) {
            }

            // Load directories first because profiles use them.
            directories.clear();
            String directoryList = p.getProperty("directories", "");
            String[] split = directoryList.split(";");
            for (int i = 0; i < split.length - 1; i += 2) {
                directories.put(split[i], new RunningDirectory(split[i], split[i + 1]));
            }

            if (!directories.containsKey(DEFAULT)) {
                directories.put(DEFAULT, new RunningDirectory(DEFAULT, "."));
            }
            String current = p.getProperty("current-directory", DEFAULT);
            currentDirectory = directories.get(current);

            profiles.clear();
            String profileList = p.getProperty("profiles", "");
            split = profileList.split(";");
            for (String profileName : split) {
                if (profileName.equals(""))
                    continue;
                Profile profile = new Profile(profileName, p.getProperty("profile-" + profileName, null));
                profiles.put(profileName, profile);
                if (!directories.containsKey(profile.runPath.name)) {
                    directories.put(profile.runPath.name, profile.runPath);
                }
            }

            if (!profiles.containsKey(DEFAULT)) {
                profiles.put(DEFAULT, new Profile(DEFAULT, null));
            }

            current = p.getProperty("current-profile", DEFAULT);
            currentProfile = profiles.get(current);

            language = p.getProperty("language", LANGUAGE_SYSTEM);

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateToFrame(LauncherFrame frame) {
        frame.jrePath.setText(jrePath);
        frame.memorySizeSlider.setValue(memory);
        if (!d32 && !d64)
            frame.runningModeDefault.setSelected(true);
        if (d32)
            frame.runningMode32.setSelected(true);
        if (d64)
            frame.runningMode64.setSelected(true);
        frame.showOld.setSelected(showOld);
        frame.showSnapshot.setSelected(showSnapshot);
        frame.memorySize.setText(String.valueOf(memory));
        frame.proxy.setText(getProxyString());
        frame.proxyType.setSelectedItem(proxyType);
        frame.enableProxy.setSelected(enableProxy);
        // update directories first
        frame.directories.removeAllItems();
        frame.runPathDirectories.removeAllItems();
        directories.values().stream().sorted(Comparator.comparing(a -> a.name)).forEach(directory -> {
            frame.directories.addItem(directory);
            frame.runPathDirectories.addItem(directory);
        });
        frame.directories.setSelectedItem(currentDirectory);
        RunningDirectory selectedDirectory = (RunningDirectory) frame.directories.getSelectedItem();
        if (selectedDirectory != null) {
            frame.directoryPath.setText(selectedDirectory.directory);
        }
        frame.profiles.removeAllItems();
        profiles.values().stream().sorted(Comparator.comparing(a -> a.profileName)).forEach(frame.profiles::addItem);
        frame.profiles.setSelectedItem(currentProfile);
        currentProfile.updateToFrame(frame);
        updateLanguageToFrame(frame);
    }

    public static void updateFromFrame(LauncherFrame frame) {
        profiles.clear();
        for (int i = 0; i < frame.profiles.getItemCount(); i++) {
            Profile profile = frame.profiles.getItemAt(i);
            profiles.put(profile.profileName, profile);
        }
        currentProfile = (Profile) frame.profiles.getSelectedItem();
        if (currentProfile == null)
            currentProfile = profiles.get(DEFAULT);
        currentProfile.updateFromFrame(frame);
        jrePath = frame.jrePath.getText();
        if (jrePath.equals("")) {
            jrePath = System.getProperty("java.home");
        }
        d64 = frame.runningMode64.isSelected();
        d32 = frame.runningMode32.isSelected();
        showOld = frame.showOld.isSelected();
        showSnapshot = frame.showSnapshot.isSelected();
        try {
            memory = Integer.parseInt(frame.memorySize.getText());
        } catch (Exception ignored) {
        }
        enableProxy = frame.enableProxy.isSelected();
        proxyType = String.valueOf(frame.proxyType.getSelectedItem());
        try {
            setProxyString(frame.proxy.getText());
        } catch (Exception ignored) {
        }
        RunningDirectory selectedDirectory = (RunningDirectory) frame.directories.getSelectedItem();
        if (selectedDirectory != null) {
            selectedDirectory.directory = frame.directoryPath.getText();
        }
        directories.clear();
        for (int i = 0; i < frame.directories.getItemCount(); i++) {
            RunningDirectory directory = frame.directories.getItemAt(i);
            directories.put(directory.name, directory);
        }
        currentDirectory = selectedDirectory;
        Language selectedLanguage = (Language) frame.language.getSelectedItem();
        language = selectedLanguage != null ? selectedLanguage.value : language;
    }

    public static String getProxyString() {
        return proxyString;
    }

    public static void setProxyString(String str) {
        proxyString = str;
        int index = proxyString.lastIndexOf(':');
        if (index == -1) {
            proxyHost = proxyString;
            proxyPort = 3128;
        } else {
            proxyHost = proxyString.substring(0, index);
            proxyPort = Integer.parseInt(proxyString.substring(index + 1));
        }
        proxy = new Proxy(getProxyType(), new InetSocketAddress(proxyHost, proxyPort));
    }

    private static Type getProxyType() {
        Type result = Type.DIRECT;
        if (proxyType.equals("HTTP")) result = Type.HTTP;
        else if (proxyType.equals("Socks")) result = Type.SOCKS;
        return result;
    }

    private static void updateLanguageToFrame(LauncherFrame frame) {
        ComboBoxModel<Language> languageModel = frame.language.getModel();
        boolean hasSelectedLanguage = false;
        for (int i = 0; i < languageModel.getSize(); i++) {
            if (languageModel.getElementAt(i).value.equals(language)) {
                frame.language.setSelectedIndex(i);
                hasSelectedLanguage = true;
                break;
            }
        }
        if (!hasSelectedLanguage && language.contains("-")) {
            String languageAlt = language.split("-")[0];
            for (int i = 0; i < languageModel.getSize(); i++) {
                if (languageModel.getElementAt(i).value.equals(languageAlt)) {
                    frame.language.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    static {
        loadConfig();
    }
}
