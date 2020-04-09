package com.utsusynth.utsu.files;

import java.util.concurrent.ConcurrentHashMap;

public class FileNameMapper {

    private static FileNameMapper _this = new FileNameMapper();
    private ConcurrentHashMap<String, String> _fileMap = new ConcurrentHashMap<>();
    private boolean _isWindows;

    private FileNameMapper() {
        String os = System.getProperty("os.name").toLowerCase();
        _isWindows = os.contains("win");
    }

    public static FileNameMapper getInstance() {
        return _this;
    }

    public String getOSName(String path) {

        if (!_isWindows)
            return path;

        if (_fileMap.containsKey(path)) {
            return _fileMap.get(path);
        }

        // Windows filenames are not reliable when calling external executables
        try {
            Process process = Runtime.getRuntime().exec("cmd /c for %I in (\"" + path + "\") do @echo %~fsI");

            process.waitFor();

            byte[] data = new byte[65536];
            int size = process.getInputStream().read(data);

            if (size <= 0) {
                return path;
            }

            String dosPath = new String(data, 0, size).replaceAll("\\r\\n", "");

            if (!_fileMap.containsKey(path)) {
                try {
                    _fileMap.put(path, dosPath);
                } catch (Exception f) {
                    // Does this throw an exception if this is already there??
                }
            }

            return dosPath;
        } catch (Exception e) {
            return path;
        }
    }
}