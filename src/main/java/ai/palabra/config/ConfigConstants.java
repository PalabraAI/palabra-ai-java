package ai.palabra.config;

/**
 * Configuration constants matching Python client defaults.
 * These are WebSocket-specific defaults for the Palabra AI translation service.
 */
public final class ConfigConstants {
    private ConfigConstants() {} // Prevent instantiation
    
    // Audio format defaults
    public static final String DEFAULT_AUDIO_FORMAT = "pcm_s16le";
    public static final int DEFAULT_SAMPLE_RATE = 24000;
    public static final int DEFAULT_CHANNELS = 1;
    public static final int OPTIMAL_CHUNK_LENGTH_MS = 320;
    
    // Stream defaults
    public static final String WEBSOCKET_CONTENT_TYPE = "ws";
    public static final String AUDIO_CONTENT_TYPE = "audio";
    
    // Transcription defaults
    public static final float SEGMENT_CONFIRMATION_SILENCE_THRESHOLD_DEFAULT = 0.7f;
    public static final float VAD_THRESHOLD_DEFAULT = 0.5f;
    public static final int VAD_LEFT_PADDING_DEFAULT = 500;
    public static final int VAD_RIGHT_PADDING_DEFAULT = 500;
    public static final float MIN_ALIGNMENT_SCORE_DEFAULT = 0.8f;
    public static final float MAX_ALIGNMENT_CER_DEFAULT = 0.3f;
    
    // Sentence splitter defaults
    public static final int MIN_SENTENCE_CHARACTERS_DEFAULT = 5;
    public static final int MIN_SENTENCE_SECONDS_DEFAULT = 1;
    public static final float MIN_SPLIT_INTERVAL_DEFAULT = 0.5f;
    public static final int CONTEXT_SIZE_DEFAULT = 512;
    public static final int SEGMENTS_AFTER_RESTART_DEFAULT = 2;
    public static final int STEP_SIZE_DEFAULT = 128;
    public static final int MAX_STEPS_WITHOUT_EOS_DEFAULT = 10;
    public static final float FORCE_END_OF_SEGMENT_DEFAULT = 10.0f;
    
    // Translation queue defaults
    public static final int DESIRED_QUEUE_LEVEL_MS_DEFAULT = 5000;
    public static final int MAX_QUEUE_LEVEL_MS_DEFAULT = 20000;
    
    // Speech generation defaults
    public static final float SPEECH_TEMPO_ADJUSTMENT_FACTOR_DEFAULT = 1.0f;
    public static final float F0_VARIANCE_FACTOR_DEFAULT = 1.0f;
    public static final float ENERGY_VARIANCE_FACTOR_DEFAULT = 1.0f;
    
    // Filler phrases defaults
    public static final int MIN_TRANSCRIPTION_LEN_DEFAULT = 10;
    public static final int MIN_TRANSCRIPTION_TIME_DEFAULT = 2;
    public static final float PHRASE_CHANCE_DEFAULT = 0.5f;
    
    // Default voice IDs
    public static final String DEFAULT_VOICE_ID = "default_low";
    public static final String DEFAULT_HIGH_TIMBRE_VOICE = "default_high";
    public static final String DEFAULT_LOW_TIMBRE_VOICE = "default_low";
    
    // Model defaults
    public static final String AUTO_MODEL = "auto";
    public static final String DENOISE_NONE = "none";
    public static final String PRIORITY_NORMAL = "normal";
    public static final String VOICE_CLONING_MODE_STATIC_10 = "static_10";
}