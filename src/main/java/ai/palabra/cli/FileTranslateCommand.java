package ai.palabra.cli;

import ai.palabra.Config;
import ai.palabra.Language;
import ai.palabra.PalabraAI;
import ai.palabra.adapter.FileReader;
import ai.palabra.adapter.FileWriter;
import ai.palabra.cli.base.BaseCommand;
import ai.palabra.config.AdvancedConfig;
import ai.palabra.config.TargetLangConfig;
import ai.palabra.util.AudioUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * File-based translation command.
 * Performs translation from input audio file to output audio file.
 */
@Command(
    name = "file",
    description = "Translate audio file from source language to target language",
    mixinStandardHelpOptions = true,
    footer = {
        "",
        "Examples:",
        "  Basic translation:",
        "    palabra file -i input.wav -t ES_MX -o output.wav",
        "",
        "  With source language detection:",
        "    palabra file -i recording.wav -s EN_US -t ES_MX -o spanish.wav",
        "",
        "  Using advanced configuration file:",
        "    palabra file -i audio.wav --config-file config.json -o translated.wav",
        "",
        "  With custom sample rate:",
        "    palabra file -i input.wav -t FR_FR --sample-rate 48000 -o output.wav",
        "",
        "Configuration File Format:",
        "  Use --config-file to load advanced settings from a JSON file.",
        "  See examples/basic-config.json for a simple configuration template.",
        "  See examples/multi-target-config.json for multi-language translation.",
        "",
        "Supported Audio Formats:",
        "  WAV, AU, AIFF (MP3 and OGG are not currently supported)"
    }
)
public class FileTranslateCommand extends BaseCommand implements Callable<Integer> {
    
    @Option(names = {"-i", "--input"}, description = "Input audio file path", required = true)
    private String inputFilePath;
    
    @Option(names = {"-o", "--output"}, description = "Output audio file path", required = true)
    private String outputFilePath;
    
    @Option(names = {"-s", "--source"}, description = "Source language (default: auto-detect)")
    private Language sourceLanguage;
    
    @Option(names = {"-t", "--target"}, description = "Target language", required = true)
    private Language targetLanguage;
    
    @Option(names = {"--config-file"}, 
            description = "Load advanced configuration from JSON file (overrides other options)")
    private String configFile;
    
    @Option(names = {"--sample-rate"}, 
            description = "Audio sample rate in Hz (valid: 16000-48000, default: 24000)")
    private Integer sampleRate = 24000;

    @Override
    protected String getCommandName() {
        return "file";
    }

    @Override
    protected Integer executeCommand() throws Exception {
        // Validate input parameters
        validateInputParameters();
        
        // Get and validate credentials
        CredentialsPair credentials = getCredentials();
        
        // Validate input file
        File inputFile = new File(inputFilePath);
        validateFile(inputFile, "Input audio");
        validateAudioFormat(inputFile);
        
        // Validate output file path
        File outputFile = new File(outputFilePath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null) {
            ensureOutputDirectory(parentDir);
        }
        
        errorHandler.reportProgress("Starting file translation...");
        errorHandler.reportInfo("Input file: " + inputFile.getAbsolutePath());
        errorHandler.reportInfo("Output file: " + outputFile.getAbsolutePath());
        errorHandler.reportInfo("Source language: " + (sourceLanguage != null ? sourceLanguage.getDisplayName() : "auto-detect"));
        errorHandler.reportInfo("Target language: " + targetLanguage.getDisplayName());
        
        // Perform file translation
        return translateFile(credentials, inputFile, outputFile);
    }
    
    private void validateInputParameters() throws Exception {
        if (inputFilePath == null || inputFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Input file path is required");
        }
        
        if (outputFilePath == null || outputFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Output file path is required");
        }
        
        // If config file is provided, target language can be loaded from config
        if (configFile == null && targetLanguage == null) {
            throw new IllegalArgumentException("Target language is required (or use --config-file)");
        }
        
        // Validate that input and output are different files
        if (inputFilePath.equals(outputFilePath)) {
            throw new IllegalArgumentException("Input and output files must be different");
        }
        
        // Validate sample rate if provided
        if (sampleRate != null && (sampleRate < 16000 || sampleRate > 48000)) {
            throw new IllegalArgumentException("Sample rate must be between 16000 and 48000 Hz");
        }
        
        // Validate config file if provided
        if (configFile != null) {
            File file = new File(configFile);
            validateFile(file, "Configuration");
            validateConfigFormat(file);
        }
    }
    
    @Override
    protected void validateAudioFormat(File file) throws Exception {
        String fileName = file.getName().toLowerCase();
        
        // Get supported formats from AudioUtils
        String[] supportedFormats = AudioUtils.getSupportedFormats();
        boolean isSupported = false;
        
        for (String format : supportedFormats) {
            if (fileName.endsWith("." + format.toLowerCase())) {
                isSupported = true;
                break;
            }
        }
        
        // Also check common formats that Java Sound API supports
        if (!isSupported) {
            isSupported = fileName.endsWith(".wav") || fileName.endsWith(".au") || fileName.endsWith(".aiff");
        }
        
        if (!isSupported) {
            StringBuilder supportedList = new StringBuilder();
            for (int i = 0; i < supportedFormats.length; i++) {
                if (i > 0) supportedList.append(", ");
                supportedList.append(supportedFormats[i].toLowerCase());
            }
            
            throw new IllegalArgumentException(
                String.format("Unsupported audio format: %s%n" +
                "Supported formats: %s, wav, au, aiff%n" +
                "Note: MP3 and OGG formats are not currently supported.", 
                getFileExtension(fileName), supportedList.toString())
            );
        }
        
        errorHandler.reportProgress("Audio format validation passed: " + getFileExtension(fileName));
    }
    
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "unknown";
    }
    
    private Integer translateFile(CredentialsPair credentials, File inputFile, File outputFile) throws Exception {
        // Initialize PalabraAI client
        PalabraAI client = new PalabraAI(credentials.clientId, credentials.clientSecret);
        
        // Initialize file adapters
        FileReader fileReader = new FileReader(inputFile.getAbsolutePath());
        FileWriter fileWriter = new FileWriter(outputFile.getAbsolutePath());
        
        try {
            // Build configuration using advanced config
            AdvancedConfig advancedConfig;
            
            if (configFile != null) {
                // Load configuration from JSON file
                advancedConfig = loadAdvancedConfig(configFile);
                
                // Update readers/writers since they can't be serialized
                AdvancedConfig.Builder builder = AdvancedConfig.builder()
                    .source(advancedConfig.getSource().getLanguage(), fileReader)
                    .inputStream(advancedConfig.getInputStream())
                    .outputStream(advancedConfig.getOutputStream())
                    .translationQueueConfigs(advancedConfig.getTranslationQueueConfigs())
                    .allowedMessageTypes(advancedConfig.getAllowedMessageTypes())
                    .silent(advancedConfig.isSilent())
                    .debug(advancedConfig.isDebug())
                    .timeout(advancedConfig.getTimeout());
                
                // Add targets with updated writers
                for (int i = 0; i < advancedConfig.getTargets().size(); i++) {
                    if (i == 0) {
                        // Use the provided output file for the first target
                        builder.addTarget(advancedConfig.getTargets().get(i).getLanguage(), fileWriter);
                    } else {
                        // For additional targets, create new writers with modified filenames
                        String baseName = outputFile.getName();
                        String extension = "";
                        int dotIndex = baseName.lastIndexOf('.');
                        if (dotIndex > 0) {
                            extension = baseName.substring(dotIndex);
                            baseName = baseName.substring(0, dotIndex);
                        }
                        String newFileName = baseName + "_" + advancedConfig.getTargets().get(i).getLanguage().getCode() + extension;
                        File newOutputFile = new File(outputFile.getParent(), newFileName);
                        builder.addTarget(advancedConfig.getTargets().get(i).getLanguage(), new FileWriter(newOutputFile.getAbsolutePath()));
                    }
                }
                
                advancedConfig = builder.build();
                
                errorHandler.reportProgress("Loaded configuration from: " + configFile);
            } else {
                // Create configuration from command line options
                if (sampleRate != null && !sampleRate.equals(24000)) {
                    // Use custom audio settings
                    advancedConfig = createAdvancedConfigWithAudioSettings(
                        sourceLanguage, targetLanguage, fileReader, fileWriter,
                        sampleRate, "pcm_s16le");
                } else {
                    // Use simple configuration
                    advancedConfig = createAdvancedConfigFromSimpleOptions(
                        sourceLanguage, targetLanguage, fileReader, fileWriter);
                }
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
            
            errorHandler.reportProgress("Initializing file adapters...");
            
            // File adapters don't need explicit initialization - they auto-initialize on first use
            errorHandler.reportProgress("File adapters ready!");
            
            // Report file information
            reportFileInfo(inputFile, outputFile);
            
            errorHandler.reportInfo("Starting translation process...");
            errorHandler.reportInfo("Note: Transcriptions and progress will be shown below:");
            
            // Run translation synchronously
            client.run(config);
            
            errorHandler.reportInfo("Translation completed successfully!");
            
            // Report final file information
            reportFileInfo(inputFile, outputFile);
            
            return 0;
            
        } catch (Exception e) {
            errorHandler.reportError("File translation failed", e);
            return 1;
        } finally {
            // Clean up resources
            try {
                fileReader.close();
                errorHandler.reportProgress("Input file reader closed");
            } catch (Exception e) {
                errorHandler.reportWarning("Warning: Error closing input file reader: " + e.getMessage());
            }
            
            try {
                fileWriter.close();
                errorHandler.reportProgress("Output file writer closed");
            } catch (Exception e) {
                errorHandler.reportWarning("Warning: Error closing output file writer: " + e.getMessage());
            }
        }
    }
    
    @Override
    protected void reportFileInfo(File inputFile, File outputFile) {
        try {
            if (inputFile.exists()) {
                long inputSize = java.nio.file.Files.size(inputFile.toPath());
                double inputSizeMB = inputSize / (1024.0 * 1024.0);
                errorHandler.reportInfo(String.format("Input file size: %,d bytes (%.2f MB)", inputSize, inputSizeMB));
            }
            
            if (outputFile.exists()) {
                long outputSize = java.nio.file.Files.size(outputFile.toPath());
                double outputSizeMB = outputSize / (1024.0 * 1024.0);
                errorHandler.reportInfo(String.format("Output file size: %,d bytes (%.2f MB)", outputSize, outputSizeMB));
            }
        } catch (Exception e) {
            errorHandler.reportWarning("Could not read file size information: " + e.getMessage());
        }
    }
}