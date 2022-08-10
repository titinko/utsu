package com.utsusynth.utsu.files.voicebank;

import com.utsusynth.utsu.common.data.WavData;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.utils.RoundUtils;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

/**
 * Writes .wav files.
 */
public class SoundFileWriter {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    public void writeWavData(WavData wavData, File wavFile) {
        writeWavData(wavData, wavFile, 0);
    }

    public void writeWavData(WavData wavData, File wavFile, int offsetMs) {
        int dataBytes = wavData.getSamples().length * 2; // Each sample is a short, or 2 bytes.
        ByteBuffer byteBuffer = ByteBuffer.allocate(dataBytes);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int maxAmplitude = 32768; // 2^(16 - 1) for 16-bit data.
        for (double sample : wavData.getSamples()) {
            int scaledSample = RoundUtils.round(sample * maxAmplitude);
            int boundedSample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, scaledSample));
            byteBuffer.putShort((short) boundedSample);
        }

        final int sampleRate = 44100;
        final boolean bigEndian = false;
        final boolean signed = true;
        final int bits = 16;
        final int channels = 1;
        try {
            if (offsetMs > 0) {
                AudioInputStream inputStream = AudioSystem.getAudioInputStream(wavFile);
                AudioFormat audioFormat = inputStream.getFormat();
                int offsetBytes = (int) (sampleRate / 1000.0 * offsetMs * 2) + 44;
                int outputBytes = offsetBytes + dataBytes;
                byte[] output = new byte[outputBytes];
                int bytesRead = inputStream.read(output, 0, offsetBytes);
                if (bytesRead < offsetBytes) {
                    System.out.println("Error: Could not read output wav file.");
                    return;
                }
                inputStream.close();
                byteBuffer.position(0);
                byteBuffer.get(output, offsetBytes, dataBytes);
                ByteArrayInputStream byteStream = new ByteArrayInputStream(output);
                AudioInputStream audioStream = new AudioInputStream(byteStream, audioFormat, outputBytes);
                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, wavFile);
                audioStream.close();
            } else {
                AudioFormat audioFormat =
                        new AudioFormat((float) sampleRate, bits, channels, signed, bigEndian);
                ByteArrayInputStream byteStream = new ByteArrayInputStream(byteBuffer.array());
                AudioInputStream audioStream =
                        new AudioInputStream(byteStream, audioFormat, wavData.getSamples().length);
                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, wavFile);
                audioStream.close();
            }
        } catch (IOException | UnsupportedAudioFileException e) {
            errorLogger.logError(e);
        }
    }
}
