package com.utsusynth.utsu.engine;

import java.io.File;
import com.google.inject.Inject;
import com.utsusynth.utsu.engine.common.ExternalProcessRunner;
import com.utsusynth.utsu.files.AssetManager;
import com.utsusynth.utsu.files.FileNameFixer;

public class FrqGenerator {
    private final ExternalProcessRunner runner;
    private final FileNameFixer fileNameFixer;
    private final File frqGeneratorPath;
    private final int samplesPerFrq; // Samples per value in frq file. Currently always 256.

    @Inject
    public FrqGenerator(
            ExternalProcessRunner runner, FileNameFixer fileNameFixer, AssetManager assetManager, int samplesPerFrq) {
        this.runner = runner;
        this.fileNameFixer = fileNameFixer;
        this.frqGeneratorPath = assetManager.getFrqGeneratorFile();
        this.samplesPerFrq = samplesPerFrq;
    }

    public void genFrqFile(File input, File output) {
        runner.runProcess(
                frqGeneratorPath.getAbsolutePath(),
                fileNameFixer.getFixedName(input.getAbsolutePath()),
                output.getAbsolutePath(),
                Integer.toString(samplesPerFrq));
    }
}
