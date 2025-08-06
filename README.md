# Palabra AI Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/ai.palabra/palabra-ai-java)](https://search.maven.org/artifact/ai.palabra/palabra-ai-java)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/github/actions/workflow/status/PalabraAI/palabra-ai-java/ci.yml?branch=main)](https://github.com/PalabraAI/palabra-ai-java/actions)

Real-time speech-to-speech translation SDK for Java applications, powered by Palabra AI's advanced translation API.

## Features

- **Real-time Translation**: Stream audio and receive translations with minimal latency
- **Voice Cloning**: Maintain speaker's voice characteristics across languages
- **25+ Languages**: Support for major world languages and regional variants
- **Multiple I/O Adapters**: File, device (microphone/speaker), and buffer adapters
- **WebSocket Streaming**: Efficient bi-directional audio streaming
- **Advanced Configuration**: Fine-tune transcription, translation, and synthesis settings
- **CLI Application**: Command-line interface for quick testing and integration

## Requirements

- Java 17 or higher
- Palabra AI API credentials ([Sign up](https://palabra.ai))

## Installation

### Maven

```xml
<dependency>
    <groupId>ai.palabra</groupId>
    <artifactId>palabra-ai-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```gradle
dependencies {
    implementation 'ai.palabra:palabra-ai-java:1.0.0'
}
```

### Building from Source

```bash
git clone https://github.com/PalabraAI/palabra-ai-java.git
cd palabra-ai-java
./gradlew build
```

## Quick Start

### Setting API Credentials

Set your API credentials as environment variables:

```bash
export PALABRA_CLIENT_ID="your-client-id"
export PALABRA_CLIENT_SECRET="your-client-secret"
```

### Basic Usage

```java
import ai.palabra.*;
import ai.palabra.adapter.*;

public class TranslationExample {
    public static void main(String[] args) {
        // Initialize client with credentials
        String clientId = System.getenv("PALABRA_CLIENT_ID");
        String clientSecret = System.getenv("PALABRA_CLIENT_SECRET");
        
        PalabraAI client = new PalabraAI(clientId, clientSecret);
        
        // Configure translation
        Config config = Config.builder()
            .sourceLang(Language.EN_US)
            .targetLang(Language.ES_MX)
            .reader(new FileReader("input.wav"))
            .writer(new FileWriter("output.wav"))
            .build();
        
        // Run translation
        client.run(config);
    }
}
```

## SDK Usage Examples

### Real-time Microphone Translation

Translate speech from microphone to speakers in real-time:

```java
String clientId = System.getenv("PALABRA_CLIENT_ID");
String clientSecret = System.getenv("PALABRA_CLIENT_SECRET");

Config config = Config.builder()
    .sourceLang(Language.EN_US)
    .targetLang(Language.JA)  // Japanese
    .reader(new DeviceReader())  // Default microphone
    .writer(new DeviceWriter())  // Default speakers
    .build();

// Run for 30 seconds
PalabraAI client = new PalabraAI(clientId, clientSecret);
CompletableFuture<Void> future = client.runAsync(config);

Thread.sleep(30000);  // Translate for 30 seconds
client.cancel();  // Cancel the translation
future.join();
```

### File-to-File Translation

Translate audio files between languages:

```java
String clientId = System.getenv("PALABRA_CLIENT_ID");
String clientSecret = System.getenv("PALABRA_CLIENT_SECRET");

PalabraAI client = new PalabraAI(clientId, clientSecret);

Config config = Config.builder()
    .sourceLang(Language.EN_US)
    .targetLang(Language.FR)  // French
    .reader(new FileReader("english_audio.wav"))
    .writer(new FileWriter("french_audio.wav"))
    .build();

client.run(config);
```

### Buffer-based Translation

Process audio from memory buffers:

```java
String clientId = System.getenv("PALABRA_CLIENT_ID");
String clientSecret = System.getenv("PALABRA_CLIENT_SECRET");

// Create buffers
BufferReader reader = new BufferReader();
BufferWriter writer = new BufferWriter();

// Load audio data into reader
byte[] audioData = Files.readAllBytes(Paths.get("audio.wav"));
reader.addData(audioData);

Config config = Config.builder()
    .sourceLang(Language.DE)  // German
    .targetLang(Language.EN_US)
    .reader(reader)
    .writer(writer)
    .build();

// Process translation
PalabraAI client = new PalabraAI(clientId, clientSecret);
client.run(config);

// Get translated audio
byte[] translatedAudio = writer.getData();
Files.write(Paths.get("translated.wav"), translatedAudio);
```

### Multiple Target Languages

Translate to multiple languages simultaneously:

```java
String clientId = System.getenv("PALABRA_CLIENT_ID");
String clientSecret = System.getenv("PALABRA_CLIENT_SECRET");

Config config = Config.builder()
    .advanced()
    .source(Language.EN_US, new FileReader("input.wav"))
    .addTarget(Language.ES_MX, new FileWriter("spanish.wav"))
    .addTarget(Language.FR, new FileWriter("french.wav"))  // French
    .addTarget(Language.DE, new FileWriter("german.wav"))   // German
    .build();

PalabraAI client = new PalabraAI(clientId, clientSecret);
client.run(config);
```

### Advanced Configuration

Fine-tune translation parameters:

```java
import ai.palabra.config.*;

String clientId = System.getenv("PALABRA_CLIENT_ID");
String clientSecret = System.getenv("PALABRA_CLIENT_SECRET");

PalabraAI client = new PalabraAI(clientId, clientSecret);

Config config = Config.builder()
    .advanced()
    .source(SourceLangConfig.builder()
        .language(Language.EN_US)
        .reader(new FileReader("input.wav"))
        .transcription(TranscriptionConfig.builder()
            .denoise("auto")
            .segmentConfirmationSilenceThreshold(0.8f)
            .build())
        .build())
    .addTarget(TargetLangConfig.builder()
        .language(Language.ES_MX)
        .writer(new FileWriter("output.wav"))
        .translation(TranslationConfig.builder()
            .translationModel("advanced")
            .speechGeneration(SpeechGenerationConfig.builder()
                .voiceId("default_low")
                .build())
            .build())
        .build())
    .inputStream(InputStreamConfig.builder()
        .audioFormat(AudioFormat.builder()
            .format("pcm_s16le")
            .sampleRate(24000)
            .channels(1)
            .build())
        .build())
    .build();

client.run(config);
```

### Loading Configuration from JSON

```java
String clientId = System.getenv("PALABRA_CLIENT_ID");
String clientSecret = System.getenv("PALABRA_CLIENT_SECRET");

// Load from file
Config config = Config.fromFile(Paths.get("config.json"));
PalabraAI client = new PalabraAI(clientId, clientSecret);
client.run(config);

// Load from JSON string
String json = """
{
    "source": {
        "lang": "EN_US",
        "transcription": {
            "denoise": "auto"
        }
    },
    "targets": [{
        "lang": "ES_MX",
        "translation": {
            "speech_generation": {
                "voice_id": "default_low"
            }
        }
    }]
}
""";
Config config2 = Config.fromJson(json);
```

## CLI Application

The SDK includes a command-line interface for quick testing and integration.

### Installation

After building from source:

```bash
# Add to PATH (optional)
export PATH=$PATH:/path/to/palabra-ai-java/build/install/palabra-cli/bin

# Or use directly
./build/install/palabra-cli/bin/palabra-cli --help
```

### CLI Commands

#### File Translation

Translate audio files:

```bash
# Basic translation
./gradlew run --args="file --input audio.wav --target ES_MX --output spanish.wav"

# With source language specification
./gradlew run --args="file -i recording.wav -s EN_US -t FR_FR -o french.wav"

# Using configuration file
./gradlew run --args="file -i input.wav --config-file config.json -o output.wav"
```

#### Device Translation

Real-time microphone translation:

```bash
# List available audio devices
./gradlew run --args="device --list-devices"

# Translate for 30 seconds
./gradlew run --args="device -s EN_US -t ES_MX -d 30"

# With specific devices
./gradlew run --args="device --input-device 0 --output-device 1 -t JA_JP -d 60"
```

#### Configuration Management

Manage API credentials:

```bash
# Set credentials
./gradlew run --args="config --set"

# Show current configuration
./gradlew run --args="config --show"

# Clear stored credentials
./gradlew run --args="config --clear"
```

## Supported Languages

The SDK supports 25+ languages including:

- English (US, UK, AU, IN)
- Spanish (ES, MX, AR)
- French (FR, CA)
- German (DE)
- Italian (IT)
- Portuguese (PT, BR)
- Russian (RU)
- Chinese (ZH_CN)
- Japanese (JA_JP)
- Korean (KO_KR)
- Arabic (AR)
- Hindi (HI_IN)
- And more...

Use the `Language` enum for type-safe language selection:

```java
Language.EN_US  // English (United States)
Language.ES_MX  // Spanish (Mexico)
Language.FR     // French
Language.DE     // German
Language.JA     // Japanese
Language.KO     // Korean
Language.ZH     // Chinese
// ... etc
```

## Testing

Run the test suite:

```bash
# All tests (requires API credentials for integration tests)
./gradlew test

# Unit tests only
./gradlew test --tests "*Test"

# Integration tests (requires credentials)
./gradlew test --tests "*IntegrationTest"

# Run specific test class
./gradlew test --tests "ai.palabra.PalabraAITest"

# Run with detailed output
./gradlew test --info
```

## Roadmap

- [ ] Maven distribution
- [ ] Quick start examples


## Support

- 📚 Documentation: [https://docs.palabra.ai](https://docs.palabra.ai)
- 🐛 Issues: [GitHub Issues](https://github.com/PalabraAI/palabra-ai-java/issues)
- 📧 Email: info@palabra.ai

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.


## Acknowledgments

Built with:
- [OkHttp](https://square.github.io/okhttp/) - HTTP & WebSocket client
- [Jackson](https://github.com/FasterXML/jackson) - JSON processing
- [Picocli](https://picocli.info/) - CLI framework
- [SLF4J](https://www.slf4j.org/) - Logging facade


---

© Palabra.ai, 2025 | 🌍 Breaking down language barriers with AI 🚀
