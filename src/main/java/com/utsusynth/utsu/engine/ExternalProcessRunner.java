package com.utsusynth.utsu.engine;

import java.io.BufferedReader;
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

    void runProcess(String... args) {
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectErrorStream(true);
        try {
            curProcess = builder.start();
            watch(curProcess.getInputStream());
            curProcess.waitFor();
        } catch (IOException | InterruptedException e) {
            errorLogger.logError(e);
        }
    }

    private void watch(final InputStream inputStream) {
        new Thread() {
            public void run() {
                BufferedReader input = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;
                try {
                    while ((line = input.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    errorLogger.logError(e);
                }
            }
        }.start();
    }
}
