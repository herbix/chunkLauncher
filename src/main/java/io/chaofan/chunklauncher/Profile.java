package io.chaofan.chunklauncher;

import java.io.IOException;

import io.chaofan.chunklauncher.auth.AuthType;
import io.chaofan.chunklauncher.util.Lang;

import java.util.Base64;

public class Profile {

    public String profileName;

    public String user = "";
    public String pass = "";
    public boolean savePass = false;

    public String authType = "yggdrasil";

    public String version = "";

    public RunningDirectory runPath = Config.directories.get(Config.DEFAULT);

    public Profile(String name, String saved) {
        profileName = name;

        if(saved == null) {
            return;
        }

        String[] split = saved.split(";");
        if(split.length < 6) {
            return;
        }

        user = split[0];
        pass = decodePass(split[1]);
        savePass = split[2].toLowerCase().equals("true");
        authType = split[3];
        version = split[4];
        RunningDirectory directory = Config.directories.get(split[5]);
        if (directory == null) {
            directory = new RunningDirectory(split[5], split[5]);
        }
        runPath = directory;
    }

    public String toSavedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(user);
        sb.append(';');
        sb.append(encodePass(pass));
        sb.append(';');
        sb.append(savePass);
        sb.append(';');
        sb.append(authType);
        sb.append(';');
        sb.append(version);
        sb.append(';');
        sb.append(runPath);
        return sb.toString();
    }

    public void updateToFrame(LauncherFrame frame) {
        frame.user.setText(user);
        frame.pass.setText(pass);
        frame.savePass.setSelected(savePass);
        frame.authType.setSelectedItem(AuthType.valueOf(authType));
        frame.gameVersion.setSelectedItem(version);
        frame.runPathDirectories.setSelectedItem(runPath);
        frame.profileDetailLabel.setText(getDetailString());
    }

    public String getDetailString() {
        return "<html><body><p>" +
                Lang.getString("ui.username.label") + " " + user + "</p><p>" +
                Lang.getString("ui.auth.type.label") + " " + String.valueOf(AuthType.valueOf(authType)) + "</p><p>" +
                Lang.getString("ui.version.label") + " " + version + "</p>" +
                "</body></html>";
    }

    public void updateFromFrame(LauncherFrame frame) {
        user = frame.user.getText();
        savePass = frame.savePass.isSelected();
        if(savePass)
            pass = frame.pass.getText();
        else
            pass = "";
        authType = ((AuthType)frame.authType.getSelectedItem()).value();
        if(frame.gameVersion.getSelectedItem() != null)
            version = frame.gameVersion.getSelectedItem().toString();
        if(version == null)
            version = "";
        RunningDirectory runPathSelection = (RunningDirectory) frame.runPathDirectories.getSelectedItem();
        runPath = runPathSelection == null ? Config.directories.get(Config.DEFAULT) : runPathSelection;
    }

    @Override
    public String toString() {
        return profileName;
    }

    private static final byte[] magic = {123, 32, 4, 12, 5, 86, 2, 12};

    private static String encodePass(String pass) {
        byte[] str = pass.getBytes();

        for(int i=0; i<str.length; i++) {
            str[i] ^= magic[i % magic.length];
        }

        String result = Base64.getEncoder().encodeToString(str);
        int equalIndex = result.indexOf('=');
        if(equalIndex > 0) {
            result = result.substring(0, equalIndex);
        }
        return result;
    }

    private static String decodePass(String encoded) {
        while(encoded.length() % 4 != 0)
            encoded += "=";

        byte[] str;

        str = Base64.getDecoder().decode(encoded);

        for(int i=0; i<str.length; i++) {
            str[i] ^= magic[i % magic.length];
        }

        return new String(str);
    }

}
