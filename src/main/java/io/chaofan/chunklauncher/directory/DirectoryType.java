package io.chaofan.chunklauncher.directory;

import io.chaofan.chunklauncher.util.Lang;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public enum DirectoryType {
    SAVE(Save.class, new String[] {
            Lang.getString("ui.directory.tab.lastmodified")
    }, new int[] {
            150
    }, null),

    PACK(Pack.class, new String[] {
            Lang.getString("ui.directory.tab.lastmodified")
    }, new int[] {
            150
    }, new FileNameExtensionFilter(Lang.getString("ui.directory.tab.zipfile"), "zip")),

    MOD(Mod.class, new String[] {
            Lang.getString("ui.directory.tab.lastmodified"),
            Lang.getString("ui.directory.tab.state")
    }, new int[] {
            150,
            50
    }, new FileNameExtensionFilter(Lang.getString("ui.directory.tab.jarfile"), "jar"));

    private final Class<? extends ITableRowProvider> type;
    private final String[] header;
    private final int[] width;
    private final FileFilter installFilter;

    DirectoryType(Class<? extends ITableRowProvider> type, String[] header, int[] width, FileFilter installFilter) {
        this.type = type;
        this.header = header;
        this.width = width;
        this.installFilter = installFilter;
    }

    public Class<? extends ITableRowProvider> getType() {
        return type;
    }

    public String[] getHeader() {
        return header;
    }

    public int[] getWidth() {
        return width;
    }

    public FileFilter getInstallFilter() {
        return installFilter;
    }
}
