package com.utsusynth.utsu.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Class that runs an external command-line process with the provided arguments.
 */
public class ExternalProcessRunner {
	void runProcess(String... args) {
		ProcessBuilder builder = new ProcessBuilder(args);
		builder.redirectErrorStream(true);
		try {
			Process process = builder.start();
			watch(process);
			process.waitFor();
		} catch (IOException | InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	private void watch(final Process process) {
		new Thread() {
			public void run() {
				BufferedReader input = new BufferedReader(
						new InputStreamReader(process.getInputStream()));
				String line = null;
				try {
					while ((line = input.readLine()) != null) {
						System.out.println(line);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}
