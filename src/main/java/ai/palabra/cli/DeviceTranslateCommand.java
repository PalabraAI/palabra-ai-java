package ai.palabra.cli;

import ai.palabra.Config;
import ai.palabra.Language;
import ai.palabra.PalabraAI;
import ai.palabra.adapter.DeviceReader;
import ai.palabra.adapter.DeviceWriter;
import ai.palabra.cli.base.BaseCommand;
import ai.palabra.cli.util.CLIErrorHandler;
import ai.palabra.config.AdvancedConfig;
import ai.palabra.config.TargetLangConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.sound.sampled.*;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Device-based translation command.
 * Performs real-time translation using microphone and speakers.
 */
@Command(
    name = "device",
    description = "Real-time translation using microphone and speakers",
    mixinStandardHelpOptions = true,
    footer = {
        "",
        "Examples:",
        "  List available audio devices:",
        "    palabra device --list-devices",
        "",
        "  Basic real-time translation:",
        "    palabra device -t ES_MX -d 30",
        "",
        "  With specific source language:",
        "    palabra device -s EN_US -t FR_FR -d 60",
        "",
        "  Using advanced configuration file:",
        "    palabra device --config-file config.json -d 30",
        "",
        "  With custom audio settings:",
        "    palabra device -t JA_JP --sample-rate 16000 --input-format opus",
        "",
        "  With specific audio devices:",
        "    palabra device --input-device 0 --output-device 1 -t DE_DE",
        "",
        "Configuration File Format:",
        "  Use --config-file to load advanced settings from a JSON file.",
        "  Advanced options include transcription/translation model selection,",
        "  multiple target languages, and detailed audio processing settings.",
        "",
        "Audio Formats:",
        "  Input: pcm_s16le (default), opus, wav",
        "  Output: pcm_s16le (default), zlib_pcm_s16le"
    }
)
public class DeviceTranslateCommand extends BaseCommand implements Callable<Integer> {
    
    @Option(names = {"-s", "--source"}, description = "Source language (default: auto-detect)")
    private Language sourceLanguage;
    
    @Option(names = {"-t", "--target"}, description = "Target language")
    private Language targetLanguage;
    
    @Option(names = {"-d", "--duration"}, description = "Recording duration in seconds (default: 10)")
    private int durationSeconds = 10;
    
    @Option(names = {"--input-device"}, description = "Input device index (default: system default)")
    private Integer inputDevice;
    
    @Option(names = {"--output-device"}, description = "Output device index (default: system default)")
    private Integer outputDevice;
    
    @Option(names = {"--sample-rate"}, description = "Sample rate in Hz (default: 24000)")
    private int sampleRate = 24000;
    
    @Option(names = {"--buffer-size"}, description = "Buffer size in bytes (default: 4096)")
    private int bufferSize = 4096;
    
    @Option(names = {"--list-devices"}, description = "List available audio devices and exit")
    private boolean listDevices = false;
    
    @Option(names = {"--config-file"}, 
            description = "Load advanced configuration from JSON file (overrides other options)")
    private String configFile;
    
    @Option(names = {"--input-format"}, 
            description = "Microphone audio format (pcm_s16le, opus, wav, default: pcm_s16le)")
    private String inputFormat = "pcm_s16le";
    
    @Option(names = {"--output-format"}, 
            description = "Speaker audio format (pcm_s16le, zlib_pcm_s16le, default: pcm_s16le)")
    private String outputFormat = "pcm_s16le";
    
    @Option(names = {"--transcription-model"}, 
            description = "Speech recognition model to use (default: auto-select)")
    private String transcriptionModel = "auto";
    
    @Option(names = {"--translation-model"}, 
            description = "Translation model to use (default: auto-select)")
    private String translationModel = "auto";

    @Override
    protected String getCommandName() {
        return "device";
    }
    
    /**
     * Execute the device translation command.
     * @return exit code (0 for success, non-zero for failure)
     */
    @Override
    protected Integer executeCommand() throws Exception {
        if (listDevices) {
            listAudioDevices();
            return 0;
        }
        
        // Target language is required for translation (but not for listing devices)
        if (configFile == null && targetLanguage == null) {
            throw new IllegalArgumentException("Target language is required for translation (or use --config-file). Use -t or --target option.");
        }
        
        // Validate input parameters
        validateInput();
        
        // Get and validate credentials
        CredentialsPair credentials = getCredentials();
        
        errorHandler.reportInfo("Starting real-time translation...");
        
        if (configFile == null) {
            errorHandler.reportInfo("Source language: " + (sourceLanguage != null ? sourceLanguage.getDisplayName() : "auto-detect"));
            errorHandler.reportInfo("Target language: " + targetLanguage.getDisplayName());
        }
        
        errorHandler.reportInfo("Duration: " + durationSeconds + " seconds");
        errorHandler.reportInfo("Sample rate: " + sampleRate + " Hz");
        errorHandler.reportInfo("Buffer size: " + bufferSize + " bytes");
        errorHandler.reportInfo("Input format: " + inputFormat);
        errorHandler.reportInfo("Output format: " + outputFormat);
        
        if (inputDevice != null) {
            errorHandler.reportInfo("Input device: " + inputDevice);
        }
        if (outputDevice != null) {
            errorHandler.reportInfo("Output device: " + outputDevice);
        }
        
        // Perform device translation
        return translateDevice(credentials);
    }
    
    private Integer translateDevice(CredentialsPair credentials) throws Exception {
        
        // Initialize PalabraAI client
        PalabraAI client = new PalabraAI(credentials.clientId, credentials.clientSecret);
        
        // Initialize device adapters
        DeviceReader deviceReader = new DeviceReader(bufferSize, 1000); // chunk size, timeout
        DeviceWriter deviceWriter = new DeviceWriter(bufferSize);
        
        // Build configuration using advanced config
        AdvancedConfig advancedConfig;
        
        if (configFile != null) {
            // Load configuration from JSON file
            advancedConfig = loadAdvancedConfig(configFile);
            
            // Update readers/writers since they can't be serialized
            AdvancedConfig.Builder builder = AdvancedConfig.builder()
                .source(advancedConfig.getSource().getLanguage(), deviceReader)
                .inputStream(advancedConfig.getInputStream())
                .outputStream(advancedConfig.getOutputStream())
                .translationQueueConfigs(advancedConfig.getTranslationQueueConfigs())
                .allowedMessageTypes(advancedConfig.getAllowedMessageTypes())
                .silent(advancedConfig.isSilent())
                .debug(advancedConfig.isDebug())
                .timeout(advancedConfig.getTimeout());
            
            // Add targets with device writers
            for (int i = 0; i < advancedConfig.getTargets().size(); i++) {
                if (i == 0) {
                    // Use the main device writer for the first target
                    builder.addTarget(advancedConfig.getTargets().get(i).getLanguage(), deviceWriter);
                } else {
                    // For additional targets, create new device writers
                    DeviceWriter additionalWriter = new DeviceWriter(bufferSize);
                    builder.addTarget(advancedConfig.getTargets().get(i).getLanguage(), additionalWriter);
                }
            }
            
            advancedConfig = builder.build();
            
            errorHandler.reportProgress("Loaded configuration from: " + configFile);
        } else {
            // Create configuration from command line options with audio settings
            advancedConfig = createAdvancedConfigWithAudioSettings(
                sourceLanguage, targetLanguage, deviceReader, deviceWriter,
                sampleRate, inputFormat);
        }
        
        // Validate the configuration
        validateAdvancedConfig(advancedConfig);
        
        // Report configuration info
        reportConfigurationInfo(advancedConfig);
        
        // Build Config from AdvancedConfig for PalabraAI
        Config.AdvancedBuilder configBuilder = Config.builder()
            .advanced()
            .source(advancedConfig.getSource())
            .inputStream(advancedConfig.getInputStream())
            .outputStream(advancedConfig.getOutputStream())
            .translationQueueConfigs(advancedConfig.getTranslationQueueConfigs())
            .allowedMessageTypes(advancedConfig.getAllowedMessageTypes())
            .silent(advancedConfig.isSilent())
            .debug(advancedConfig.isDebug())
            .timeout(advancedConfig.getTimeout());
        
        // Add all targets
        for (TargetLangConfig target : advancedConfig.getTargets()) {
            configBuilder.addTarget(target);
        }
        
        Config config = configBuilder.build();
        
        try {
            errorHandler.reportProgress("Initializing audio devices...");
            
            // Initialize adapters
            deviceReader.initialize();
            deviceWriter.initialize();
            
            errorHandler.reportProgress("Audio devices initialized successfully!");
            errorHandler.reportInfo("Press Ctrl+C to stop recording...");
            
            // Start translation asynchronously
            CompletableFuture<Void> future = client.runAsync(config);
            
            // Wait for the specified duration or until completion
            try {
                future.get(durationSeconds, TimeUnit.SECONDS);
                errorHandler.reportInfo("Translation completed successfully!");
            } catch (Exception e) {
                errorHandler.reportInfo("Translation stopped: " + e.getMessage());
            }
            
            // Wait additional time for audio output to complete
            errorHandler.reportProgress("Waiting for audio output to complete...");
            try {
                Thread.sleep(10000); // Wait 10 seconds for audio chunks to arrive and play
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return 0;
            
        } catch (Exception e) {
            errorHandler.reportError("Device translation failed", e);
            return 1;
        } finally {
            // Clean up resources
            try {
                deviceReader.close();
                deviceWriter.close();
                errorHandler.reportProgress("Audio devices closed");
            } catch (Exception e) {
                errorHandler.reportWarning("Warning: Error during cleanup: " + e.getMessage());
            }
        }
    }
    
    private void validateInput() throws Exception {
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("Duration must be positive: " + durationSeconds);
        }
        
        if (sampleRate <= 0 || sampleRate < 16000 || sampleRate > 24000) {
            throw new IllegalArgumentException("Sample rate must be between 16000 and 24000 Hz, got: " + sampleRate);
        }
        
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("Buffer size must be positive: " + bufferSize);
        }
        
        // Validate audio formats
        if (!"pcm_s16le".equals(inputFormat) && !"opus".equals(inputFormat) && !"wav".equals(inputFormat)) {
            throw new IllegalArgumentException("Unsupported input format: " + inputFormat + 
                ". Supported formats: pcm_s16le, opus, wav");
        }
        
        if (!"pcm_s16le".equals(outputFormat) && !"zlib_pcm_s16le".equals(outputFormat)) {
            throw new IllegalArgumentException("Unsupported output format: " + outputFormat + 
                ". Supported formats: pcm_s16le, zlib_pcm_s16le");
        }
        
        // Validate config file if provided
        if (configFile != null) {
            File file = new File(configFile);
            validateFile(file, "Configuration");
            validateConfigFormat(file);
        }
        
        errorHandler.reportProgress("Input validation passed");
    }
    
    private void listAudioDevices() {
        System.out.println("Available Audio Devices:");
        System.out.println("========================");
        
        // Get all available mixers
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        
        int inputIndex = 0;
        int outputIndex = 0;
        
        for (int i = 0; i < mixerInfos.length; i++) {
            Mixer.Info mixerInfo = mixerInfos[i];
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            
            // Check for input capabilities (microphone)
            Line.Info[] targetLineInfos = mixer.getTargetLineInfo();
            boolean hasInput = false;
            for (Line.Info lineInfo : targetLineInfos) {
                if (lineInfo instanceof DataLine.Info) {
                    DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
                    if (dataLineInfo.getLineClass().equals(TargetDataLine.class)) {
                        hasInput = true;
                        break;
                    }
                }
            }
            
            // Check for output capabilities (speakers)
            Line.Info[] sourceLineInfos = mixer.getSourceLineInfo();
            boolean hasOutput = false;
            for (Line.Info lineInfo : sourceLineInfos) {
                if (lineInfo instanceof DataLine.Info) {
                    DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
                    if (dataLineInfo.getLineClass().equals(SourceDataLine.class)) {
                        hasOutput = true;
                        break;
                    }
                }
            }
            
            // Display device information
            if (hasInput) {
                System.out.printf("Input Device %d: %s%n", inputIndex++, mixerInfo.getName());
                System.out.printf("  Description: %s%n", mixerInfo.getDescription());
                System.out.printf("  Vendor: %s%n", mixerInfo.getVendor());
                System.out.println();
            }
            
            if (hasOutput) {
                System.out.printf("Output Device %d: %s%n", outputIndex++, mixerInfo.getName());
                System.out.printf("  Description: %s%n", mixerInfo.getDescription());
                System.out.printf("  Vendor: %s%n", mixerInfo.getVendor());
                System.out.println();
            }
        }
        
        if (inputIndex == 0 && outputIndex == 0) {
            System.out.println("No audio devices found.");
        } else {
            System.out.printf("Found %d input device(s) and %d output device(s)%n", inputIndex, outputIndex);
            System.out.println();
            System.out.println("To use a specific device, run:");
            System.out.println("  palabra-cli device --input-device <index> --output-device <index> -t <language>");
        }
    }
}
