package com.utsusynth.utsu.files.voicebank;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Optional;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.google.common.base.Function;
import org.apache.commons.io.FileUtils;
import com.utsusynth.utsu.common.data.FrequencyData;
import com.utsusynth.utsu.common.data.WavData;
import com.utsusynth.utsu.common.exception.ErrorLogger;

/**
 * Reads .frq and .wav files.
 */
public class SoundFileReader {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    public Optional<FrequencyData> loadFrqData(File frqFile, Function<String, Void> updateStatus) {
        if (!frqFile.canRead()) {
            updateStatus.apply("Warning: frq file not found: " + frqFile.getAbsolutePath());
            return Optional.empty();
        }
        try {
            // Parse header values.
            ByteBuffer buffer = ByteBuffer.wrap(FileUtils.readFileToByteArray(frqFile));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            byte[] charBuf = new byte[8];
            buffer.get(charBuf);
            if (!"FREQ0003".equals(new String(charBuf))) {
                updateStatus.apply("Error: Tried to load frq data on a non-frq file.");
                return Optional.empty();
            }
            int samplesPerFrq = buffer.getInt(); // Number of samples per frequency value.
            double average = buffer.getDouble(); // Average F0 (pitch) of the sound.
            buffer.get(new byte[16]); // 16 bytes of empty space.

            // Parse frequency/amplitude values.
            int numBlocks = buffer.getInt();
            double[] frqs = new double[numBlocks];
            double[] amplitudes = new double[numBlocks];
            for (int i = 0; i < numBlocks; i++) {
                frqs[i] = buffer.getDouble();
                amplitudes[i] = buffer.getDouble();
            }
            if (buffer.hasRemaining()) {
                updateStatus.apply("Warning: Parts of frq file were left unread.");
            }
            return Optional.of(new FrequencyData(average, samplesPerFrq, frqs, amplitudes));
        } catch (IOException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
            return Optional.empty();
        }
    }

    public Optional<WavData> loadWavData(File wavFile, Function<String, Void> updateStatus) {
        return loadWavData(wavFile, /* offsetMs= */ 0, updateStatus);
    }

    public Optional<WavData> loadWavData(
            File wavFile, int offsetMs, Function<String, Void> updateStatus) {
        if (!wavFile.canRead()) {
            updateStatus.apply("Error: wav file not found!");
            return Optional.empty();
        }
        try (AudioInputStream input = AudioSystem.getAudioInputStream(wavFile)) {
            int bitsPerSample = input.getFormat().getSampleSizeInBits();
            if (bitsPerSample != 8 && bitsPerSample != 16) {
                updateStatus.apply("Error: Does not support sample sizes other than 8 or 16 bit.");
                return Optional.empty();
            }

            Encoding encoding = input.getFormat().getEncoding();
            if (encoding != Encoding.PCM_SIGNED && encoding != Encoding.PCM_UNSIGNED) {
                updateStatus.apply("Error: Does not support encodings other than PCM.");
                return Optional.empty();
            }

            // Calculate the number of bytes to offset.
            offsetMs = Math.max(offsetMs, 0); // Ignore negative offsets.
            int offsetFrames = (int) (input.getFormat().getFrameRate() / 1000.0 * offsetMs);
            int offsetBytes = offsetFrames * bitsPerSample / 8;
            int numFrames = (int) input.getFrameLength();
            int numBytes = numFrames * input.getFormat().getFrameSize();
            double lengthMs = numFrames / input.getFormat().getFrameRate() * 1000;

            // Create a buffer to read 16-bit samples.
            byte[] bytes = new byte[numBytes - offsetBytes];
            long bytesSkipped = input.skip(offsetBytes);
            if (bytesSkipped < offsetBytes) {
                updateStatus.apply("Error: Could not skip to correct location in wav file.");
                return Optional.empty();
            }
            int bytesRead = input.read(bytes);
            if (bytesRead < bytes.length) {
                updateStatus.apply("Error: Could not read entire wav file.");
                return Optional.empty();
            }
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            byteBuffer.order(
                    input.getFormat().isBigEndian() ? ByteOrder.BIG_ENDIAN
                            : ByteOrder.LITTLE_ENDIAN);
            ShortBuffer shortBuffer = byteBuffer.asShortBuffer(); // Use if 16 bits per sample.

            double[] samples = new double[numFrames - offsetFrames];
            double maxAmplitude = Math.pow(2.0, bitsPerSample - 1);
            for (int i = 0; i < numFrames - offsetFrames; i++) {
                for (int channel = 0; channel < input.getFormat().getChannels(); channel++) {
                    short sample = bitsPerSample == 16 ? shortBuffer.get() : byteBuffer.get();
                    if (channel == 0) {
                        if (encoding.equals(Encoding.PCM_UNSIGNED)) {
                            sample -= (int) maxAmplitude;
                        }
                        samples[i] = sample / maxAmplitude;
                    }
                }
            }
            if ((bitsPerSample == 16 && shortBuffer.hasRemaining())
                    || (bitsPerSample == 8 && byteBuffer.hasRemaining())) {
                updateStatus.apply("Warning: Parts of wav file were left unread.");
            }
            input.close();
            return Optional.of(new WavData(lengthMs, samples));
        } catch (IOException | UnsupportedAudioFileException e) {
            // TODO: Handle this.
            errorLogger.logError(e);
            return Optional.empty();
        }
    }
}
