package com.utsusynth.utsu.model.voicebank;

import java.io.File;

public class CharacterData {

    private File pathToVoicebank; // Example: "/Library/Iona.utau/"
    private String name; // Example: "Iona"
    private String author; // Example: "Lethe"
    private String description; // Contents of readme.txt
    private String imageName; // Example: "img.bmp"
    private String sampleName;

    public CharacterData(File pathToVoicebank) {
        // Default values.
        this.pathToVoicebank = pathToVoicebank;
        this.name = pathToVoicebank.getName();
        this.author = "";
        this.description = "";
        this.imageName = "";
        this.sampleName = "";
    }

    public CharacterData setPathToVoicebank(File pathToVoicebank) {
        this.pathToVoicebank = pathToVoicebank;
        return this;
    }

    public CharacterData setName(String name) {
        this.name = name;
        return this;
    }

    public CharacterData setAuthor(String author) {
        this.author = author;
        return this;
    }

    public CharacterData setDescription(String description) {
        this.description = description;
        return this;
    }

    public CharacterData setImageName(String imageName) {
        this.imageName = imageName;
        return this;
    }

    public CharacterData setSampleName(String sampleName) {
        this.sampleName = sampleName;
        return this;
    }

    public File getPathToVoicebank() {
        return pathToVoicebank;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getImageName() {
        return imageName;
    }

    public String getSampleName() {
        return sampleName;
    }

    public String getImagePath() {
        return new File(pathToVoicebank, imageName).getAbsolutePath();
    }

    public String getSamplePath() {
        return new File(pathToVoicebank, sampleName).getAbsolutePath();
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        // Crappy string representation of a Voicebank object.
        String result = "";
        return result + " " + pathToVoicebank + " " + name + " " + imageName;
    }
}