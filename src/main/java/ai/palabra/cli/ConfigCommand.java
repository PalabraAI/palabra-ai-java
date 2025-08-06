package ai.palabra.cli;

import ai.palabra.config.AdvancedConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Configuration management for the CLI tool.
 * Handles loading and saving configuration from files and environment variables.
 */
@Command(
    name = "config",
    description = "Manage CLI configuration",
    mixinStandardHelpOptions = true,
    subcommands = {
        ConfigCommand.SetCommand.class,
        ConfigCommand.GetCommand.class,
        ConfigCommand.ListCommand.class,
        ConfigCommand.ValidateCommand.class,
        ConfigCommand.LoadCommand.class
    },
    footer = {
        "",
        "Configuration Management:",
        "  The config command manages both simple key-value credentials and",
        "  advanced JSON configuration files for complex translation scenarios.",
        "",
        "Simple Configuration (credentials):",
        "  Set credentials:",
        "    palabra config set client.id YOUR_CLIENT_ID",
        "    palabra config set client.secret YOUR_CLIENT_SECRET",
        "",
        "  Get a value:",
        "    palabra config get client.id",
        "",
        "  List all values:",
        "    palabra config list",
        "",
        "Advanced Configuration (JSON files):",
        "  Validate configuration:",
        "    palabra config validate config.json",
        "",
        "  Load and display configuration:",
        "    palabra config load config.json --pretty",
        "",
        "Environment Variables:",
        "  PALABRA_CLIENT_ID     - Client ID",
        "  PALABRA_CLIENT_SECRET - Client secret",
        "  PALABRA_API_URL       - Override API endpoint",
        "",
        "Configuration File Examples:",
        "  See examples/basic-config.json for a simple configuration",
        "  See examples/multi-target-config.json for multi-language translation"
    }
)
public class ConfigCommand implements Callable<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigCommand.class);
    private static final String CONFIG_FILE = System.getProperty("user.home") + "/.palabra-cli.properties";
    
    /**
     * Default behavior when no subcommand is specified.
     * @return exit code (0 for success)
     */
    @Override
    public Integer call() {
        System.out.println("Use 'config --help' to see available configuration commands.");
        System.out.println("\nSupported subcommands:");
        System.out.println("  set <key> <value>    Set a configuration value");
        System.out.println("  get <key>           Get a configuration value");
        System.out.println("  list                List all configuration values");
        System.out.println("  validate <file>     Validate an advanced JSON configuration file");
        System.out.println("  load <file>         Load and display an advanced JSON configuration file");
        return 0;
    }
    
    @Command(name = "set", description = "Set a configuration value", mixinStandardHelpOptions = true)
    static class SetCommand implements Callable<Integer> {
        
        @Parameters(index = "0", description = "Configuration key")
        private String key;
        
        @Parameters(index = "1", description = "Configuration value")
        private String value;
        
        @Override
        public Integer call() {
            try {
                Properties props = loadConfig();
                props.setProperty(key, value);
                saveConfig(props);
                System.out.println("Set " + key + " = " + value);
                return 0;
            } catch (IOException e) {
                System.err.println("Error saving configuration: " + e.getMessage());
                return 1;
            }
        }
    }
    
    @Command(name = "get", description = "Get a configuration value", mixinStandardHelpOptions = true)
    static class GetCommand implements Callable<Integer> {
        
        @Parameters(index = "0", description = "Configuration key")
        private String key;
        
        @Override
        public Integer call() {
            try {
                Properties props = loadConfig();
                String value = props.getProperty(key);
                if (value != null) {
                    System.out.println(key + " = " + value);
                } else {
                    System.out.println(key + " is not set");
                }
                return 0;
            } catch (IOException e) {
                System.err.println("Error loading configuration: " + e.getMessage());
                return 1;
            }
        }
    }
    
    @Command(name = "list", description = "List all configuration values", mixinStandardHelpOptions = true)
    static class ListCommand implements Callable<Integer> {
        
        @Override
        public Integer call() {
            try {
                Properties props = loadConfig();
                if (props.isEmpty()) {
                    System.out.println("No configuration values set.");
                } else {
                    System.out.println("Configuration values:");
                    props.forEach((key, value) -> System.out.println("  " + key + " = " + value));
                }
                return 0;
            } catch (IOException e) {
                System.err.println("Error loading configuration: " + e.getMessage());
                return 1;
            }
        }
    }
    
    /**
     * Load configuration from file and environment variables.
     * Environment variables take precedence over file values.
     */
    public static Properties loadConfig() throws IOException {
        Properties props = new Properties();
        
        // Load from file if it exists
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }
        }
        
        // Override with environment variables
        String clientId = System.getenv("PALABRA_CLIENT_ID");
        if (clientId != null) {
            props.setProperty("client.id", clientId);
        }
        
        String clientSecret = System.getenv("PALABRA_CLIENT_SECRET");
        if (clientSecret != null) {
            props.setProperty("client.secret", clientSecret);
        }
        
        String apiUrl = System.getenv("PALABRA_API_URL");
        if (apiUrl != null) {
            props.setProperty("api.url", apiUrl);
        }
        
        return props;
    }
    
    /**
     * Save configuration to file.
     */
    public static void saveConfig(Properties props) throws IOException {
        File configFile = new File(CONFIG_FILE);
        configFile.getParentFile().mkdirs();
        
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "Palabra AI CLI Configuration");
        }
    }
    
    /**
     * Get a configuration value, checking environment variables first, then config file.
     */
    public static String getConfigValue(String key) {
        try {
            Properties props = loadConfig();
            return props.getProperty(key);
        } catch (IOException e) {
            logger.warn("Error loading configuration: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Load advanced configuration from a JSON file.
     * @param configFile path to the JSON configuration file
     * @return AdvancedConfig instance
     * @throws IOException if file cannot be loaded or parsed
     */
    public static AdvancedConfig loadAdvancedConfig(String configFile) throws IOException {
        File file = new File(configFile);
        if (!file.exists()) {
            throw new IOException("Configuration file does not exist: " + configFile);
        }
        
        if (!file.getName().toLowerCase().endsWith(".json")) {
            throw new IOException("Configuration file must be in JSON format (.json)");
        }
        
        return AdvancedConfig.fromFile(file);
    }
    
    /**
     * Check if a configuration file path is specified and valid.
     * @param configFile the configuration file path to check
     * @return true if the file exists and is readable
     */
    public static boolean isValidConfigFile(String configFile) {
        if (configFile == null || configFile.trim().isEmpty()) {
            return false;
        }
        
        File file = new File(configFile);
        return file.exists() && file.canRead() && file.getName().toLowerCase().endsWith(".json");
    }
    
    @Command(name = "validate", description = "Validate an advanced configuration file", mixinStandardHelpOptions = true)
    static class ValidateCommand implements Callable<Integer> {
        
        @Parameters(index = "0", description = "Path to JSON configuration file")
        private String configFile;
        
        @Override
        public Integer call() {
            try {
                File file = new File(configFile);
                if (!file.exists()) {
                    System.err.println("Configuration file does not exist: " + configFile);
                    return 1;
                }
                
                if (!file.getName().toLowerCase().endsWith(".json")) {
                    System.err.println("Configuration file must be in JSON format (.json)");
                    return 1;
                }
                
                // Try to load and parse the configuration
                AdvancedConfig config = AdvancedConfig.fromFile(file);
                
                // Basic validation - check required fields
                if (config.getSource() == null) {
                    System.err.println("Invalid configuration: source configuration is missing");
                    return 1;
                }
                
                if (config.getTargets() == null || config.getTargets().isEmpty()) {
                    System.err.println("Invalid configuration: at least one target configuration is required");
                    return 1;
                }
                
                System.out.println("✓ Configuration file is valid: " + configFile);
                System.out.println("  Source language: " + 
                    (config.getSource().getTranscription().getSourceLanguage() != null ?
                     config.getSource().getTranscription().getSourceLanguage() : "auto-detect"));
                
                config.getTargets().forEach(target -> 
                    System.out.println("  Target language: " + target.getLanguage().getCode()));
                
                return 0;
            } catch (Exception e) {
                System.err.println("Configuration validation failed: " + e.getMessage());
                return 1;
            }
        }
    }
    
    @Command(name = "load", description = "Load and display an advanced configuration file", mixinStandardHelpOptions = true)
    static class LoadCommand implements Callable<Integer> {
        
        @Parameters(index = "0", description = "Path to JSON configuration file")
        private String configFile;
        
        @Option(names = {"--pretty"}, description = "Pretty-print the configuration")
        private boolean pretty = false;
        
        @Override
        public Integer call() {
            try {
                File file = new File(configFile);
                if (!file.exists()) {
                    System.err.println("Configuration file does not exist: " + configFile);
                    return 1;
                }
                
                AdvancedConfig config = AdvancedConfig.fromFile(file);
                
                if (pretty) {
                    System.out.println("Configuration loaded from: " + configFile);
                    System.out.println("=" + "=".repeat(50));
                    System.out.println(config.toJson());
                } else {
                    System.out.println(config.toJson());
                }
                
                return 0;
            } catch (Exception e) {
                System.err.println("Error loading configuration file: " + e.getMessage());
                return 1;
            }
        }
    }
}
