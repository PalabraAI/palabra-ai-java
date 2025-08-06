package ai.palabra.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Main CLI entry point for the Palabra AI Java client.
 * Provides command-line interface for file and device-based translation.
 */
@Command(
    name = "palabra-cli",
    description = "Palabra AI - Real-time Speech Translation CLI",    
    mixinStandardHelpOptions = true,
    subcommands = {
        FileTranslateCommand.class,
        DeviceTranslateCommand.class,
        ConfigCommand.class
    }
)
public class PalabraCLI implements Callable<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(PalabraCLI.class);
    
    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose = false;
    
    @Option(names = {"-q", "--quiet"}, description = "Quiet mode (minimal output)")
    private boolean quiet = false;
    
    /**
     * Main entry point for the CLI application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new PalabraCLI()).execute(args);
        System.exit(exitCode);
    }
    
    /**
     * Default command behavior when no subcommand is specified.
     * Shows the help message.
     * @return exit code (0 for success)
     */
    @Override
    public Integer call() {
        // When no subcommand is provided, show help
        CommandLine.usage(this, System.out);
        return 0;
    }
    
    /**
     * @return true if verbose mode is enabled
     */
    public boolean isVerbose() {
        return verbose;
    }
    
    /**
     * @return true if quiet mode is enabled
     */
    public boolean isQuiet() {
        return quiet;
    }
}
