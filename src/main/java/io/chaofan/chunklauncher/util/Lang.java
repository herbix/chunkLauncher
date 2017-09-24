package io.chaofan.chunklauncher.util;

import java.io.InputStreamReader;
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

    private static final String SYSTEM_LANG_FILE_PRIMARY = "/lang/" +
        System.getProperty("user.language", "en") + "-" +
        System.getProperty("user.country", "US") + ".lang";

    private static final String SYSTEM_LANG_FILE_SECONDARY = "/lang/" +
        System.getProperty("user.language", "en") + ".lang";

    private static final String SYSTEM_LANG_FILE_DEFAULT = "/lang/en.lang";

    private static Properties langContentDefault = new Properties();
    private static Properties langContentSecondary = new Properties(langContentDefault);
    private static Properties langContentPrimary = new Properties(langContentSecondary);

    /**
     * Use an id to get the localized string.
     * @param id The id, for example 'msg.auth.succeeded'
     * @return Localized string
     */
    public static String getString(String id) {
        String result = langContentPrimary.getProperty(id);
        return result == null ? id : result;
    }

    private static void loadDefaultLangFile() {
        if(!loadLangResource(SYSTEM_LANG_FILE_PRIMARY, langContentPrimary)) {
            if(!loadLangResource(SYSTEM_LANG_FILE_SECONDARY, langContentPrimary))
                loadLangResource(SYSTEM_LANG_FILE_DEFAULT, langContentPrimary);
            else
                loadLangResource(SYSTEM_LANG_FILE_DEFAULT, langContentSecondary);
        } else {
            if(!loadLangResource(SYSTEM_LANG_FILE_SECONDARY, langContentSecondary))
                loadLangResource(SYSTEM_LANG_FILE_DEFAULT, langContentSecondary);
            else
                loadLangResource(SYSTEM_LANG_FILE_DEFAULT, langContentDefault);
        }
    }

    private static boolean loadLangResource(String resource, Properties properties) {
        try {
            properties.load(Lang.class.getResourceAsStream(resource));
            String encoding = properties.getProperty("encoding", null);
            if(encoding != null) {
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
