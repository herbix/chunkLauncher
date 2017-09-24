package io.chaofan.chunklauncher.version;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.chaofan.chunklauncher.util.EasyFileAccess;
import io.chaofan.chunklauncher.util.OS;
import org.json.JSONArray;
import org.json.JSONObject;
import io.chaofan.chunklauncher.Config;

public class Library {

    private String name;
    private boolean extract;
    private List<String> extractExclude;
    private JSONObject nativesMap;
    private List<Rule> rules;
    private String key;
    private String url;
    private String arch = "32";
    private Map<String, DownloadInfo> downloads;

    private Library() {

    }

    public Library(JSONObject json) {
        name = json.getString("name");
        extract = json.has("extract");
        if(extract) {
            JSONArray exls = json.getJSONObject("extract").getJSONArray("exclude");
            extractExclude = new ArrayList<String>();
            for(int i=0; i<exls.length(); i++) {
                extractExclude.add(exls.getString(i));
            }
        }
        if(json.has("natives")) {
            nativesMap = json.getJSONObject("natives");
        }
        if(json.has("rules")) {
            JSONArray rls = json.getJSONArray("rules");
            rules = new ArrayList<Rule>();
            for(int i=0; i<rls.length(); i++) {
                rules.add(new Rule(rls.getJSONObject(i)));
            }
        }
        if(json.has("url")) {
            url = json.getString("url");
            if(!url.endsWith("/"))
                url += "/";
        }
        if(json.has("downloads")) {
            downloads = DownloadInfo.getDownloadInfo(json.getJSONObject("downloads"));
        }
    }

    public String getKey() {
        if(key != null) {
            return key;
        }
        String result = "";
        String[] part = name.split(":");
        result += part[0].replace('.', '/') + "/" + part[1] + "/" + part[2] + "/";
        result += part[1] + "-" + part[2];

        if(nativesMap != null) {
            String osName = OS.getCurrentPlatform().getName();
            result += "-" + nativesMap.getString(osName).replaceAll("\\$\\{arch\\}", arch);
        }

        result += ".jar";
        key = result;
        return result;
    }

    public String getTempFilePath() {
        return Config.TEMP_DIR + "/libraries/" + getKey();
    }

    private DownloadInfo getDownloadInfo() {
        if(downloads != null) {
            if(nativesMap != null) {
                DownloadInfo info = downloads.get("classifiers");
                if(info != null) {
                    String osName = OS.getCurrentPlatform().getName();
                    return info.getPlatformDownloadInfo(nativesMap.getString(osName).replaceAll("\\$\\{arch\\}", arch));
                }
            } else {
                return downloads.get("artifact");
            }
        }
        return null;
    }

    public String getFullUrl() {
        DownloadInfo info = getDownloadInfo();
        if (info != null && info.url != null) {
            return info.url;
        }
        if(url != null)
            return url + getKey();
        else
            return Config.MINECRAFT_DOWNLOAD_LIBRARY + "/" + getKey();
    }

    public String getRealFilePath() {
        DownloadInfo info = getDownloadInfo();
        if (info != null && info.path != null) {
            return Config.gamePath + "/libraries/" + info.path;
        }
        return Config.gamePath + "/libraries/" + getKey();
    }

    public String getExtractTempPath() {
        String key = getTempFilePath();
        return key.substring(0, key.length() - 4);
    }

    public boolean needDownloadInOS() {
        return Rule.isAllowed(rules);
    }

    public boolean needExtract() {
        return extract;
    }

    public List<String> getExtractExclude() {
        return extractExclude;
    }

    public String getShaUrl() {
        if(getDownloadInfo() != null) {
            return null;
        }
        if(url != null) {
            return null;
        }
        return getFullUrl() + ".sha1";
    }

    public String getTempShaPath() {
        return getTempFilePath() + ".sha";
    }

    public String getRealShaPath() {
        return getRealFilePath() + ".sha";
    }

    public boolean downloaded() {
        DownloadInfo info = getDownloadInfo();
        if(info != null) {
            if (info.sha1 != null) {
                return EasyFileAccess.doSha1Checksum2(info.sha1, getRealFilePath());
            } else {
                return new File(getRealFilePath()).isFile();
            }
        }
        return EasyFileAccess.doSha1Checksum(getRealShaPath(), getRealFilePath());
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Library) && ((Library) o).name.equals(name);
    }

    public boolean have64BitVersion() {
        if(!needDownloadInOS()) {
            return false;
        }
        if(nativesMap != null) {
            String osName = OS.getCurrentPlatform().getName();
            return nativesMap.getString(osName).contains("${arch}");
        }
        return false;
    }

    public Library clone64Version() {
        Library result = new Library();
        result.name = name;
        result.extract = extract;
        result.extractExclude = extractExclude;
        result.nativesMap = nativesMap;
        result.rules = rules;
        result.key = key;
        result.url = url;
        result.arch = "64";
        return result;
    }

    public boolean isCompatibleForArch(String arch2) {
        return !have64BitVersion() || arch.equals(arch2);
    }

}
