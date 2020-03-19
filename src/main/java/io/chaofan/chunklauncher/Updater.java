package io.chaofan.chunklauncher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Date;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.swing.JOptionPane;

import io.chaofan.chunklauncher.util.HttpFetcher;
import io.chaofan.chunklauncher.util.Lang;
import io.chaofan.chunklauncher.util.OS;
import org.json.JSONArray;
import org.json.JSONObject;

public class Updater {

    private static final String RELEASE_URL = "https://api.github.com/repos/herbix/chunkLauncher/releases/latest";
    private static final String UPDATE_URL = "https://github.com/herbix/chunkLauncher/raw/master/bin/ChunkLauncher.jar";

    private String currentFile;

    private String eTag = "";
    private int size = 0;

    private String getUpdateUrl(String releaseUrl) {
        String data = HttpFetcher.fetch(releaseUrl);
        if (data == null) {
            return null;
        }

        JSONObject obj = new JSONObject(data);
        String version = obj.getString("tag_name");
        if (version.startsWith("v")) {
            version = version.substring(1);
        }

        if (versionCompare(version, Launcher.VERSION) <= 0) {
            return null;
        }

        JSONArray assets = obj.getJSONArray("assets");
        for (int i=0; i<assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if (asset.getString("name").equalsIgnoreCase("ChunkLauncher.jar")) {
                return asset.getString("browser_download_url");
            }
        }

        return null;
    }

    private int versionCompare(String a, String b) {
        String[] splitA = a.split("\\.");
        String[] splitB = b.split("\\.");
        for (int i=0; i<splitA.length && i<splitB.length; i++) {
            int c = Integer.compare(Integer.parseInt(splitA[i]), Integer.parseInt(splitB[i]));
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(splitA.length, splitB.length);
    }

    private InputStream getRemoteFileInfo(String updateUrl) {
        URLConnection conn;
        try {
            conn = new URL(updateUrl).openConnection();
            if (!eTag.equals("")) {
                conn.addRequestProperty("If-None-Match", "\"" + eTag + "\"");
            }
            conn.connect();
            int code = ((HttpURLConnection) conn).getResponseCode();
            if (code == 200) {
                size = conn.getContentLength();
                eTag = conn.getHeaderField("ETag");
                eTag = eTag.substring(1, eTag.length() - 1);
                return conn.getInputStream();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean checkSizeEqual() {
        return size == new File(currentFile).length();
    }

    private boolean extractUpdater() throws Exception {
        JarFile file = new JarFile(currentFile);

        ZipEntry updaterEntry = file.getEntry("io/chaofan/chunklauncher/UpdaterLater.class");

        InputStream in = file.getInputStream(updaterEntry);
        File saved = new File(new File(Config.TEMP_DIR), "io/chaofan/chunklauncher/UpdaterLater.class");
        if (!saved.getParentFile().mkdirs())
            return false;
        FileOutputStream out = new FileOutputStream(saved);

        byte[] buffer = new byte[65536];
        int count;
        while ((count = in.read(buffer)) >= 0) {
            out.write(buffer, 0, count);
        }

        in.close();
        out.close();

        return true;
    }

    void checkUpdate() {
        new Thread(this::checkUpdate0).start();
    }

    void checkUpdate0() {

        if (Config.dontUpdateUntil > new Date().getTime()) {
            return;
        }

        eTag = Config.currentETag;

        try {
            currentFile = URLDecoder.decode(
                    Launcher.class.getResource("/io/chaofan/chunklauncher/Launcher.class").toString(), "UTF-8");

            if (!currentFile.startsWith("jar:")) {
                return;
            }

            currentFile = currentFile.substring(4 + 5);
            currentFile = currentFile.substring(0, currentFile.lastIndexOf('!'));

            String updateUrl = getUpdateUrl(RELEASE_URL);
            if (updateUrl == null)
                return;

            InputStream in = getRemoteFileInfo(updateUrl);
            if (in == null)
                return;

            if (Config.currentETag.equals(""))
                if (checkSizeEqual()) {
                    Config.currentETag = eTag;
                    return;
                }

            if (eTag.equals(Config.currentETag))
                return;

            int selection = JOptionPane.showConfirmDialog(null, Lang.getString("msg.update.request"), "ChunkLauncher", JOptionPane.YES_NO_OPTION);
            if (selection != JOptionPane.YES_OPTION) {
                Config.dontUpdateUntil = new Date().getTime() + 7 * 24 * 60 * 60 * 1000;
                eTag = Config.currentETag;
                in.close();
                return;
            }

            Launcher.hideFrame();

            UpdateDialog dialog = new UpdateDialog();
            dialog.setVisible(true);

            try {
                if (!extractUpdater())
                    throw new Exception("Cannot extract updater.");

                File tempFile = new File(new File(Config.TEMP_DIR), "ChunkLauncher.jar");
                FileOutputStream out = new FileOutputStream(tempFile);

                byte[] buffer = new byte[65536];
                int count;
                int downloaded = 0;
                while ((count = in.read(buffer)) >= 0) {
                    if (count > 0) {
                        downloaded += count;
                        out.write(buffer, 0, count);
                        dialog.setProgress((double) downloaded / size);
                    }
                    if (!dialog.isVisible()) {
                        in.close();
                        out.close();
                        return;
                    }
                }

                in.close();
                out.close();

                dialog.setVisible(false);

                String java = System.getProperty("java.home") + "/bin/java";

                if (OS.getCurrentPlatform() == OS.WINDOWS) {
                    if (new File(java + "w.exe").exists()) {
                        java += "w.exe";
                    }
                }

                Config.currentETag = eTag;
                Config.dontUpdateUntil = Long.MIN_VALUE;
                Config.saveConfig();

                ProcessBuilder pb = new ProcessBuilder(java, "-cp", Config.TEMP_DIR, "io.chaofan.chunklauncher.UpdaterLater",
                        tempFile.getAbsolutePath(), currentFile);

                Launcher.removeShutdownHook();

                pb.start();
                System.exit(0);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, Lang.getString("msg.update.exception1") + e.toString() + Lang.getString("msg.update.exception2"), "ChunkLauncher",
                        JOptionPane.ERROR_MESSAGE);
                dialog.setVisible(false);
                Launcher.unhideFrame();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Launcher.exceptionReport(e);
        }
    }
}
