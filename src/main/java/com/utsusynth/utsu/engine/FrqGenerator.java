package com.utsusynth.utsu.engine;

import java.io.File;

import com.google.inject.Inject;
import com.utsusynth.utsu.files.FileNameMapper;

public class FrqGenerator {
    private final ExternalProcessRunner runner;
    private final File frqGeneratorPath;
    private final int samplesPerFrq; // Samples per value in frq file. Currently always 256.

    @Inject
    public FrqGenerator(ExternalProcessRunner runner, File frqGeneratorPath, int samplesPerFrq) {
        this.runner = runner;
        this.frqGeneratorPath = frqGeneratorPath;
        this.samplesPerFrq = samplesPerFrq;
    }

    public void genFrqFile(File input, File output) {

        FileNameMapper fileUtils = FileNameMapper.getInstance();
        String inputFilePath = fileUtils.getOSName(input.getAbsolutePath());

        runner.runProcess(
                frqGeneratorPath.getAbsolutePath(),
                inputFilePath,
                output.getAbsolutePath(),
                Integer.toString(samplesPerFrq));
    }
}
