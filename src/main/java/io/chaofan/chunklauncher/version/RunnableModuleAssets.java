package io.chaofan.chunklauncher.version;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class RunnableModuleAssets {

    public String index;
    public boolean virtual;
    public List<AssetItem> objects = new ArrayList<AssetItem>();

    public RunnableModuleAssets(JSONObject json, String index) {
        this.index = index;
        this.virtual = json.has("virtual") && json.getBoolean("virtual");
        JSONObject objs = json.getJSONObject("objects");
        for(Object key : objs.keySet()) {
            String keyStr = (String)key;
            objects.add(new AssetItem(objs.getJSONObject(keyStr), keyStr));
        }
    }

}
