package io.chaofan.chunklauncher.directory;

import io.chaofan.chunklauncher.util.EasyFileAccess;
import io.chaofan.chunklauncher.util.Lang;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class Mod implements ITableRowProvider, IEnableProvider {

    public static final String[] HEADERS = new String[]{
            Lang.getString("ui.directory.tab.version"),
            Lang.getString("ui.directory.tab.mcversion"),
            Lang.getString("ui.directory.tab.state")
    };
    public static final int[] HEADER_WIDTHS = new int[]{
            50,
            50,
            20
    };
    public static final boolean[] COLUMN_CENTER = new boolean[] {
            false,
            false,
            true
    };
    public static final FileFilter INSTALL_FILTER =
            new FileNameExtensionFilter(Lang.getString("ui.directory.tab.jarfile"), "jar");

    public static Mod create(File file) {
        if (file.isFile() && (file.getName().endsWith(".jar") || file.getName().endsWith(".jar.disabled"))) {
            return new Mod(file);
        }
        return null;
    }

    private File file;
    private String name;
    private String version;
    private String mcVersion;

    public Mod(File file) {
        this.file = file;
        loadModInfo(file);
    }

    private void loadModInfo(File file) {
        JarFile jar = null;
        try {
            jar = new JarFile(file);
            ZipEntry entry = jar.getEntry("mcmod.info");
            if (entry != null) {
                InputStream stream = jar.getInputStream(entry);
                String json = EasyFileAccess.loadInputStream(stream);
                if (json != null) {
                    JSONArray arr = new JSONArray(json);
                    if (arr.length() > 0) {
                        JSONObject first = arr.getJSONObject(0);
                        name = first.has("name") ? first.getString("name") : null;
                        version = first.has("version") ? first.getString("version") : "";
                        mcVersion = first.has("mcversion") ? first.getString("mcversion") : "";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (name == null) {
            name = file.getName();
            if (name.endsWith(".disabled")) {
                name = name.substring(0, name.length() - 13);
            } else {
                name = name.substring(0, name.length() - 4);
            }
        }
    }

    private static DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Object[] provideRow() {
        return new Object[] {
                version,
                mcVersion,
                file.getName().endsWith(".disabled") ?
                        Lang.getString("ui.directory.tab.disabled") : Lang.getString("ui.directory.tab.enabled")
        };
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean isEnabled() {
        return !file.getName().endsWith(".disabled");
    }
}
