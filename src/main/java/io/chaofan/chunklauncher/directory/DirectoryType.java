package io.chaofan.chunklauncher.directory;

import javax.swing.filechooser.FileFilter;

public enum DirectoryType {
    SAVE(Save.class, Save.HEADERS, Save.HEADER_WIDTHS, null, null),
    PACK(Pack.class, Pack.HEADERS, Pack.HEADER_WIDTHS, Pack.INSTALL_FILTER, null),
    MOD(Mod.class, Mod.HEADERS, Mod.HEADER_WIDTHS, Mod.INSTALL_FILTER, Mod.COLUMN_CENTER);

    private final Class<? extends ITableRowProvider> type;
    private final String[] header;
    private final int[] width;
    private final FileFilter installFilter;
    private final boolean[] columnCenter;

    DirectoryType(Class<? extends ITableRowProvider> type, String[] header, int[] width, FileFilter installFilter, boolean[] center) {
        this.type = type;
        this.header = header;
        this.width = width;
        this.installFilter = installFilter;
        this.columnCenter = center;
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

    public boolean[] getColumnCenter() {
        return columnCenter;
    }
}
