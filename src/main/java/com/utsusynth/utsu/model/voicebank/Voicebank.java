package com.utsusynth.utsu.model.voicebank;

import com.google.common.base.Optional;
import java.io.File;
import java.util.Map;

/**
 * In-code representation of a voice bank. Compatible with oto.ini files. TODO: Support oto_ini.txt
 * as well
 */
public class Voicebank {
	private File pathToVoicebank; // Example: "/Library/Iona.utau/"
	private String name; // Example: "Iona"
	private String imageName; // Example: "img.bmp"
	private Map<String, LyricConfig> lyricConfigs;

	Voicebank(
			File pathToVoicebank,
			String name,
			String imageName,
			Map<String, LyricConfig> lyricConfigs) {
		this.pathToVoicebank = pathToVoicebank;
		this.name = name;
		this.imageName = imageName;
		this.lyricConfigs = lyricConfigs;
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

	public File getPathToVoicebank() {
		return pathToVoicebank;
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
