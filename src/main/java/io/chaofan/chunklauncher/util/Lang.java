package io.chaofan.chunklauncher.util;

import io.chaofan.chunklauncher.Config;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * This class contains mothods about ui localization.
 * Localization files is contained in 'lang/' folder, with the
 * files '*.lang'.
 *
 * @author Chaos
 * @since ChunkLauncher 1.3.2
 */
public class Lang {

    private static final String LANGUAGE_PRIMARY = Config.language;

    private static final String LANGUAGE_SECONDARY =
            System.getProperty("user.language", "en");

    private static final String LANGUAGE_DEFAULT = "en";

    private static final String SYSTEM_LANG_FILE_PRIMARY = "/lang/" +
            LANGUAGE_PRIMARY + ".lang";

    private static final String SYSTEM_LANG_FILE_SECONDARY = "/lang/" +
            LANGUAGE_SECONDARY + ".lang";

    private static final String SYSTEM_LANG_FILE_DEFAULT = "/lang/" +
            LANGUAGE_DEFAULT + ".lang";

    private static Properties langContentDefault = new Properties();
    private static Properties langContentSecondary = new Properties(langContentDefault);
    private static Properties langContentPrimary = new Properties(langContentSecondary);

    /**
     * Use an id to get the localized string.
     *
     * @param id The id, for example 'msg.auth.succeeded'
     * @return Localized string
     */
    public static String getString(String id) {
        String result = langContentPrimary.getProperty(id);
        return result == null ? id : result;
    }

    /**
     * Register a string for language.
     *
     * @param language The language, for example 'en', 'zh-CN'.
     * @param id       The id, for example 'msg.auth.succeeded'
     * @param value    Localized string
     */
    public static void registerString(String language, String id, String value) {
        language = language.toLowerCase();
        if (language.equals(LANGUAGE_DEFAULT.toLowerCase())) {
            langContentDefault.setProperty(id, value);
        }
        if (language.equals(LANGUAGE_SECONDARY.toLowerCase())) {
            langContentSecondary.setProperty(id, value);
        }
        if (language.equals(LANGUAGE_PRIMARY.toLowerCase())) {
            langContentPrimary.setProperty(id, value);
        }
    }

    public static Language[] getAvailableLanguages() {
        List<Language> languageList = new ArrayList<>();
        for (String langFile : ClassUtil.listResources("lang")) {
            if (langFile.endsWith(".lang")) {
                String langValue = langFile.substring(0, langFile.length() - 5);
                Properties properties = new Properties();
                if (loadLangResource("/lang/" + langFile, properties)) {
                    languageList.add(new Language(properties.getProperty("name", langValue), langValue));
                }
            }
        }

        languageList.sort(Comparator.comparing(a -> a.displayName));
        return languageList.toArray(new Language[0]);
    }

    private static void loadDefaultLangFile() {
        if (!loadLangResource(SYSTEM_LANG_FILE_PRIMARY, langContentPrimary)) {
            if (!loadLangResource(SYSTEM_LANG_FILE_SECONDARY, langContentPrimary))
                loadLangResource(SYSTEM_LANG_FILE_DEFAULT, langContentPrimary);
            else
                loadLangResource(SYSTEM_LANG_FILE_DEFAULT, langContentSecondary);
        } else {
            if (!loadLangResource(SYSTEM_LANG_FILE_SECONDARY, langContentSecondary))
                loadLangResource(SYSTEM_LANG_FILE_DEFAULT, langContentSecondary);
            else
                loadLangResource(SYSTEM_LANG_FILE_DEFAULT, langContentDefault);
        }
    }

    private static boolean loadLangResource(String resource, Properties properties) {
        try {
            properties.load(Lang.class.getResourceAsStream(resource));
            String encoding = properties.getProperty("encoding", null);
            if (encoding != null) {
                properties.load(new InputStreamReader(Lang.class.getResourceAsStream(resource), encoding));
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    static {
        loadDefaultLangFile();
    }
}
