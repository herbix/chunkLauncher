package io.chaofan.chunklauncher.version;

import org.json.JSONObject;

import java.util.Map;

public class DownloadInfoClassified extends DownloadInfo {

    private final Map<String, DownloadInfo> info;

    protected DownloadInfoClassified(String type, JSONObject json) {
        super(type);
        info = getDownloadInfo(json);
    }

    @Override
    public DownloadInfo getPlatformDownloadInfo(String platform) {
        return info.get(platform);
    }
}
