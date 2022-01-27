package io.chaofan.chunklauncher.util;

public class Language {
    public final String displayName;
    public final String value;

    public Language(String displayName, String value) {
        this.displayName = displayName;
        this.value = value;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
