package com.utsusynth.utsu.files;

import com.google.common.collect.ImmutableMap;
import com.utsusynth.utsu.UtsuModule.SettingsPath;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.i18n.Localizer;
import com.utsusynth.utsu.common.i18n.NativeLocale;
import com.utsusynth.utsu.model.config.Theme;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
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

import static com.utsusynth.utsu.files.ThemeManager.DEFAULT_LIGHT_THEME;

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

    public NativeLocale getLocale() {
        String localeCode = preferences.containsKey("locale")
                ? preferences.get("locale") : defaultPreferences.get("locale");
        return new NativeLocale(new Locale(localeCode));
    }

    public void setLocale(NativeLocale locale) {
        preferences.put("locale", locale.getLocale().toLanguageTag());
    }
}
