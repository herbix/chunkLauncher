package io.chaofan.chunklauncher.version;

import org.json.JSONObject;
import io.chaofan.chunklauncher.Config;

import java.io.File;

public class AssetItem {

    private RunnableModuleAssets assets;
    private String name;
    private String hash;
    private int size;

    public AssetItem(JSONObject json, String name, RunnableModuleAssets assets) {
        this.name = name;
        this.hash = json.getString("hash");
        this.size = json.getInt("size");
        this.assets = assets;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public String getKey() {
        return hash.substring(0, 2) + "/" + hash;
    }

    public String getTempFilePath() {
        return Config.TEMP_DIR + Config.MINECRAFT_OBJECTS_PATH + "/" + getKey();
    }

    public String getRealFilePath() {
        return Config.gamePath + Config.MINECRAFT_OBJECTS_PATH + "/" + getKey();
    }

    public String getFullUrl() {
        return Config.MINECRAFT_RESOURCE_BASE + "/" + getKey();
    }

    public boolean downloaded() {
        return new File(getRealFilePath()).isFile();
    }
}
