package io.chaofan.chunklauncher.process;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import io.chaofan.chunklauncher.Launcher;
import io.chaofan.chunklauncher.util.OS;
import io.chaofan.chunklauncher.version.Library;
import io.chaofan.chunklauncher.version.RunnableModule;
import org.json.JSONObject;
import io.chaofan.chunklauncher.Config;
import io.chaofan.chunklauncher.auth.ServerAuth;
import io.chaofan.chunklauncher.util.Lang;

public class Runner {

    private final RunnableModule module;
    private final List<String> params = new ArrayList<>();
    private final ServerAuth auth;

    public Runner(RunnableModule module, ServerAuth auth) {
        this.module = module;
        this.auth = auth;
    }

    public boolean prepare() {

        String java = Config.jrePath + "/bin/java";

        java = java.replace('/', System.getProperty("file.separator").charAt(0));

        String javaraw = java;

        if (OS.getCurrentPlatform() == OS.WINDOWS) {
            if (new File(java + "w.exe").exists()) {
                java += "w.exe";
            } else {
                if (!new File(java + ".exe").exists()) {
                    System.out.println(Lang.getString("msg.jrepath.error"));
                    return false;
                }
            }
        } else {
            if (!new File(java).exists()) {
                System.out.println(Lang.getString("msg.jrepath.error"));
                return false;
            }
        }

        Map<String, String> valueMap = new HashMap<>();

        valueMap.put("auth_access_token", auth.getAccessToken());
        valueMap.put("user_properties", new JSONObject(auth.getUserProperties()).toString());
        valueMap.put("clientid", auth.getClientId());

        valueMap.put("auth_session", auth.getSession());

        valueMap.put("auth_player_name", auth.getPlayerName());
        valueMap.put("auth_uuid", auth.getUuid());
        valueMap.put("auth_xuid", auth.getUuid()); // TODO find out what should be correct value
        valueMap.put("user_type", auth.getUserType());

        valueMap.put("profile_name", Config.currentProfile.profileName);
        valueMap.put("version_name", module.getName());

        valueMap.put("game_directory", Config.currentProfile.runPath.directory);
        valueMap.put("game_assets", Config.gamePath + Config.MINECRAFT_VIRTUAL_PATH + "/" + module.getAssetsIndex());

        valueMap.put("assets_root", Config.gamePath + Config.MINECRAFT_ASSET_PATH);
        valueMap.put("assets_index_name", module.getAssetsIndex());

        valueMap.put("version_type", module.getType());

        String arch = isJre64Bit(javaraw) ? "64" : "32";

        if (Config.d64)
            arch = "64";
        if (Config.d32)
            arch = "32";

        valueMap.put("natives_directory", module.getNativePath(arch));

        valueMap.put("launcher_name", "ChunkLauncher");
        valueMap.put("launcher_version", Launcher.VERSION);

        valueMap.put("classpath", module.getClassPath());
        valueMap.put("classpath_separator", System.getProperty("path.separator"));
        valueMap.put("library_directory", Library.getLibraryDirectory());

        params.add(java);

        params.add("-Xmx" + Config.memory + "M");
        params.add("-Xms" + Config.memory + "M");

        if (Config.d64)
            params.add("-d64");
        if (Config.d32)
            params.add("-d32");

        Set<String> providedFeatures = new HashSet<>();

        String[] jvmParams = module.getJvmParams(providedFeatures);
        if (!replaceParams(jvmParams, valueMap)) {
            return false;
        }

        params.addAll(Arrays.asList(jvmParams));
        params.add(module.getMainClass());

        String[] gameParams = module.getRunningParams(providedFeatures);
        if (!replaceParams(gameParams, valueMap)) {
            return false;
        }

        if (Config.enableProxy && Config.proxy != null) {
            if (Config.proxyType.equals("Socks")) {
                params.add("--proxyHost");
                params.add(Config.proxyHost);
                params.add("--proxyPort");
                params.add(String.valueOf(Config.proxyPort));
            } else {
                JOptionPane.showMessageDialog(null, Lang.getString("msg.proxy.notsocks"), "ChunkLauncher", JOptionPane.WARNING_MESSAGE);
            }
        }

        if (module.isAssetsVirtual() && !module.copyAssetsToVirtual()) {
            System.out.println(Lang.getString("msg.assets.cannotload"));
            return false;
        }

        params.addAll(Arrays.asList(gameParams));

        return true;
    }

    private static final Pattern paramPattern = Pattern.compile("\\$\\{([a-zA-Z0-9_]*)}");

    private boolean replaceParams(String[] gameParams, Map<String, String> map) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < gameParams.length; i++) {
            String param = gameParams[i];
            Matcher m = paramPattern.matcher(param);
            int lastend = 0;
            sb.setLength(0);

            while (m.find()) {
                sb.append(param, lastend, m.start());
                String key = m.group(1);
                String value = map.get(key);

                if (value == null) {
                    System.out.println(Lang.getString("msg.run.unknownparam1") +
                            key + Lang.getString("msg.run.unknownparam2"));
                    return false;
                }

                sb.append(value);
                lastend = m.end();
            }

            sb.append(param, lastend, param.length());
            gameParams[i] = sb.toString();
        }
        return true;
    }

    public void start() {
        try {
            ProcessBuilder pb = new ProcessBuilder(params.toArray(new String[0]));
            pb.directory(new File(Config.gamePath));
            pb.redirectErrorStream(true);

            Process p = pb.start();
            boolean windowOpened = false;

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("last.log")));
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));

            out.write("Running with params : " + params + "\r\n");

            String line;
            while ((line = in.readLine()) != null) {
                out.write(line + "\n");
                System.out.println(line);
                if (checkWindowOpened(line)) {
                    if (!Config.showDebugInfo) {
                        out.close();
                        return;
                    }
                    windowOpened = true;
                }
            }
            out.close();

            if (!windowOpened) {
                JOptionPane.showMessageDialog(null, Lang.getString("msg.run.failed"), "ChunkLauncher", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Launcher.exceptionReport(e);
        }
    }

    private boolean checkWindowOpened(String line) {
        String moduleName = module.getName();
        if (moduleName.startsWith("a") || moduleName.startsWith("b") || moduleName.equals("1.0") || moduleName.equals("1.1")) {
            return line.toLowerCase().contains("initializing lwjgl");
        } else if (moduleName.charAt(0) < '0' || moduleName.charAt(0) > '9') {
            return true;
        }
        return line.toLowerCase().contains("lwjgl version");
    }

    private boolean isJre64Bit(String java) {
        try {
            ProcessBuilder pb = new ProcessBuilder(java, "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();

            InputStream in = p.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];
            int n;

            while ((n = in.read(buffer)) >= 0) {
                sb.append(new String(buffer, 0, n));
            }

            return sb.toString().toLowerCase().contains("64-bit");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
