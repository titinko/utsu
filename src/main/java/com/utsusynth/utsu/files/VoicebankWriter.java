package com.utsusynth.utsu.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.model.voicebank.Voicebank;

public class VoicebankWriter {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();
    private static CharsetEncoder sjisEncoder =
            Charset.forName("SJIS").newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);

    public void writeVoicebankToDirectory(Voicebank voicebank, File saveDir) {
        // Save character.txt.
        File characterFile = saveDir.toPath().resolve("character.txt").toFile();
        String voiceData = voicebank.getName() + voicebank.getAuthor() + voicebank.getImagePath();
        try (PrintStream ps = new PrintStream(characterFile, getCharset(voiceData))) {
            ps.println("name=" + voicebank.getName());
            ps.println("author=" + voicebank.getAuthor());
            ps.println("image=" + voicebank.getImageName());
            ps.flush();
            ps.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }

        // Save readme.txt.
        File readmeFile = saveDir.toPath().resolve("readme.txt").toFile();
        String description = voicebank.getDescription();
        try (PrintStream ps = new PrintStream(readmeFile, getCharset(description))) {
            ps.print(description);
            ps.flush();
            ps.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }

        // TODO: Save lyric configs.
        // TODO: Save pitch map.
    }

    private String getCharset(String toRender) {
        // Default to Shift-JIS unless there are Unicode-only characters.
        String charset = "SJIS";
        try {
            sjisEncoder.encode(CharBuffer.wrap(toRender.toCharArray()));
        } catch (MalformedInputException | UnmappableCharacterException e) {
            charset = "UTF-8";
        } catch (IOException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
        }
        return charset;
    }
}
