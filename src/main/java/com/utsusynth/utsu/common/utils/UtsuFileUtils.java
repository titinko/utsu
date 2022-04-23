package com.utsusynth.utsu.common.utils;

import com.utsusynth.utsu.common.exception.ErrorLogger;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

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
}
