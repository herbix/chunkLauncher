package io.chaofan.chunklauncher.directory;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.stream.NBTInputStream;
import io.chaofan.chunklauncher.util.Lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Save implements ITableRowProvider {

    public static final String[] HEADERS = new String[]{
            Lang.getString("ui.directory.tab.version"),
            Lang.getString("ui.directory.tab.lastmodified")
    };
    public static final int[] HEADER_WIDTHS = new int[]{
            80,
            150
    };

    public static Save create(File file) {
        if (file.isDirectory()) {
            return new Save(file);
        }
        return null;
    }

    private File file;
    private String name;
    private String version;

    public Save(File file) {
        this.file = file;
        loadSaveInfo(file);
    }

    private void loadSaveInfo(File file) {
        File levelDataFile = new File(file, "level.dat");
        version = "";
        NBTInputStream in = null;
        try {
            in = new NBTInputStream(new FileInputStream(levelDataFile));
            Tag rootTag = in.readTag();
            if (rootTag instanceof CompoundTag) {
                Tag tag = ((CompoundTag) rootTag).getValue().get("Data");
                if (tag instanceof CompoundTag) {
                    CompoundMap dataTagMap = ((CompoundTag) tag).getValue();
                    Tag versionTag = dataTagMap.get("Version");
                    if (versionTag instanceof CompoundTag) {
                        Tag versionString = ((CompoundTag) versionTag).getValue().get("Name");
                        if (versionString instanceof StringTag) {
                            version = ((StringTag) versionString).getValue();
                        }
                    }

                    Tag nameTag = dataTagMap.get("LevelName");
                    Object nameObj = nameTag.getValue();
                    if (nameObj != null) {
                        name = String.valueOf(nameObj);
                    } else {
                        name = file.getName();
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Object[] provideRow() {
        return new Object[]{
                version,
                format.format(new Date(file.lastModified()))
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
}
