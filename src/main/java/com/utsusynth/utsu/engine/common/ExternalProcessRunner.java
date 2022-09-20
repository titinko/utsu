package com.utsusynth.utsu.engine.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import com.utsusynth.utsu.common.exception.ErrorLogger;

/**
 * Class that runs an external command-line process with the provided arguments.
 */
public class ExternalProcessRunner {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    private Process curProcess;

    public ExternalProcessRunner() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Destroy any ongoing processes when Utsu closes.
            if (curProcess != null && curProcess.isAlive()) {
                curProcess.destroy();
            }
        }));
    }

    public void runProcess(String... args) {
        runProcess(null, args);
    }

    public void runProcess(File workingDir, String... args) {
        // Uncomment these lines to see calls to resampler/wavtool.
        //StringBuilder argString = new StringBuilder();
        //for (String arg : args) {
        //    argString.append(arg.substring(Math.max(arg.lastIndexOf('\\') + 1, 0))).append(' ');
        //}
        //System.out.println(argString);
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectErrorStream(true);
        if (workingDir != null) {
            builder.directory(workingDir);
        }
        try {
            curProcess = builder.start();
            watch(curProcess.getInputStream());
            curProcess.waitFor();
        } catch (IOException | InterruptedException e) {
            errorLogger.logError(e);
        }
    }

    private void watch(final InputStream inputStream) {
        new Thread(() -> {
            BufferedReader input = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            try {
                while ((line = input.readLine()) != null) {
                    // Uncomment this line to see output from resampler/wavtool.
                    // System.out.println(line);
                }
            } catch (IOException e) {
                errorLogger.logError(e);
            }
        }).start();
    }
}
