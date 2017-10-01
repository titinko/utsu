package com.utsusynth.utsu.model.voicebank;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class VoicebankReader {
	private static final Pattern LYRIC_PATTERN = Pattern.compile("(.+\\.wav)=([^,]*),");

	// TODO: Replace with actual default.
	private static final String DEFAULT_PATH =
			"/Users/emmabreen/Library/UTAU/voice/Japanese/IONA TestDrive2.utau/";

	public Voicebank loadFromDirectory(String sourceDir) {
		String pathToVoicebank;
		String name = "";
		String imageName = "";
		Map<String, LyricConfig> lyricConfigs = new HashMap<>();

		if (sourceDir.isEmpty()) {
			pathToVoicebank = DEFAULT_PATH;
		} else {
			sourceDir = sourceDir.replaceFirst("\\$\\{DEFAULT\\}", DEFAULT_PATH).replaceFirst(
					"\\$\\{HOME\\}",
					System.getProperty("user.home"));
			if (sourceDir.endsWith("//")) {
				pathToVoicebank = sourceDir.substring(0, sourceDir.length() - 1);
			} else if (sourceDir.endsWith("/")) {
				pathToVoicebank = sourceDir;
			} else {
				pathToVoicebank = sourceDir + "/";
			}
			if (!(new File(pathToVoicebank).isDirectory())) {
				pathToVoicebank = DEFAULT_PATH;
			}
		}
		System.out.println("Parsed voicebank as " + pathToVoicebank);

		// Parse character data.
		String characterData = readConfigFile(pathToVoicebank, "character.txt");
		for (String rawLine : characterData.split("\n")) {
			String line = rawLine.trim();
			if (line.startsWith("name=")) {
				name = line.substring("name=".length());
			} else if (line.startsWith("image=")) {
				imageName = line.substring("image=".length());
			}
		}

		// Parse oto_ini.txt and oto.ini, with oto.ini overriding oto_ini.txt.
		parseOtoIni(pathToVoicebank, "oto_ini.txt", lyricConfigs);
		parseOtoIni(pathToVoicebank, "oto.ini", lyricConfigs);
		return new Voicebank(pathToVoicebank, name, imageName, lyricConfigs);
	}

	private void parseOtoIni(
			String pathToVoicebank,
			String otoFile,
			Map<String, LyricConfig> lyricConfigs) {
		String otoData = readConfigFile(pathToVoicebank, otoFile);
		for (String rawLine : otoData.split("\n")) {
			String line = rawLine.trim();
			Matcher matcher = LYRIC_PATTERN.matcher(line);
			if (matcher.find()) {
				String fileName = matcher.group(1); // Assuming this is a .wav file
				String lyricName = matcher.group(2);
				String[] configValues = line.substring(matcher.end()).split(",");
				if (configValues.length != 5 || fileName == null || lyricName == null) {
					System.out.println("Received unexpected results while parsing oto.ini");
					continue;
				}
				lyricConfigs.put(
						lyricName,
						new LyricConfig(
								pathToVoicebank + fileName,
								fileName.substring(
										fileName.lastIndexOf("/") + 1,
										fileName.indexOf('.')),
								configValues));
			}
		}
	}

	private String readConfigFile(String path, String fileName) {
		File file = new File(path + fileName);
		if (!file.canRead()) {
			// This is often okay.
			System.out.println("Unable to find file: " + fileName);
			return "";
		}
		try {
			String charset = "UTF-8";
			CharsetDecoder utf8Decoder = Charset
					.forName("UTF-8")
					.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);
			try {
				utf8Decoder.decode(ByteBuffer.wrap(FileUtils.readFileToByteArray(file)));
			} catch (MalformedInputException | UnmappableCharacterException e) {
				charset = "SJIS";
			}
			return FileUtils.readFileToString(file, charset);
		} catch (IOException e) {
			// TODO Handle this.
			e.printStackTrace();
		}
		return "";
	}
}
