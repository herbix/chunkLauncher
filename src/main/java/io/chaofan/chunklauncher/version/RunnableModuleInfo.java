package io.chaofan.chunklauncher.version;

import java.util.*;

import io.chaofan.chunklauncher.util.JsonUtil;
import io.chaofan.chunklauncher.util.StreamUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class RunnableModuleInfo {

    public String id;
    public StringWithRules[] minecraftArguments;
    public StringWithRules[] jvmArguments;
    public String time;
    public String mainClass;
    public String releaseTime;
    public String type;
    public String incompatibilityReason;
    public List<Library> libraries;
    public List<Rule> rules;
    public String assets;
    public String inheritsFrom;
    public Stack<RunnableModule> inheritStack;
    public String jar;
    public DownloadInfo assetIndex;
    public Map<String, DownloadInfo> downloads;
    public JSONObject originalJson;
    public JSONObject fullJson;

    public RunnableModuleInfo(JSONObject json) {
        originalJson = json;
        updateUsingJsonObject(json);
    }

    public void updateUsingJsonObject(JSONObject json) {
        fullJson = json;
        id = json.getString("id");
        time = json.getString("time");
        mainClass = json.getString("mainClass");
        releaseTime = json.getString("releaseTime");
        type = json.getString("type");
        if (json.has("incompatibilityReason")) {
            incompatibilityReason = json.getString("incompatibilityReason");
        } else {
            incompatibilityReason = "";
        }

        if (json.has("inheritsFrom")) {
            inheritsFrom = json.getString("inheritsFrom");
            inheritStack = new Stack<>();
        } else {
            inheritsFrom = null;
        }

        JSONArray libs = json.getJSONArray("libraries");
        libraries = new ArrayList<>();
        for (int i = 0; i < libs.length(); i++) {
            Library lib = new Library(libs.getJSONObject(i));
            libraries.add(lib);
            if (lib.have64BitVersion()) {
                libraries.add(lib.clone64Version());
            }
        }

        if (json.has("rules")) {
            JSONArray rls = json.getJSONArray("rules");
            rules = new ArrayList<>();
            for (int i = 0; i < rls.length(); i++) {
                rules.add(new Rule(rls.getJSONObject(i)));
            }
        } else {
            rules = null;
        }

        if (json.has("assets")) {
            this.assets = json.getString("assets");
        } else {
            this.assets = "legacy";
        }

        if (json.has("jar")) {
            this.jar = json.getString("jar");
        } else {
            this.jar = id;
        }

        if (json.has("downloads")) {
            this.downloads = DownloadInfo.getDownloadInfo(json.getJSONObject("downloads"));
        } else {
            this.downloads = null;
        }

        if (json.has("assetIndex")) {
            this.assetIndex = new DownloadInfo("assetIndex", json.getJSONObject("assetIndex"));
        } else {
            this.assetIndex = null;
        }

        parseArguments(json);
    }

    public boolean canRunInThisOS() {
        return Rule.isAllowed(rules);
    }

    public void addInheritedInfo(RunnableModuleInfo inherited) {
        JSONObject mergedJson = JsonUtil.mergeJson(originalJson, inherited.fullJson);
        if (mergedJson.has("inheritsFrom")) {
            mergedJson.remove("inheritsFrom");
        }
        updateUsingJsonObject(mergedJson);
    }

    private void parseArguments(JSONObject json) {
        jvmArguments = toStringWithRules(
                "-Djava.library.path=${natives_directory} -cp ${classpath}".split("[\\s]+")
        );

        if (json.has("arguments")) {
            JSONObject obj = json.getJSONObject("arguments");
            minecraftArguments = parseArguments(obj.getJSONArray("game"));
            if (obj.has("jvm")) {
                jvmArguments = parseArguments(obj.getJSONArray("jvm"));
            }
        } else if (json.has("minecraftArguments")) {
            minecraftArguments = toStringWithRules(json.getString("minecraftArguments").split("[\\s]+"));
        } else {
            throw new IllegalArgumentException("Must have arguments | minecraftArguments");
        }
    }

    private StringWithRules[] toStringWithRules(String[] strings) {
        return Arrays.stream(strings).map(StringWithRules::new).toArray(StringWithRules[]::new);
    }

    private StringWithRules[] parseArguments(JSONArray arr) {
        return StreamUtil.toStream(arr).map(StringWithRules::new).toArray(StringWithRules[]::new);
    }
}
