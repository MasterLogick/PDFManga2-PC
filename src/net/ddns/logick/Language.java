package net.ddns.logick;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class Language {
    private static Properties lang;

    public static void loadLocale() throws IOException {
        lang = new Properties();
        lang.load(new InputStreamReader(Language.class.getResourceAsStream("lang_" + Main.CURRENT_LANGUAGE + ".properties"), StandardCharsets.UTF_8));
    }

    public static String get(String key) {
        return lang.getProperty(key, key);
    }
}
