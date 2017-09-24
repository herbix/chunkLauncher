package io.chaofan.chunklauncher.version;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.json.JSONArray;
import org.json.JSONObject;

public class RunnableModuleInfo {

    public String id;
    public String[] minecraftArguments;
    public String time;
    public String mainClass;
    public String releaseTime;
    public String type;
    public String incompatibilityReason;
    public List<Library> libraries;
    public List<Rule> rules;
    public String assets;
    public String inheritsFrom;
    public Stack<RunnableModule> inhertStack;
    public String jar;
    public DownloadInfo assetIndex;
    public Map<String, DownloadInfo> downloads;

    public RunnableModuleInfo(JSONObject json) {
        id = json.getString("id");
        minecraftArguments = json.getString("minecraftArguments").split("[\\s]+");
        time = json.getString("time");
        mainClass = json.getString("mainClass");
        releaseTime = json.getString("releaseTime");
        type = json.getString("type");
        if(json.has("incompatibilityReason"))
            incompatibilityReason = json.getString("incompatibilityReason");
        if(json.has("inheritsFrom")) {
            inheritsFrom = json.getString("inheritsFrom");
            inhertStack = new Stack<RunnableModule>();
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
        }

        if(json.has("assetIndex")) {
            this.assetIndex = new DownloadInfo("assetIndex", json.getJSONObject("assetIndex"));
        }
    }

    public boolean canRunInThisOS() {
        return Rule.isAllowed(rules);
    }

    public void addInheritedInfo(RunnableModuleInfo inherited) {
        this.libraries.addAll(inherited.libraries);
        if (this.assets.equals("legacy")) {
            this.assets = inherited.assets;
        }
        if (this.assetIndex == null) {
            this.assetIndex = inherited.assetIndex;
        }
    }
}
