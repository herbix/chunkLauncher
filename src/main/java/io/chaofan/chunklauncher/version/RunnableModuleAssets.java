package io.chaofan.chunklauncher.version;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.chaofan.chunklauncher.Config;
import io.chaofan.chunklauncher.util.EasyFileAccess;
import org.json.JSONObject;

public class RunnableModuleAssets {

    public String index;
    public boolean virtual;
    public boolean mapToResources;
    public List<AssetItem> objects = new ArrayList<>();

    public RunnableModuleAssets(JSONObject json, String index) {
        this.index = index;
        this.virtual = json.has("virtual") && json.getBoolean("virtual");
        this.mapToResources = json.has("map_to_resources") && json.getBoolean("map_to_resources");
        JSONObject objs = json.getJSONObject("objects");
        for (String key : objs.keySet()) {
            objects.add(new AssetItem(objs.getJSONObject(key), key));
        }
    }

    public boolean copyAssetsToVirtual() {
        File virtualDir;

        if (this.virtual) {
            virtualDir = new File(Config.gamePath + Config.MINECRAFT_VIRTUAL_PATH + "/" + index);
        } else if (this.mapToResources) {
            virtualDir = new File(Config.currentProfile.runPath.directory, "resources");
            if (!virtualDir.isAbsolute()) {
                virtualDir = new File(Config.gamePath, virtualDir.getPath());
            }
        } else {
            return true;
        }

        virtualDir.mkdirs();

        for (AssetItem asset : objects) {
            String path = asset.getRealFilePath();
            File file = new File(path);
            if (!file.isFile()) {
                return false;
            }
            File targetFile = new File(virtualDir, asset.getName());
            if (targetFile.isFile()) {
                continue;
            }
            if (!EasyFileAccess.copyFile(file, targetFile)) {
                return false;
            }
        }

        return true;
    }
}
