package com.utsusynth.utsu.engine;

import java.io.File;
import com.google.inject.Inject;

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
        runner.runProcess(
                frqGeneratorPath.getAbsolutePath(),
                input.getAbsolutePath(),
                output.getAbsolutePath(),
                Integer.toString(samplesPerFrq));
    }
}
