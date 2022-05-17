package com.utsusynth.utsu.common.utils;

import com.google.common.collect.ImmutableList;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class UtsuFileUtils {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    public static String guessCharset(byte[] bytes) {
        CharsetDecoder utf8Decoder =
                StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            utf8Decoder.decode(ByteBuffer.wrap(bytes));
        } catch (CharacterCodingException e) {
            return "SJIS";
        }
        return "UTF-8";
    }

    public static String readConfigFile(File file) {
        if (!file.canRead() || !file.isFile()) {
            // This is often okay.
            return "";
        }
        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            String charset = guessCharset(bytes);
            return new String(bytes, charset);
        } catch (IOException e) {
            // TODO Handle this.
            errorLogger.logError(e);
        }
        return "";
    }

    public static Node readYamlFile(File file) {
        Yaml yaml = new Yaml();
        Node node = yaml.load("");
        if (!file.canRead()) {
            return node;
        }
        try {
            node = yaml.represent(yaml.load(FileUtils.openInputStream(file)));
        } catch (IOException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }
        return node;
    }

    public static List<NodeTuple> getYamlMapEntries(Node yamlNode) {
        if (!(yamlNode instanceof MappingNode)) {
            return ImmutableList.of();
        }
        return ((MappingNode) yamlNode).getValue();
    }

    public static List<Node> getYamlListEntries(Node yamlNode) {
        if (!(yamlNode instanceof SequenceNode)) {
            return ImmutableList.of();
        }
        return ((SequenceNode) yamlNode).getValue();
    }

    public static String getYamlStringValue(Node yamlNode, String fallback) {
        if (!(yamlNode instanceof ScalarNode)) {
            return fallback;
        }
        return ((ScalarNode) yamlNode).getValue();
    }

    public static Integer getYamlIntValue(Node yamlNode, Integer fallback) {
        if (!(yamlNode instanceof ScalarNode)) {
            return fallback;
        }
        return RoundUtils.round(Double.parseDouble(((ScalarNode) yamlNode).getValue()));
    }

    public static double getYamlDoubleValue(Node yamlNode, double fallback) {
        if (!(yamlNode instanceof ScalarNode)) {
            return fallback;
        }
        return Double.parseDouble(((ScalarNode) yamlNode).getValue());
    }
}
