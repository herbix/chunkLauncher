package io.chaofan.chunklauncher.directory;

import javax.swing.filechooser.FileFilter;

public enum DirectoryType {
    SAVE(Save.class, Save.HEADERS, Save.HEADER_WIDTHS, null),
    PACK(Pack.class, Pack.HEADERS, Pack.HEADER_WIDTHS, Pack.INSTALL_FILTER),
    MOD(Mod.class, Mod.HEADERS, Mod.HEADER_WIDTHS, Mod.INSTALL_FILTER);

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
