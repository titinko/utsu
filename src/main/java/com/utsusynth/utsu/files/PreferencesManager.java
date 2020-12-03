package com.utsusynth.utsu.files;

import com.google.common.collect.ImmutableMap;
import com.utsusynth.utsu.UtsuModule.SettingsPath;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.model.config.Theme;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Locale;

public class PreferencesManager {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private final File preferencesFile;
    private final DocumentBuilderFactory documentBuilderFactory;
    private final TransformerFactory transformerFactory;
    private final HashMap<String, String> preferences;
    private final ImmutableMap<String, String> defaultPreferences;

    public PreferencesManager(
            @SettingsPath File settingsPath,
            DocumentBuilderFactory documentBuilderFactory,
            TransformerFactory transformerFactory,
            ImmutableMap<String, String> defaultPreferences) {
        preferencesFile = new File(settingsPath, "preferences.xml");
        this.documentBuilderFactory = documentBuilderFactory;
        this.transformerFactory = transformerFactory;
        preferences = new HashMap<>();
        this.defaultPreferences = defaultPreferences;
    }

    public void initializePreferences() {
        if (!preferencesFile.canRead()) {
            return;
        }
        try {
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Node root = documentBuilder.parse(preferencesFile).getDocumentElement();
            if (!root.getNodeName().equals("preferences")) {
                System.out.println("Error: Unexpected format for preferences file.");
                return;
            }
            NodeList elements = root.getChildNodes();
            for (int i = 0; i < elements.getLength(); i++) {
                Node element = elements.item(i);
                preferences.put(element.getNodeName(), element.getTextContent());
            }
        } catch (Exception e) {
            errorLogger.logError(e);
        }
    }

    public boolean hasPreferencesFile() {
        return preferencesFile.exists();
    }

    public void saveToFile() {
        if (!preferencesFile.exists() && !preferencesFile.getParentFile().exists()) {
            System.out.println("Error: Could not find settings directory.");
            return;
        }
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document document = builder.newDocument();
            Node root = document.createElement("preferences");
            document.appendChild(root);
            for (String key : preferences.keySet()) {
                Node element = document.createElement(key);
                element.setTextContent(preferences.get(key));
                root.appendChild(element);
            }
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new FileWriter(preferencesFile));
            transformer.transform(source, result);
        } catch (Exception e) {
            errorLogger.logError(e);
        }
    }

    public Theme getTheme() {
        return preferences.containsKey("theme")
                ? new Theme(preferences.get("theme")) : new Theme(defaultPreferences.get("theme"));
    }

    public void setTheme(Theme newTheme) {
        preferences.put("theme", newTheme.getId());
    }

    public enum AutoscrollMode {
        DISABLED, ENABLED_END, ENABLED_MIDDLE,
    }

    public AutoscrollMode getAutoscroll() {
        String autoscrollName = preferences.containsKey("autoscroll")
                ? preferences.get("autoscroll") : defaultPreferences.get("autoscroll");
        try {
            return AutoscrollMode.valueOf(autoscrollName);
        } catch (IllegalArgumentException e) {
            errorLogger.logError(e);
            return AutoscrollMode.valueOf(defaultPreferences.get("autoscroll"));
        }
    }

    public void setAutoscroll(AutoscrollMode autoscrollMode) {
        preferences.put("autoscroll", autoscrollMode.name());
    }

    public enum AutoscrollCancelMode {
        DISABLED, ENABLED
    }

    public AutoscrollCancelMode getAutoscrollCancel() {
        String autoscrollCancelName = preferences.containsKey("autoscrollCancel")
                ? preferences.get("autoscrollCancel") : defaultPreferences.get("autoscrollCancel");
        try {
            return AutoscrollCancelMode.valueOf(autoscrollCancelName);
        } catch (IllegalArgumentException e) {
            errorLogger.logError(e);
            return AutoscrollCancelMode.valueOf(defaultPreferences.get("autoscrollCancel"));
        }
    }

    public void setAutoscrollCancel(AutoscrollCancelMode autoscrollCancelMode) {
        preferences.put("autoscrollCancel", autoscrollCancelMode.name());
    }

    public NativeLocale getLocale() {
        String localeCode = preferences.containsKey("locale")
                ? preferences.get("locale") : defaultPreferences.get("locale");
        // Workaround since Locale class can't parse its own string representation.
        Locale locale = localeCode.contains("-")
                ? new Locale(localeCode.substring(0, 2), localeCode.substring(3))
                : new Locale(localeCode);
        return new NativeLocale(locale);
    }

    public void setLocale(NativeLocale locale) {
        preferences.put("locale", locale.getLocale().toLanguageTag());
    }

    public enum CacheMode {
        DISABLED, ENABLED
    }

    public CacheMode getCache() {
        String cacheName = preferences.containsKey("cache")
                ? preferences.get("cache") : defaultPreferences.get("cache");
        try {
            return CacheMode.valueOf(cacheName);
        } catch (IllegalArgumentException e) {
            errorLogger.logError(e);
            return CacheMode.valueOf(defaultPreferences.get("cache"));
        }
    }

    public void setCache(CacheMode cacheMode) {
        preferences.put("cache", cacheMode.name());
    }

    public File getResampler() {
        File resampler = preferences.containsKey("resampler")
                ? new File(preferences.get("resampler"))
                : new File(defaultPreferences.get("resampler"));
        if (!resampler.canExecute()) {
            return new File(defaultPreferences.get("resampler"));
        }
        return resampler;
    }

    public File getResamplerDefault() {
        return new File(defaultPreferences.get("resampler"));
    }

    public void setResampler(File resampler) {
        preferences.put("resampler", resampler.getAbsolutePath());
    }

    public File getWavtool() {
        File wavtool = preferences.containsKey("wavtool")
                ? new File(preferences.get("wavtool"))
                : new File(defaultPreferences.get("wavtool"));
        if (!wavtool.canExecute()) {
            return new File(defaultPreferences.get("wavtool"));
        }
        return wavtool;
    }

    public File getWavtoolDefault() {
        return new File(defaultPreferences.get("wavtool"));
    }

    public void setWavtool(File wavtool) {
        preferences.put("wavtool", wavtool.getAbsolutePath());
    }

    public File getVoicebank() {
        File voicebank = preferences.containsKey("voicebank")
                ? new File(preferences.get("voicebank"))
                : new File(defaultPreferences.get("voicebank"));
        if (!voicebank.isDirectory()) {
            return new File(defaultPreferences.get("voicebank"));
        }
        return voicebank;
    }

    public File getVoicebankDefault() {
        return new File(defaultPreferences.get("voicebank"));
    }

    public void setVoicebank(File voicebank) {
        preferences.put("voicebank", voicebank.getAbsolutePath());
    }
}
