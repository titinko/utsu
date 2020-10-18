package com.utsusynth.utsu.files;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles some OS-specific encoding issues in file names.
 */
public class FileNameFixer {
    private final ConcurrentHashMap<String, String> fileMap;

    public FileNameFixer() {
        fileMap = new ConcurrentHashMap<>();
    }

    public String getFixedName(String absolutePath) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return getWindowsPath(absolutePath);
        }
        return absolutePath;
    }

    private String getWindowsPath(String absolutePath) {
        if (fileMap.containsKey(absolutePath)) {
            return fileMap.get(absolutePath);
        }

        // Windows filenames are not reliable when calling external executables.
        try {
            Process process = Runtime.getRuntime().exec(
                    "cmd /c for %I in (\"" + absolutePath + "\") do @echo %~fsI");
            process.waitFor();

            byte[] data = new byte[65536];
            int bytesRead = process.getInputStream().read(data);
            if (bytesRead <= 0) {
                return absolutePath;
            }

            String dosPath = new String(data, 0, bytesRead).replaceAll("\\r\\n", "");
            fileMap.put(absolutePath, dosPath);
            return dosPath;
        } catch (Exception e) {
            return absolutePath;
        }
    }
}
