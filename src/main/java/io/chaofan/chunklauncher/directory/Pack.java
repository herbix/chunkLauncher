package io.chaofan.chunklauncher.directory;

import io.chaofan.chunklauncher.util.Lang;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Pack implements ITableRowProvider {

    public static Pack create(File file) {
        if (file.isFile() && file.getName().endsWith(".zip")) {
            return new Pack(file);
        }
        return null;
    }

    private File file;

    public Pack(File file) {
        this.file = file;
    }

    private static DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Object[] provideRow() {
        return new Object[] {
                format.format(new Date(file.lastModified()))
        };
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        String name = file.getName();
        return name.substring(0, name.length() - 4);
    }
}
