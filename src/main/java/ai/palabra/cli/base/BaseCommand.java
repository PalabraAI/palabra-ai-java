package ai.palabra.cli.base;

import ai.palabra.cli.ConfigCommand;
import ai.palabra.cli.util.CLIErrorHandler;
import ai.palabra.config.*;
import ai.palabra.*;
import ai.palabra.adapter.*;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Base class for all CLI commands providing consistent error handling,
 * verbose/quiet mode support, and common utilities.
 */
public abstract class BaseCommand implements Callable<Integer> {
    
    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    protected boolean verbose = false;
    
    @CommandLine.Option(names = {"-q", "--quiet"}, description = "Quiet mode (minimal output)")
    protected boolean quiet = false;
    
    /** Error handler for consistent error reporting. */
    protected CLIErrorHandler errorHandler;
    
    /**
     * Initialize the command with error handler.
     */
    protected void initializeCommand() {
        this.errorHandler = new CLIErrorHandler(verbose, quiet);
    }
    
    @Override
    public final Integer call() {
        try {
            initializeCommand();
            return executeCommand();
        } catch (Exception e) {
            if (errorHandler == null) {
                errorHandler = new CLIErrorHandler(verbose, quiet);
            }
            
            errorHandler.reportError(getCommandName(), e);
            errorHandler.suggestSolutions(e);
            
            return errorHandler.getExitCode(e);
        }
    }
    
    /**
     * Execute the specific command logic.
     * Subclasses should implement this method instead of call().
     * @return exit code (0 for success, non-zero for failure)
     * @throws Exception if command execution fails
     */
    protected abstract Integer executeCommand() throws Exception;
    
    /**
     * Get the command name for error reporting.
     * @return the command name used in error messages
     */
    protected abstract String getCommandName();
    
    /**
     * Validate and get client credentials.
     * @return credentials pair containing client ID and secret
     * @throws Exception if credentials are not configured or invalid
     */
    protected CredentialsPair getCredentials() throws Exception {
        String clientId = ConfigCommand.getConfigValue("client.id");
        String clientSecret = ConfigCommand.getConfigValue("client.secret");
        
        if (clientId == null || clientSecret == null) {
            throw new IllegalStateException("Client credentials not configured. Use 'config set client.id <id>' and 'config set client.secret <secret>'");
        }
        
        errorHandler.reportProgress("Using configured credentials");
        return new CredentialsPair(clientId, clientSecret);
    }
    
    /**
     * Validate file existence and readability.
     * @param file the file to validate
     * @param fileType description of the file type for error messages
     * @throws Exception if file does not exist or cannot be read
     */
    protected void validateFile(java.io.File file, String fileType) throws Exception {
        if (!file.exists()) {
            throw new java.io.FileNotFoundException(fileType + " file does not exist: " + file.getAbsolutePath());
        }
        
        if (!file.canRead()) {
            throw new java.io.IOException("Cannot read " + fileType + " file: " + file.getAbsolutePath());
        }
        
        errorHandler.reportProgress("Validated " + fileType + " file: " + file.getName());
    }
    
    /**
     * Validate audio file format.
     * @param file the audio file to validate
     * @throws Exception if file format is not supported
     */
    protected void validateAudioFormat(java.io.File file) throws Exception {
        String fileName = file.getName().toLowerCase();
        if (!fileName.endsWith(".wav") && !fileName.endsWith(".mp3") && !fileName.endsWith(".ogg")) {
            throw new IllegalArgumentException("Unsupported file format. Supported formats: wav, mp3, ogg");
        }
        
        errorHandler.reportProgress("Audio format validation passed");
    }
    
    /**
     * Validate JSON configuration file format.
     * @param file the configuration file to validate
     * @throws Exception if file format is not supported
     */
    protected void validateConfigFormat(java.io.File file) throws Exception {
        String fileName = file.getName().toLowerCase();
        if (!fileName.endsWith(".json")) {
            throw new IllegalArgumentException("Configuration file must be in JSON format (.json)");
        }
        
        errorHandler.reportProgress("Configuration file format validation passed");
    }
    
    /**
     * Load advanced configuration from JSON file.
     * @param configFile path to the JSON configuration file
     * @return AdvancedConfig instance
     * @throws Exception if file cannot be loaded or parsed
     */
    protected AdvancedConfig loadAdvancedConfig(String configFile) throws Exception {
        if (configFile == null || configFile.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration file path cannot be null or empty");
        }
        
        File file = new File(configFile);
        validateFile(file, "Configuration");
        
        try {
            AdvancedConfig config = AdvancedConfig.fromFile(file);
            errorHandler.reportProgress("Loaded advanced configuration from: " + configFile);
            return config;
        } catch (IOException e) {
            throw new Exception("Failed to parse configuration file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create advanced configuration from simple language options.
     * @param sourceLanguage source language (can be null for auto-detect)
     * @param targetLanguage target language (required)
     * @param reader audio input reader
     * @param writer audio output writer
     * @return AdvancedConfig instance
     * @throws Exception if configuration cannot be created
     */
    protected AdvancedConfig createAdvancedConfigFromSimpleOptions(
            Language sourceLanguage, Language targetLanguage, 
            Reader reader, Writer writer) throws Exception {
        
        if (targetLanguage == null) {
            throw new IllegalArgumentException("Target language is required");
        }
        if (reader == null) {
            throw new IllegalArgumentException("Reader is required");
        }
        if (writer == null) {
            throw new IllegalArgumentException("Writer is required");
        }
        
        // Use auto-detect if source language not specified
        Language effectiveSourceLang = sourceLanguage != null ? sourceLanguage : Language.EN_US;
        String sourceLanguageCode = sourceLanguage != null ? sourceLanguage.getCode() : "auto";
        
        AdvancedConfig config = AdvancedConfig.builder()
            .source(SourceLangConfig.builder()
                .language(effectiveSourceLang)
                .reader(reader)
                .transcription(TranscriptionConfig.builder()
                    .sourceLanguage(sourceLanguageCode)
                    .build())
                .build())
            .addTarget(TargetLangConfig.builder()
                .language(targetLanguage)
                .writer(writer)
                .build())
            .build();
            
        errorHandler.reportProgress("Created advanced configuration: " + 
            sourceLanguageCode + " -> " + targetLanguage.getCode());
        return config;
    }
    
    /**
     * Create advanced configuration with custom audio settings.
     * @param sourceLanguage source language (can be null for auto-detect)
     * @param targetLanguage target language (required)
     * @param reader audio input reader
     * @param writer audio output writer
     * @param sampleRate audio sample rate
     * @param audioFormat audio format ("pcm_s16le", "opus", "wav")
     * @return AdvancedConfig instance
     * @throws Exception if configuration cannot be created
     */
    protected AdvancedConfig createAdvancedConfigWithAudioSettings(
            Language sourceLanguage, Language targetLanguage,
            Reader reader, Writer writer,
            int sampleRate, String audioFormat) throws Exception {
        
        // Validate audio format
        if (!"pcm_s16le".equals(audioFormat) && !"opus".equals(audioFormat) && !"wav".equals(audioFormat)) {
            throw new IllegalArgumentException("Unsupported audio format: " + audioFormat + 
                ". Supported formats: pcm_s16le, opus, wav");
        }
        
        // Validate sample rate
        if (sampleRate < 16000 || sampleRate > 24000) {
            throw new IllegalArgumentException("Sample rate must be between 16000 and 24000, got: " + sampleRate);
        }
        
        AudioFormat inputAudioFormat = AudioFormat.builder()
            .format(audioFormat)
            .sampleRate(sampleRate)
            .channels(1)
            .build();
        
        InputStreamConfig inputStream = InputStreamConfig.websocket(inputAudioFormat);
        OutputStreamConfig outputStream = OutputStreamConfig.builder()
            .contentType("audio")
            .targetType("webrtc")
            .format("pcm_s16le")
            .target(java.util.Map.of(
                "type", "webrtc",
                "format", "pcm_s16le",
                "sample_rate", sampleRate,
                "channels", 1
            ))
            .build();
        
        Language effectiveSourceLang = sourceLanguage != null ? sourceLanguage : Language.EN_US;
        String sourceLanguageCode = sourceLanguage != null ? sourceLanguage.getCode() : "auto";
        
        AdvancedConfig config = AdvancedConfig.builder()
            .source(SourceLangConfig.builder()
                .language(effectiveSourceLang)
                .reader(reader)
                .transcription(TranscriptionConfig.builder()
                    .sourceLanguage(sourceLanguageCode)
                    .build())
                .build())
            .addTarget(TargetLangConfig.builder()
                .language(targetLanguage)
                .writer(writer)
                .build())
            .inputStream(inputStream)
            .outputStream(outputStream)
            .build();
            
        errorHandler.reportProgress("Created advanced configuration with audio settings: " + 
            audioFormat + "@" + sampleRate + "Hz");
        return config;
    }
    
    /**
     * Validate advanced configuration.
     * @param config the configuration to validate
     * @throws Exception if configuration is invalid
     */
    protected void validateAdvancedConfig(AdvancedConfig config) throws Exception {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        
        if (config.getSource() == null) {
            throw new IllegalArgumentException("Source configuration is required");
        }
        
        if (config.getTargets() == null || config.getTargets().isEmpty()) {
            throw new IllegalArgumentException("At least one target configuration is required");
        }
        
        // Validate that readers and writers are set for CLI usage
        if (config.getSource().getReader() == null) {
            throw new IllegalArgumentException("Source reader is required for CLI usage");
        }
        
        for (TargetLangConfig target : config.getTargets()) {
            if (target.getWriter() == null) {
                throw new IllegalArgumentException("Target writer is required for CLI usage: " + 
                    target.getLanguage().getCode());
            }
        }
        
        errorHandler.reportProgress("Advanced configuration validation passed");
    }
    
    /**
     * Report configuration information for user feedback.
     * @param config the configuration to report
     */
    protected void reportConfigurationInfo(AdvancedConfig config) {
        if (config == null) return;
        
        SourceLangConfig source = config.getSource();
        String sourceLang = source.getTranscription().getSourceLanguage();
        if ("auto".equals(sourceLang)) {
            errorHandler.reportInfo("Source language: auto-detect");
        } else {
            errorHandler.reportInfo("Source language: " + sourceLang);
        }
        
        for (TargetLangConfig target : config.getTargets()) {
            errorHandler.reportInfo("Target language: " + target.getLanguage().getCode());
        }
        
        // Report audio format if custom
        InputStreamConfig inputStream = config.getInputStream();
        if (inputStream != null && inputStream.getSource().containsKey("format")) {
            String format = (String) inputStream.getSource().get("format");
            Integer sampleRate = (Integer) inputStream.getSource().get("sample_rate");
            errorHandler.reportInfo("Audio format: " + format + 
                (sampleRate != null ? "@" + sampleRate + "Hz" : ""));
        }
    }
    
    /**
     * Create output directory if it doesn't exist.
     * @param outputDir the output directory to create
     * @throws Exception if directory cannot be created or is not writable
     */
    protected void ensureOutputDirectory(java.io.File outputDir) throws Exception {
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new java.io.IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
            }
            errorHandler.reportInfo("Created output directory: " + outputDir.getAbsolutePath());
        }
        
        if (!outputDir.canWrite()) {
            throw new java.io.IOException("Cannot write to output directory: " + outputDir.getAbsolutePath());
        }
        
        errorHandler.reportProgress("Output directory validated: " + outputDir.getAbsolutePath());
    }
    
    /**
     * Report file size information.
     * @param inputFile the input file to report size for
     * @param outputFile the output file to report size for
     */
    protected void reportFileInfo(java.io.File inputFile, java.io.File outputFile) {
        try {
            if (inputFile.exists()) {
                long inputSize = java.nio.file.Files.size(inputFile.toPath());
                errorHandler.reportInfo(String.format("Input file size: %,d bytes", inputSize));
            }
            
            if (outputFile.exists()) {
                long outputSize = java.nio.file.Files.size(outputFile.toPath());
                errorHandler.reportInfo(String.format("Output file size: %,d bytes", outputSize));
            }
        } catch (Exception e) {
            errorHandler.reportWarning("Could not read file size information: " + e.getMessage());
        }
    }
    
    /**
     * Simple credentials container.
     */
    public static class CredentialsPair {
        /** Client ID for authentication. */
        public final String clientId;
        /** Client secret for authentication. */
        public final String clientSecret;
        
        /**
         * Construct a new credentials pair.
         * @param clientId the client ID
         * @param clientSecret the client secret
         */
        public CredentialsPair(String clientId, String clientSecret) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }
    }
}
