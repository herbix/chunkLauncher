package io.chaofan.chunklauncher.directory;

import io.chaofan.chunklauncher.util.Lang;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Save implements ITableRowProvider {

    public static Save create(File file) {
        if (file.isDirectory()) {
            return new Save(file);
        }
        return null;
    }

    private File file;

    public Save(File file) {
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
        return file.getName();
    }
}
