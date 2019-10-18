package io.chaofan.chunklauncher.version;

import java.util.HashMap;
import java.util.Map;

import io.chaofan.chunklauncher.util.EasyFileAccess;
import org.json.JSONArray;
import org.json.JSONObject;

public class VersionManager {

    private static Map<String, Version> versionList = null;
    private static String latestSnapshot = null;
    private static String latestRelease = null;

    public synchronized static boolean initVersionInfo(String path) {

        String text = EasyFileAccess.loadFile(path);
        if (text == null)
            return false;

        try {
            JSONObject content = new JSONObject(text);

            JSONObject latest = content.getJSONObject("latest");
            latestSnapshot = latest.getString("snapshot");
            latestRelease = latest.getString("release");

            JSONArray versions = content.getJSONArray("versions");

            versionList = new HashMap<>();

            for (int i = 0; i < versions.length(); i++) {
                JSONObject arrelem = versions.getJSONObject(i);

                Version elem = new Version(arrelem);

                versionList.put(elem.id, elem);
            }

        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static Map<String, Version> getVersionList() {
        return versionList;
    }

    public static String getLatestSnapshot() {
        return latestSnapshot;
    }

    public static String getLatestRelease() {
        return latestRelease;
    }
}
