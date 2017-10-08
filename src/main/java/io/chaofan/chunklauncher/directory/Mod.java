package io.chaofan.chunklauncher.directory;

import io.chaofan.chunklauncher.util.Lang;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Mod implements ITableRowProvider, IEnableProvider {

    public static Mod create(File file) {
        if (file.isFile() && (file.getName().endsWith(".jar") || file.getName().endsWith(".jar.disabled"))) {
            return new Mod(file);
        }
        return null;
    }

    private File file;

    public Mod(File file) {
        this.file = file;
    }

    private static DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Object[] provideRow() {
        return new Object[] {
                format.format(new Date(file.lastModified())),
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
        String name = file.getName();
        if (name.endsWith(".disabled")) {
            name = name.substring(0, name.length() - 9);
        }
        return name.substring(0, name.length() - 4);
    }

    @Override
    public boolean isEnabled() {
        return !file.getName().endsWith(".disabled");
    }
}
