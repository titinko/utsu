package com.utsusynth.utsu.files;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class VoicebankFileManager {

    public ArrayList<File> getVoiceBankDirs(File path) {
        ArrayList<File> voiceBankDirs = new ArrayList<>();
        getVoiceBankDirs(voiceBankDirs, path);
        return voiceBankDirs;
    }

    private void getVoiceBankDirs(ArrayList<File> voiceBankDirs, File path) {

        // Recurse through child directories
        String[] directories = path.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });

        if (directories != null & directories.length > 0) {
            // Run this for each child directory
            for (String dir : directories) {
                getVoiceBankDirs(voiceBankDirs, new File(path.getPath() + "/" + dir));
            }
        }

        // Finally, see if this is a voice bank
        File otoFile = new File(path + "/oto.ini");

        if (otoFile.exists()) {

            // Make sure it also has wav files
            String[] wavFiles = path.list(new FilenameFilter() {
                @Override
                public boolean accept(File current, String name) {
                    return name.endsWith(".wav");
                }
            });

            if (wavFiles != null && wavFiles.length > 0) {
                voiceBankDirs.add(path);
            }
        }
    }
}