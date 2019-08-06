package io.chaofan.chunklauncher.version;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import io.chaofan.chunklauncher.util.JsonUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class RunnableModuleInfo {

    public String id;
    public String[] minecraftArguments;
    public String[] jvmArguments;
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
        if(json.has("incompatibilityReason")) {
            incompatibilityReason = json.getString("incompatibilityReason");
        } else {
            incompatibilityReason = "";
        }

        if(json.has("inheritsFrom")) {
            inheritsFrom = json.getString("inheritsFrom");
            inheritStack = new Stack<RunnableModule>();
        } else {
            inheritsFrom = null;
        }

        JSONArray libs = json.getJSONArray("libraries");
        libraries = new ArrayList<Library>();
        for(int i=0; i<libs.length(); i++) {
            Library lib = new Library(libs.getJSONObject(i));
            libraries.add(lib);
            if(lib.have64BitVersion()) {
                libraries.add(lib.clone64Version());
            }
        }

        if(json.has("rules")) {
            JSONArray rls = json.getJSONArray("rules");
            rules = new ArrayList<Rule>();
            for(int i=0; i<rls.length(); i++) {
                rules.add(new Rule(rls.getJSONObject(i)));
            }
        } else {
            rules = null;
        }

        if(json.has("assets")) {
            this.assets = json.getString("assets");
        } else {
            this.assets = "legacy";
        }

        if(json.has("jar")) {
            this.jar = json.getString("jar");
        } else {
            this.jar = id;
        }

        if(json.has("downloads")) {
            this.downloads = DownloadInfo.getDownloadInfo(json.getJSONObject("downloads"));
        } else {
            this.downloads = null;
        }

        if(json.has("assetIndex")) {
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
        jvmArguments = "-Djava.library.path=${natives_directory} -cp ${classpath}".split("[\\s]+");

        if (json.has("arguments")) {
            JSONObject obj = json.getJSONObject("arguments");
            minecraftArguments = parseArguments(obj.getJSONArray("game"));
            if (obj.has("jvm")) {
                jvmArguments = parseArguments(obj.getJSONArray("jvm"));
            }
        } else if (json.has("minecraftArguments")) {
            minecraftArguments = json.getString("minecraftArguments").split("[\\s]+");
        } else {
            throw new IllegalArgumentException("Must have arguments | minecraftArguments");
        }
    }

    private String[] parseArguments(JSONArray arr) {
        List<String> result = new ArrayList<String>();

        for(int i=0; i<arr.length(); i++) {
            Object obj = arr.get(i);
            if (obj instanceof String) {
                result.add((String)obj);
            } else if (obj instanceof JSONObject) {
                JSONArray rls = ((JSONObject)obj).getJSONArray("rules");
                List<Rule> rules = new ArrayList<Rule>();
                for(int j=0; j<rls.length(); j++) {
                    rules.add(new Rule(rls.getJSONObject(j)));
                }

                if (Rule.isAllowed(rules)) {
                    Object value = ((JSONObject)obj).get("value");
                    if (value instanceof String) {
                        result.add((String)value);
                    } else if (value instanceof JSONArray) {
                        JSONArray valueArr = (JSONArray)value;
                        for(int j=0; j<valueArr.length(); j++) {
                            result.add(valueArr.getString(j));
                        }
                    }
                }
            }
        }

        return result.toArray(new String[result.size()]);
    }
}
