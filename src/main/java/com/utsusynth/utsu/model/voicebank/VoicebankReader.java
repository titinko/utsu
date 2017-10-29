package com.utsusynth.utsu.model.voicebank;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.ImmutableSet;

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
		String characterData = readConfigFile(new File(pathToVoicebank).toPath(), "character.txt");
		for (String rawLine : characterData.split("\n")) {
			String line = rawLine.trim();
			if (line.startsWith("name=")) {
				name = line.substring("name=".length());
			} else if (line.startsWith("image=")) {
				imageName = line.substring("image=".length());
			}
		}

		// Parse all oto_ini.txt and oto.ini files in arbitrary order.
		try {
			Files.walkFileTree(
					new File(pathToVoicebank).toPath(),
					EnumSet.of(FileVisitOption.FOLLOW_LINKS),
					10,
					new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path path, BasicFileAttributes attr) {
							for (String otoName : ImmutableSet.of("oto.ini", "oto_ini.txt")) {
								if (path.endsWith(otoName)) {
									Path pathToFile = path.toFile().getParentFile().toPath();
									parseOtoIni(pathToFile, otoName, lyricConfigs);
									break;
								}
							}
							return FileVisitResult.CONTINUE;
						}
					});
		} catch (IOException e) {
			// TODO: Handle this.
			e.printStackTrace();
		}
		return new Voicebank(pathToVoicebank, name, imageName, lyricConfigs);
	}

	private void parseOtoIni(
			Path pathToOtoFile,
			String otoFile,
			Map<String, LyricConfig> lyricConfigs) {
		String otoData = readConfigFile(pathToOtoFile, otoFile);
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
								pathToOtoFile.resolve(fileName).toFile().getAbsolutePath(),
								fileName.substring(
										fileName.lastIndexOf("/") + 1,
										fileName.indexOf('.')),
								configValues));
			}
		}
	}

	private String readConfigFile(Path path, String fileName) {
		File file = path.resolve(fileName).toFile();
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
