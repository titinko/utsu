package com.utsusynth.utsu.model;

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

import com.google.common.base.Optional;

/**
 * In-code representation of a voice bank. Compatible with oto.ini files. TODO: Support oto_ini.txt
 * as well
 */
public class Voicebank {
	private static final Pattern LYRIC_PATTERN = Pattern.compile("(.+\\.wav)=([^,]*),");

	// TODO: Replace with actual default.
	private static final String DEFAULT_PATH =
			"/Users/emmabreen/Library/UTAU/voice/Japanese/IONA TestDrive2.utau/";

	private String pathToVoicebank; // Example: "/Library/Iona.utau/"
	private String name; // Example: "Iona"
	private String imageName; // Example: "img.bmp"
	private Map<String, LyricConfig> lyricConfigs;

	public static Voicebank loadFromDirectory(String sourceDir) {
		Voicebank voicebank = new Voicebank();

		if (sourceDir.isEmpty()) {
			voicebank.pathToVoicebank = DEFAULT_PATH;
		} else {
			sourceDir = sourceDir.replaceFirst("\\$\\{DEFAULT\\}", DEFAULT_PATH)
					.replaceFirst("\\$\\{HOME\\}", System.getProperty("user.home"));
			if (sourceDir.endsWith("//")) {
				voicebank.pathToVoicebank = sourceDir.substring(0, sourceDir.length() - 1);
			} else if (sourceDir.endsWith("/")) {
				voicebank.pathToVoicebank = sourceDir;
			} else {
				voicebank.pathToVoicebank = sourceDir + "/";
			}
			if (!(new File(voicebank.pathToVoicebank).isDirectory())) {
				voicebank.pathToVoicebank = DEFAULT_PATH;
			}
		}
		System.out.println("Parsed voicebank as " + voicebank.pathToVoicebank);

		// Parse character data.
		String characterData = readConfigFile(voicebank.pathToVoicebank, "character.txt");
		for (String rawLine : characterData.split("\n")) {
			String line = rawLine.trim();
			if (line.startsWith("name=")) {
				voicebank.name = line.substring("name=".length());
			} else if (line.startsWith("image=")) {
				voicebank.imageName = line.substring("image=".length());
			}
		}

		// Parse oto_ini.txt and oto.ini, with oto.ini overriding oto_ini.txt.
		parseOtoIni(voicebank.pathToVoicebank, "oto_ini.txt", voicebank.lyricConfigs);
		parseOtoIni(voicebank.pathToVoicebank, "oto.ini", voicebank.lyricConfigs);
		return voicebank;
	}

	private static void parseOtoIni(
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
				lyricConfigs.put(lyricName,
						new LyricConfig(pathToVoicebank + fileName, fileName
								.substring(fileName.lastIndexOf("/") + 1, fileName.indexOf('.')),
								configValues));
			}
		}
	}

	private static String readConfigFile(String path, String fileName) {
		File file = new File(path + fileName);
		if (!file.canRead()) {
			// This is often okay.
			System.out.println("Unable to find file: " + fileName);
			return "";
		}
		try {
			String charset = "UTF-8";
			CharsetDecoder utf8Decoder =
					Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPORT)
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

	private Voicebank() {
		// Set defaults for name, image path.
		this.name = "";
		this.imageName = "";
		this.lyricConfigs = new HashMap<>();
	}

	public Optional<LyricConfig> getLyricConfig(String lyric) {
		if (lyricConfigs.keySet().contains(lyric)) {
			return Optional.of(lyricConfigs.get(lyric));
		}
		return Optional.absent();
	}

	public String getName() {
		return name;
	}

	public String getImagePath() {
		return pathToVoicebank + imageName;
	}

	@Override
	public String toString() {
		// Crappy string representation of a Voicebank object.
		String result = "";
		for (String lyric : lyricConfigs.keySet()) {
			result += lyric + " = " + lyricConfigs.get(lyric) + "\n";
		}
		return result + " " + pathToVoicebank + " " + name + " " + imageName;
	}
}
