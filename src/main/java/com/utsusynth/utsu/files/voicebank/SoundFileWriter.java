package com.utsusynth.utsu.files.voicebank;

import com.utsusynth.utsu.common.data.WavData;
import com.utsusynth.utsu.common.exception.ErrorLogger;
import com.utsusynth.utsu.common.utils.RoundUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Writes .wav files.
 */
public class SoundFileWriter {
    private static final ErrorLogger errorLogger = ErrorLogger.getLogger();

    public void writeWavData(WavData wavData, File wavFile) {
        int dataSize = wavData.getSamples().length * 2; // Each sample is a short, or 2 bytes.
        int fileSize = dataSize + 44;

        ByteBuffer byteBuffer = ByteBuffer.allocate(fileSize);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putChar('R').putChar('I').putChar('F').putChar('F');
        // TODO: chunksize
        byteBuffer.putInt(fileSize - 8); // Number of bytes in file minus the top two fields.
        byteBuffer.putChar('W').putChar('A').putChar('V').putChar('E');

        // Format.
        byteBuffer.putChar('f').putChar('m').putChar('t').putChar(' ');
        byteBuffer.putInt(16); // Format chunk size.
        byteBuffer.putShort((short) 1); // Format tag.
        byteBuffer.putShort((short) 1); // Num channels.
        byteBuffer.putInt(44100); // Sample rate.
        byteBuffer.putInt(88200); // Bytes per second.
        byteBuffer.putShort((short) 2); // Bytes per sample.
        byteBuffer.putShort((short) 16); // Bits per sample.

        // Data.
        byteBuffer.putChar('d').putChar('a').putChar('t').putChar('a');
        int maxAmplitude = 32768; // 2^(16 - 1) for 16-bit data.
        for (double sample : wavData.getSamples()) {
            int scaledSample = RoundUtils.round(sample * maxAmplitude);
            int boundedSample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, scaledSample));
            byteBuffer.putShort((short) boundedSample);
        }
        try {
            FileUtils.writeByteArrayToFile(wavFile, byteBuffer.array(), /* append= */ false);
        } catch (IOException e) {
            errorLogger.logError(e);
        }
    }
}
