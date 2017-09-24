package io.chaofan.chunklauncher.version;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DownloadInfo {

    public static Map<String, DownloadInfo> getDownloadInfo(JSONObject json) {
        Map<String, DownloadInfo> result = new HashMap<String, DownloadInfo>();
        Iterator it = json.keys();
        while (it.hasNext()) {
            String key = it.next().toString();
            if (key.equals("classifiers")) {
                result.put(key, new DownloadInfoClassified(key, json.getJSONObject(key)));
            } else {
                result.put(key, new DownloadInfo(key, json.getJSONObject(key)));
            }
        }
        return result;
    }

    public final int size;
    public final String sha1;
    public final String path;
    public final String url;
    public final String type;

    protected DownloadInfo(String type) {
        this.type = type;
        this.size = 0;
        this.sha1 = null;
        this.path = null;
        this.url = null;
    }

    public DownloadInfo(String type, JSONObject json) {
        this.type = type;
        this.size = json.has("size") ? json.getInt("size") : 0;
        this.sha1 = json.has("sha1") ? json.getString("sha1") : null;
        this.path = json.has("path") ? json.getString("path") : null;
        this.url = json.has("url") ? json.getString("url") : null;
    }

    public DownloadInfo getPlatformDownloadInfo(String platform) {
        return this;
    }
}
