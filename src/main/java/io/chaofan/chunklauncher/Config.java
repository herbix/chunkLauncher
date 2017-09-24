package io.chaofan.chunklauncher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.minecraft.bootstrap.Util;

public class Config {

    public static final String CONFIG_FILE = "Launcher.properties";

    public static final String MINECRAFT_DOWNLOAD_BASE =
        "https://s3.amazonaws.com/Minecraft.Download";
    public static final String MINECRAFT_DOWNLOAD_LIBRARY =
        "https://libraries.minecraft.net";
    public static final String MINECRAFT_RESOURCE_BASE =
        "http://resources.download.minecraft.net";
    public static final String MINECRAFT_VERSION_FILE =
        "/versions/versions.json";
    public static final String MINECRAFT_VERSION_PATH =
        "/versions";
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
    public static final String MINECRAFT_VERSION_GAME_RUN_FORMAT =
        "/versions/%s/%s_run.jar";
    public static final String MINECRAFT_VERSION_GAME_EXTRACT_TEMP_FORMAT =
        "/versions/%s/%s_temp/";
    public static final String MINECRAFT_VERSION_GAME_EXTRACT_FORMAT =
        "/versions/%s/%s/";
    public static final String MINECRAFT_VERSION_NATIVE_PATH_FORMAT =
        "/versions/%s/%s-natives";

    public static final String TEMP_DIR = new File(new File(System.getProperty("java.io.tmpdir")), "ChunkLauncher").getPath();

    public static Profile currentProfile = null;
    public static Map<String, Profile> profiles = new HashMap<String, Profile>();
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

    public static void saveConfig() {
        Properties p = new Properties();
        p.setProperty("jre-path", jrePath);
        p.setProperty("d64", String.valueOf(d64));
        p.setProperty("d32", String.valueOf(d32));
        p.setProperty("memory", String.valueOf(memory));
        p.setProperty("game-path", gamePathOld);
        p.setProperty("current-etag", currentETag);
        p.setProperty("dont-update-until", String.valueOf(dontUpdateUntil));
        String profileList = "";
        for(String profileName : profiles.keySet()) {
            profileList += profileName + ";";
            p.setProperty("profile-" + profileName, profiles.get(profileName).toSavedString());
        }
        p.setProperty("profiles", profileList);
        p.setProperty("current-profile", currentProfile.profileName);
        p.setProperty("show-debug", String.valueOf(showDebugInfo));
        p.setProperty("show-old", String.valueOf(showOld));
        p.setProperty("show-snapshot", String.valueOf(showSnapshot));

        p.setProperty("proxy", getProxyString());
        p.setProperty("proxy-enabled", String.valueOf(enableProxy));
        p.setProperty("proxy-type", proxyType);

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

        profiles.put("(Default)", new Profile("(Default)", null));
        currentProfile = profiles.get("(Default)");

        try {
            in = new FileInputStream(CONFIG_FILE);
            p.load(in);
            jrePath = p.getProperty("jre-path", System.getProperty("java.home"));
            if(jrePath.equals("")) {
                jrePath = System.getProperty("java.home");
            }
            try {
                d64 = Boolean.valueOf(p.getProperty("d64", "false"));
            } catch (Exception e) {    }
            try {
                d32 = Boolean.valueOf(p.getProperty("d32", "false"));
            } catch (Exception e) {    }
            try {
                showDebugInfo = Boolean.valueOf(p.getProperty("show-debug", "false"));
            } catch (Exception e) {    }
            try {
                showOld = Boolean.valueOf(p.getProperty("show-old", "false"));
            } catch (Exception e) {    }
            try {
                showSnapshot = Boolean.valueOf(p.getProperty("show-snapshot", "false"));
            } catch (Exception e) {    }
            try {
                memory = Integer.valueOf(p.getProperty("memory", "1024"));
            } catch (Exception e) {    }
            gamePathOld = p.getProperty("game-path", Util.getWorkingDirectory().getPath());
            gamePath = new File(gamePathOld).getAbsolutePath();
            try {
                currentETag = p.getProperty("current-etag", "");
            } catch (Exception e) {    }
            try {
                dontUpdateUntil = Long.valueOf(p.getProperty("dont-update-until", String.valueOf(Long.MIN_VALUE)));
            } catch (Exception e) {    }

            profiles.clear();
            String profileList = p.getProperty("profiles", "");
            String[] split = profileList.split(";");
            for(String profileName : split) {
                if(profileName.equals(""))
                    continue;
                Profile profile = new Profile(profileName, p.getProperty("profile-" + profileName, null));
                profiles.put(profileName, profile);
            }

            String current = p.getProperty("current-profile", "(Default)");
            currentProfile = profiles.get(current);

            try {
                setProxyString(p.getProperty("proxy", null));
            } catch (Exception e) {    }
            try {
                enableProxy = Boolean.valueOf(p.getProperty("proxy-enabled", "false"));
            } catch (Exception e) {    }
            proxyType = p.getProperty("proxy-type", "HTTP");

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateToFrame(LauncherFrame frame) {
        frame.profiles.removeAllItems();
        for(Profile profile : profiles.values()) {
            frame.profiles.addItem(profile);
        }
        frame.profiles.setSelectedItem(currentProfile);
        currentProfile.updateToFrame(frame);
        frame.jrePath.setText(jrePath);
        frame.memorySizeSlider.setValue(memory);
        if(!d32 && !d64)
            frame.runningModeDefault.setSelected(true);
        if(d32)
            frame.runningMode32.setSelected(true);
        if(d64)
            frame.runningMode64.setSelected(true);
        frame.showOld.setSelected(showOld);
        frame.showSnapshot.setSelected(showSnapshot);
        frame.memorySize.setText(String.valueOf(memory));
        frame.proxy.setText(getProxyString());
        frame.proxyType.setSelectedItem(proxyType);
        frame.enableProxy.setSelected(enableProxy);
    }

    public static void updateFromFrame(LauncherFrame frame) {
        profiles.clear();
        for(int i=0; i<frame.profiles.getItemCount(); i++) {
            Profile profile = (Profile)frame.profiles.getItemAt(i);
            profiles.put(profile.profileName, profile);
        }
        currentProfile = (Profile)frame.profiles.getSelectedItem();
        if(currentProfile == null)
            currentProfile = profiles.get("(Default)");
        currentProfile.updateFromFrame(frame);
        jrePath = frame.jrePath.getText();
        if(jrePath.equals("")) {
            jrePath = System.getProperty("java.home");
        }
        d64 = frame.runningMode64.isSelected();
        d32 = frame.runningMode32.isSelected();
        showOld = frame.showOld.isSelected();
        showSnapshot = frame.showSnapshot.isSelected();
        try {
            memory = Integer.valueOf(frame.memorySize.getText());
        } catch (Exception e) {    }
        enableProxy = frame.enableProxy.isSelected();
        proxyType = frame.proxyType.getSelectedItem().toString();
        try {
            setProxyString(frame.proxy.getText());
        } catch (Exception e) {    }

    }

    public static String getProxyString() {
        return proxyString;
    }

    public static void setProxyString(String str) {
        proxyString = str;
        int index = proxyString.lastIndexOf(':');
        if(index == -1) {
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
        if(proxyType.equals("HTTP")) result = Type.HTTP;
        else if(proxyType.equals("Socks")) result = Type.SOCKS;
        return result;
    }

    static {
        loadConfig();
    }
}
