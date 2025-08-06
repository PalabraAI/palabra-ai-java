package ai.palabra;

/**
 * Language enumeration with all supported languages and dialects.
 * Mirrors the Python library Language enum exactly with all 46 supported languages.
 */
public enum Language {
    // Arabic variants
    AR("ar", "Arabic"),
    AR_AE("ar-ae", "Arabic (UAE)"),
    AR_SA("ar-sa", "Arabic (Saudi Arabia)"),
    
    // Azerbaijani
    AZ("az", "Azerbaijani"),
    
    // Bulgarian
    BG("bg", "Bulgarian"),
    
    // Czech
    CS("cs", "Czech"),
    
    // Danish
    DA("da", "Danish"),
    
    // German
    DE("de", "German"),
    
    // Greek
    EL("el", "Greek"),
    
    // English variants
    EN("en", "English"),
    EN_AU("en-au", "English (Australia)"),
    EN_CA("en-ca", "English (Canada)"),
    EN_GB("en-gb", "English (UK)"),
    EN_US("en-us", "English (US)"),
    
    // Spanish variants
    ES("es", "Spanish"),
    ES_MX("es-mx", "Spanish (Mexico)"),
    
    // Finnish
    FI("fi", "Finnish"),
    
    // Filipino
    FIL("fil", "Filipino"),
    
    // French variants
    FR("fr", "French"),
    FR_CA("fr-ca", "French (Canada)"),
    
    // Hebrew
    HE("he", "Hebrew"),
    
    // Hindi
    HI("hi", "Hindi"),
    
    // Croatian
    HR("hr", "Croatian"),
    
    // Hungarian
    HU("hu", "Hungarian"),
    
    // Indonesian
    ID("id", "Indonesian"),
    
    // Italian
    IT("it", "Italian"),
    
    // Japanese
    JA("ja", "Japanese"),
    
    // Korean
    KO("ko", "Korean"),
    
    // Malay
    MS("ms", "Malay"),
    
    // Dutch
    NL("nl", "Dutch"),
    
    // Norwegian
    NO("no", "Norwegian"),
    
    // Polish
    PL("pl", "Polish"),
    
    // Portuguese variants
    PT("pt", "Portuguese"),
    PT_BR("pt-br", "Portuguese (Brazil)"),
    
    // Romanian
    RO("ro", "Romanian"),
    
    // Russian
    RU("ru", "Russian"),
    
    // Slovak
    SK("sk", "Slovak"),
    
    // Swedish
    SV("sv", "Swedish"),
    
    // Tamil
    TA("ta", "Tamil"),
    
    // Turkish
    TR("tr", "Turkish"),
    
    // Ukrainian
    UK("uk", "Ukrainian"),
    
    // Vietnamese
    VN("vn", "Vietnamese"),
    
    // Chinese
    ZH("zh", "Chinese");
    
    /** The language code (e.g., "en-US", "es-MX"). */
    private final String code;
    
    /** The display name of the language (e.g., "English (US)", "Spanish (Mexico)"). */
    private final String displayName;
    
    /**
     * Creates a new Language enum value.
     * 
     * @param code the language code
     * @param displayName the human-readable display name
     */
    Language(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
    
    /**
     * Gets the language code.
     * 
     * @return The language code (e.g., "en-US")
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Gets the simple language code for server API (e.g., "en" instead of "en-US").
     * 
     * @return The simple language code
     */
    public String getSimpleCode() {
        if (code.contains("-")) {
            return code.split("-")[0];
        }
        return code;
    }
    
    /**
     * Gets the display name of the language.
     * 
     * @return The human-readable language name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Finds a language by its code.
     * 
     * @param code The language code to search for
     * @return The matching Language enum value
     * @throws IllegalArgumentException if the language code is not found
     */
    public static Language fromCode(String code) {
        for (Language lang : values()) {
            if (lang.code.equalsIgnoreCase(code)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("Unsupported language code: " + code);
    }
    
    /**
     * Finds a language by its simple code (e.g., "en" for "en-US").
     * 
     * @param simpleCode The simple language code to search for
     * @return The matching Language enum value
     * @throws IllegalArgumentException if the language code is not found
     */
    public static Language fromSimpleCode(String simpleCode) {
        for (Language lang : values()) {
            if (lang.getSimpleCode().equalsIgnoreCase(simpleCode)) {
                return lang;
            }
        }
        throw new IllegalArgumentException("Unsupported simple language code: " + simpleCode);
    }
    
    @Override
    public String toString() {
        return displayName + " (" + code + ")";
    }
}
