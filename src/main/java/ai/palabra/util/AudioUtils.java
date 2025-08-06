package ai.palabra.util;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Audio utility class for handling audio format conversion and PCM processing.
 * Currently supports WAV format processing with built-in Java Sound API.
 * Note: MP3/OGG support will be added in future iterations.
 */
public class AudioUtils {
    
    private static final AudioFormat PCM_FORMAT = new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        48000.0f,  // Sample rate - 48kHz (matching Python reference)
        16,        // Bits per sample
        1,         // Channels (mono)
        2,         // Frame size (2 bytes per sample for 16-bit)
        48000.0f,  // Frame rate
        false      // Little endian
    );
    
    /**
     * Convert any supported audio format to PCM 16-bit mono at 48kHz.
     * Currently supports formats supported by Java Sound API (WAV, AU, AIFF).
     * 
     * @param audioData Raw audio file data
     * @return PCM 16-bit mono audio data
     * @throws IOException If conversion fails
     */
    public static byte[] convertToPcm16(byte[] audioData) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(audioData)) {
            
            // Try to read as audio file
            AudioInputStream originalStream;
            try {
                originalStream = AudioSystem.getAudioInputStream(inputStream);
            } catch (UnsupportedAudioFileException e) {
                // If not a recognized audio file, assume it's raw PCM data
                return convertRawPcmTo16Bit(audioData);
            }
            
            AudioFormat originalFormat = originalStream.getFormat();
            
            // If already in correct format, return as-is
            if (isPcm16Format(originalFormat)) {
                return readAllBytes(originalStream);
            }
            
            // Convert to PCM 16-bit mono
            AudioInputStream pcmStream = AudioSystem.getAudioInputStream(PCM_FORMAT, originalStream);
            return readAllBytes(pcmStream);
            
        } catch (Exception e) {
            throw new IOException("Failed to convert audio to PCM format", e);
        }
    }
    
    /**
     * Read audio file from disk and convert to PCM 16-bit mono.
     * 
     * @param filePath Path to audio file
     * @return PCM 16-bit mono audio data
     * @throws IOException If file reading or conversion fails
     */
    public static byte[] readAudioFile(Path filePath) throws IOException {
        // Check if file exists
        if (!Files.exists(filePath)) {
            throw new IOException("Audio file not found: " + filePath);
        }
        
        try {
            // Try reading as audio file first
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(filePath.toFile());
            AudioFormat originalFormat = audioInputStream.getFormat();
            
            // Convert to our target format if needed
            if (isPcm16Format(originalFormat)) {
                return readAllBytes(audioInputStream);
            } else {
                AudioInputStream pcmStream = AudioSystem.getAudioInputStream(PCM_FORMAT, audioInputStream);
                return readAllBytes(pcmStream);
            }
            
        } catch (UnsupportedAudioFileException e) {
            // If not a recognized audio file, try reading as raw binary data
            byte[] fileData = Files.readAllBytes(filePath);
            return convertToPcm16(fileData);
        }
    }
    
    /**
     * Write PCM audio data to WAV file.
     * 
     * @param audioData PCM 16-bit mono audio data
     * @param outputPath Output file path
     * @throws IOException If writing fails
     */
    public static void writePcmToWav(byte[] audioData, Path outputPath) throws IOException {
        if (audioData == null || audioData.length == 0) {
            // Create empty WAV file
            audioData = new byte[0];
        }
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
             AudioInputStream audioInputStream = new AudioInputStream(bais, PCM_FORMAT, audioData.length / PCM_FORMAT.getFrameSize())) {
            
            // Ensure parent directory exists
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }
            
            // Write WAV file
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputPath.toFile());
        }
    }
    
    /**
     * Convert raw PCM data to 16-bit format if needed.
     * This handles cases where data might be in different bit depths.
     * 
     * @param rawData Raw audio data
     * @return 16-bit PCM data
     */
    private static byte[] convertRawPcmTo16Bit(byte[] rawData) {
        // For now, assume input is already 16-bit PCM
        // Future enhancement: detect and convert from other bit depths
        return rawData;
    }
    
    /**
     * Resample audio data to specified sample rate.
     * Simple linear interpolation implementation.
     * 
     * @param audioData Input audio data
     * @param inputSampleRate Input sample rate
     * @param outputSampleRate Desired output sample rate
     * @return Resampled audio data
     */
    public static byte[] resampleAudio(byte[] audioData, float inputSampleRate, float outputSampleRate) {
        if (Math.abs(inputSampleRate - outputSampleRate) < 1.0f) {
            return audioData;
        }
        
        // Simple linear interpolation resampling
        double ratio = outputSampleRate / inputSampleRate;
        int inputSamples = audioData.length / 2; // 16-bit = 2 bytes per sample
        int outputSamples = (int) (inputSamples * ratio);
        
        ByteBuffer inputBuffer = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer outputBuffer = ByteBuffer.allocate(outputSamples * 2).order(ByteOrder.LITTLE_ENDIAN);
        
        for (int i = 0; i < outputSamples; i++) {
            double sourceIndex = i / ratio;
            int index = (int) sourceIndex;
            
            if (index >= inputSamples - 1) {
                // Use last sample
                outputBuffer.putShort(inputBuffer.getShort((inputSamples - 1) * 2));
            } else {
                // Linear interpolation
                short sample1 = inputBuffer.getShort(index * 2);
                short sample2 = inputBuffer.getShort((index + 1) * 2);
                double fraction = sourceIndex - index;
                short interpolated = (short) (sample1 + fraction * (sample2 - sample1));
                outputBuffer.putShort(interpolated);
            }
        }
        
        return outputBuffer.array();
    }
    
    /**
     * Convert stereo audio to mono by averaging channels.
     * 
     * @param stereoData Stereo audio data (interleaved)
     * @return Mono audio data
     */
    public static byte[] stereoToMono(byte[] stereoData) {
        ByteBuffer inputBuffer = ByteBuffer.wrap(stereoData).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer outputBuffer = ByteBuffer.allocate(stereoData.length / 2).order(ByteOrder.LITTLE_ENDIAN);
        
        for (int i = 0; i < stereoData.length / 4; i++) {
            short left = inputBuffer.getShort();
            short right = inputBuffer.getShort();
            short mono = (short) ((left + right) / 2);
            outputBuffer.putShort(mono);
        }
        
        return outputBuffer.array();
    }
    
    /**
     * Check if audio format is already PCM 16-bit mono at 48kHz.
     */
    private static boolean isPcm16Format(AudioFormat format) {
        return format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) &&
               format.getSampleSizeInBits() == 16 &&
               format.getChannels() == 1 &&
               Math.abs(format.getSampleRate() - 48000.0f) < 100.0f; // Allow small variance
    }
    
    /**
     * Read all bytes from an AudioInputStream.
     */
    private static byte[] readAllBytes(AudioInputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int bytesRead;
        
        while ((bytesRead = stream.read(data)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        
        return buffer.toByteArray();
    }
    
    /**
     * Get the standard PCM format used by Palabra AI.
     * 
     * @return The standard PCM audio format configuration
     */
    public static AudioFormat getPcmFormat() {
        return PCM_FORMAT;
    }
    
    /**
     * Get information about supported audio file formats.
     * 
     * @return Array of supported audio file format extensions
     */
    public static String[] getSupportedFormats() {
        AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();
        String[] formats = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            formats[i] = types[i].getExtension();
        }
        return formats;
    }
    
    /**
     * Resample PCM audio from 48kHz to 24kHz using simple decimation.
     * This is a basic implementation that takes every other sample.
     * 
     * @param pcm48k PCM audio data at 48kHz, 16-bit mono, little endian
     * @return PCM audio data at 24kHz, 16-bit mono, little endian
     */
    public static byte[] resample48kTo24k(byte[] pcm48k) {
        // For 16-bit audio, each sample is 2 bytes
        // 48kHz to 24kHz is a 2:1 ratio, so we take every other sample
        int inputSamples = pcm48k.length / 2;
        int outputSamples = inputSamples / 2;
        byte[] output = new byte[outputSamples * 2];
        
        for (int i = 0; i < outputSamples; i++) {
            // Take every other sample (simple decimation)
            int inputIndex = i * 2 * 2; // Skip every other sample
            int outputIndex = i * 2;
            
            if (inputIndex + 1 < pcm48k.length) {
                output[outputIndex] = pcm48k[inputIndex];         // Low byte
                output[outputIndex + 1] = pcm48k[inputIndex + 1]; // High byte
            }
        }
        
        return output;
    }
    
    /**
     * Resample PCM audio from 24kHz to 48kHz using simple interpolation.
     * This duplicates each sample to double the sample rate.
     * 
     * @param pcm24k PCM audio data at 24kHz, 16-bit mono, little endian
     * @return PCM audio data at 48kHz, 16-bit mono, little endian
     */
    public static byte[] resample24kTo48k(byte[] pcm24k) {
        // For 16-bit audio, each sample is 2 bytes
        // 24kHz to 48kHz is a 1:2 ratio, so we duplicate each sample
        int inputSamples = pcm24k.length / 2;
        int outputSamples = inputSamples * 2;
        byte[] output = new byte[outputSamples * 2];
        
        for (int i = 0; i < inputSamples; i++) {
            int inputIndex = i * 2;
            int outputIndex1 = i * 4;     // First copy
            int outputIndex2 = i * 4 + 2; // Second copy (duplicate)
            
            // Copy sample twice to double the sample rate
            output[outputIndex1] = pcm24k[inputIndex];         // Low byte, first copy
            output[outputIndex1 + 1] = pcm24k[inputIndex + 1]; // High byte, first copy
            output[outputIndex2] = pcm24k[inputIndex];         // Low byte, second copy
            output[outputIndex2 + 1] = pcm24k[inputIndex + 1]; // High byte, second copy
        }
        
        return output;
    }
}
