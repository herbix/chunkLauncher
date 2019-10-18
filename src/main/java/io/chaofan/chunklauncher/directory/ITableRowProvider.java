package io.chaofan.chunklauncher.directory;

import java.io.File;

public interface ITableRowProvider {
    Object[] provideRow();

    File getFile();
}
