package io.chaofan.chunklauncher.util;

import java.util.regex.Pattern;

public enum OS {

    LINUX("linux", new String[]{"linux", "unix"}),
    WINDOWS("windows", new String[]{"win"}),
    OSX("osx", new String[]{"mac"}),
    UNKNOWN("unknown", new String[0]);

    private final String name;
    private final String[] aliases;

    OS(String name, String[] aliases) {
        this.name = name;
        this.aliases = (aliases == null ? new String[0] : aliases);
    }

    public String getName() {
        return this.name;
    }

    public String[] getAliases() {
        return this.aliases;
    }

    public static OS getCurrentPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();

        for (OS os : values()) {
            for (String alias : os.getAliases()) {
                if (osName.contains(alias))
                    return os;
            }
        }

        return UNKNOWN;
    }

    public static boolean matchOsNameAndVersion(String osName, String osVersion) {
        if (!osName.equals(getCurrentPlatform().getName())) {
            return false;
        }
        if (osVersion != null && !Pattern.compile(osVersion).matcher(System.getProperty("os.version")).find()) {
            return false;
        }
        return true;
    }

    public static boolean matchOsArch(String osArch) {
        return System.getProperty("os.arch").matches(osArch);
    }
}
