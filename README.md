# <a href="https://palabra.ai"><img src="https://avatars.githubusercontent.com/u/199107821?s=32" alt="Palabra AI" align="center"></a> Palabra AI Java SDK

[![Build Status](https://img.shields.io/github/actions/workflow/status/PalabraAI/palabra-ai-java/ci.yml?branch=main&label=build)](https://github.com/PalabraAI/palabra-ai-java/actions)
[![Coverage](https://codecov.io/gh/PalabraAI/palabra-ai-java/branch/main/graph/badge.svg)](https://codecov.io/gh/PalabraAI/palabra-ai-java)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ai.palabra/palabra-ai-java/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ai.palabra/palabra-ai-java)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

🌍 **Java SDK for Palabra AI's real-time speech-to-speech translation API**  
🚀 Break down language barriers and enable seamless communication across 25+ languages

## Overview 📋

🎯 **The Palabra AI Java SDK provides a high-level API for integrating real-time speech-to-speech translation into your Java applications.**

✨ **What can Palabra.ai do?**
- ⚡ Real-time speech-to-speech translation with near-zero latency
- 🎙️ Auto voice cloning - speak any language in YOUR voice
- 🔄 Two-way simultaneous translation for live discussions
- 🚀 Developer API/SDK for building your own apps
- 🎯 Works everywhere - applications, services, any Java platform
- 🔒 Zero data storage - your conversations stay private

🔧 **This SDK focuses on making real-time translation simple and accessible:**
- 🛡️ Uses WebSocket protocol under the hood
- ⚡ Abstracts away all complexity
- 🎮 Simple configuration with source/target languages
- 🎤 Supports multiple input/output adapters (microphones, speakers, files, buffers)

📊 **How it works:**
1. 🎤 Configure input/output adapters
2. 🔄 SDK handles the entire pipeline
3. 🎯 Automatic transcription, translation, and synthesis
4. 🔊 Real-time audio stream ready for playback

💡 **All with just a few lines of code!**

## Installation 📦

### Gradle 📦
```gradle
dependencies {
    implementation 'ai.palabra:palabra-ai-java:0.1.0'
}
```

### Maven 📦
```xml
<dependency>
    <groupId>ai.palabra</groupId>
    <artifactId>palabra-ai-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Quick Start 🚀

### Real-time microphone translation 🎤

```java
import ai.palabra.*;
import ai.palabra.adapter.*;

PalabraAI client = new PalabraAI("your-client-id", "your-client-secret");
Config config = Config.builder()
    .sourceLang(Language.EN_US)
    .targetLang(Language.ES_MX)
    .reader(new DeviceReader())  // Microphone
    .writer(new DeviceWriter())  // Speakers
    .build();
client.run(config);
```

⚙️ **Set your API credentials as environment variables:**
```bash
export PALABRA_CLIENT_ID="your-client-id"
export PALABRA_CLIENT_SECRET="your-client-secret"
```

## Examples 💡

### File-to-file translation 📁

```java
import ai.palabra.*;
import ai.palabra.adapter.*;

PalabraAI client = new PalabraAI("your-client-id", "your-client-secret");
Config config = Config.builder()
    .sourceLang(Language.EN_US)
    .targetLang(Language.ES_MX)
    .reader(new FileReader("input.wav"))
    .writer(new FileWriter("output.wav"))
    .build();
client.run(config);
```

### Multiple target languages 🌐

```java
import ai.palabra.*;
import ai.palabra.adapter.*;

PalabraAI client = new PalabraAI("your-client-id", "your-client-secret");
Config config = Config.builder()
    .sourceLang(Language.EN_US)
    .reader(new FileReader("presentation.mp3"))
    .targetLang(Language.ES_MX, new FileWriter("spanish.wav"))
    .targetLang(Language.FR, new FileWriter("french.wav"))
    .targetLang(Language.DE, new FileWriter("german.wav"))
    .build();
client.run(config);
```

### Device-based translation (Live Microphone) 🎤

```java
import ai.palabra.*;
import ai.palabra.adapter.*;

PalabraAI client = new PalabraAI("your-client-id", "your-client-secret");
Config config = Config.builder()
    .sourceLang(Language.EN_US)
    .targetLang(Language.FR)
    .reader(new DeviceReader())  // Microphone
    .writer(new DeviceWriter())  // Speakers
    .build();
client.run(config);
```

### Asynchronous processing ⚡

```java
import java.util.concurrent.CompletableFuture;

CompletableFuture<Void> translation = client.runAsync(config);
translation.thenRun(() -> System.out.println("Translation completed"));
```

### Using buffers 💾

```java
import ai.palabra.*;
import ai.palabra.adapter.*;
import java.nio.file.Files;
import java.nio.file.Paths;

byte[] audioBytes = Files.readAllBytes(Paths.get("input.wav"));
BufferWriter outputBuffer = new BufferWriter();

Config config = Config.builder()
    .sourceLang(Language.EN_US)
    .targetLang(Language.ES_MX)
    .reader(new BufferReader(audioBytes))
    .writer(outputBuffer)
    .build();

client.run(config);
byte[] translatedAudio = outputBuffer.getBytes();
```

### Standalone Examples (Ready to Run) 🎯
- **NanoExample.java** - Minimal WebSocket client without SDK dependencies
- **SDKExample.java** - Comprehensive SDK usage with all features

Both examples are self-contained and can be run with simple compilation commands. See the [examples README](examples/README.md) for detailed usage instructions and helper scripts.

```bash
# Set environment variables
export PALABRA_CLIENT_ID="your-client-id"
export PALABRA_CLIENT_SECRET="your-client-secret"

# Run using helper scripts (recommended)
cd examples
./setup.sh                    # Check environment
./run_nano.sh                 # Run minimal WebSocket example
./run_sdk.sh                  # Run full SDK example

# Or compile and run manually
cd examples
javac -cp ".:lib/*" NanoExample.java
java -cp ".:lib/*" NanoExample
```

## I/O Adapters & Mixing 🔌

### Available adapters 🛠️

🎯 **The Palabra AI Java SDK provides flexible I/O adapters that can be combined to:**

- 📁 **FileReader/FileWriter**: Read from and write to audio files (WAV, MP3, OGG formats)
- 🎤 **DeviceReader/DeviceWriter**: Use microphones and speakers directly
- 💾 **BufferReader/BufferWriter**: Work with in-memory audio buffers

### Mixing examples 🎨

🔄 **Combine any input adapter with any output adapter:**

#### 🎤➡️📁 Microphone to file - record translations
```java
Config config = Config.builder()
    .sourceLang(Language.EN_US)
    .targetLang(Language.ES_MX)
    .reader(new DeviceReader())
    .writer(new FileWriter("recording_es.wav"))
    .build();
```

#### 📁➡️🔊 File to speaker - play translations
```java
Config config = Config.builder()
    .sourceLang(Language.EN_US)
    .targetLang(Language.ES_MX)
    .reader(new FileReader("presentation.mp3"))
    .writer(new DeviceWriter())
    .build();
```

#### 🎤➡️🔊📁 Microphone to multiple outputs
```java
Config config = Config.builder()
    .sourceLang(Language.EN_US)
    .reader(new DeviceReader())
    .targetLang(Language.ES_MX, new DeviceWriter())       // Play Spanish through speaker
    .targetLang(Language.ES_MX, new FileWriter("spanish.wav"))  // Save Spanish to file
    .targetLang(Language.FR, new FileWriter("french.wav"))      // Save French to file
    .build();
```

#### 💾➡️💾 Buffer to buffer - for integration
```java
byte[] inputAudio = getAudioFromSomewhere();
BufferWriter outputBuffer = new BufferWriter();

Config config = Config.builder()
    .sourceLang(Language.EN_US)
    .targetLang(Language.ES_MX)
    .reader(new BufferReader(inputAudio))
    .writer(outputBuffer)
    .build();
```

## Features ✨

### Real-time translation ⚡
🎯 Translate audio streams in real-time with minimal latency  
💬 Perfect for live conversations, conferences, and meetings

### Voice cloning 🗣️
🎭 Preserve the original speaker's voice characteristics in translations  
⚙️ Enable voice cloning in the configuration

### Type-safe Java API 🛡️
🔧 Modern Java 17+ API with builder patterns and immutable configurations  
🎯 Comprehensive error handling and logging support

### Production-ready 🚀
- **End-to-End Audio Translation**: Complete pipeline from input to output
- **Production Error Handling**: Comprehensive exception hierarchy
- **Multi-format Support**: File, buffer, and device adapters
- **Async Processing**: Non-blocking translation operations
- **CLI Interface**: Professional command-line tools

## Supported Languages 🌍

### Speech recognition languages 🎤
🇸🇦 Arabic (AR), 🇨🇳 Chinese (ZH), 🇨🇿 Czech (CS), 🇩🇰 Danish (DA), 🇳🇱 Dutch (NL), 🇬🇧 English (EN), 🇫🇮 Finnish (FI), 🇫🇷 French (FR), 🇩🇪 German (DE), 🇬🇷 Greek (EL), 🇮🇱 Hebrew (HE), 🇭🇺 Hungarian (HU), 🇮🇹 Italian (IT), 🇯🇵 Japanese (JA), 🇰🇷 Korean (KO), 🇵🇱 Polish (PL), 🇵🇹 Portuguese (PT), 🇷🇺 Russian (RU), 🇪🇸 Spanish (ES), 🇹🇷 Turkish (TR), 🇺🇦 Ukrainian (UK)

### Translation languages 🔄
🇸🇦 Arabic (AR), 🇧🇬 Bulgarian (BG), 🇨🇳 Chinese Mandarin (ZH), 🇨🇿 Czech (CS), 🇩🇰 Danish (DA), 🇳🇱 Dutch (NL), 🇬🇧 English UK (EN_GB), 🇺🇸 English US (EN_US), 🇫🇮 Finnish (FI), 🇫🇷 French (FR), 🇩🇪 German (DE), 🇬🇷 Greek (EL), 🇮🇱 Hebrew (HE), 🇭🇺 Hungarian (HU), 🇮🇩 Indonesian (ID), 🇮🇹 Italian (IT), 🇯🇵 Japanese (JA), 🇰🇷 Korean (KO), 🇵🇱 Polish (PL), 🇵🇹 Portuguese (PT), 🇧🇷 Portuguese Brazilian (PT_BR), 🇷🇴 Romanian (RO), 🇷🇺 Russian (RU), 🇸🇰 Slovak (SK), 🇪🇸 Spanish (ES), 🇲🇽 Spanish Mexican (ES_MX), 🇸🇪 Swedish (SV), 🇹🇷 Turkish (TR), 🇺🇦 Ukrainian (UK), 🇻🇳 Vietnamese (VI)

### Available language constants 📚

```java
import ai.palabra.Language;

// English variants - 1.5+ billion speakers (including L2)
Language.EN, Language.EN_AU, Language.EN_CA, Language.EN_GB, Language.EN_US

// Chinese - 1.3+ billion speakers  
Language.ZH_CN, Language.ZH_TW

// Spanish variants - 500+ million speakers
Language.ES, Language.ES_MX, Language.ES_ES, Language.ES_AR

// Arabic variants - 400+ million speakers
Language.AR

// French variants - 280+ million speakers
Language.FR, Language.FR_CA

// Portuguese variants - 260+ million speakers
Language.PT, Language.PT_BR, Language.PT_PT

// Russian - 260+ million speakers
Language.RU

// Japanese & Korean - 200+ million speakers combined
Language.JA, Language.KO

// German and other European languages
Language.DE, Language.IT, Language.PL, Language.NL
// ... and many more
```

## Building 🔨

### Using Make (Recommended) 🛠️

The project includes a comprehensive Makefile with convenient commands:

```bash
# Show all available commands
make help

# Build everything (library + CLI)
make build

# Build just the CLI application
make cli

# Run tests
make test

# Generate documentation
make docs

# Clean build artifacts
make clean

# Show project status
make status
```

### Using Gradle Directly 📦

```bash
# Build the library
./gradlew build

# Run tests
./gradlew test

# Generate documentation
./gradlew javadoc
```

## CLI Usage 💻

The library includes a comprehensive command-line interface for translation tasks:

```bash
# Using Make
make run-cli ARGS="--help"
make run-cli ARGS="file input.wav -t ES_MX -o output.wav"
make run-cli ARGS="device -s EN_US -t FR"
make run-cli ARGS="device --list-devices"
make run-cli ARGS="async input.wav -t ES_MX --audio-output --buffer-output"

# Using shell script
./build.sh run-cli "--help"
./build.sh run-cli "file input.wav -t ES_MX -o output.wav"
./build.sh run-cli "device --list-devices"

# Using Gradle directly
./gradlew run --args="--help"
./gradlew run --args="file input.wav -t ES_MX -o output.wav"
```

### CLI Commands 🎯

- **`file`** - Translate audio files with comprehensive options
- **`device`** - Real-time translation using microphone and speakers  
- **`async`** - Advanced async processing with multiple output formats
- **`config`** - Manage CLI configuration and credentials

## Troubleshooting 🔧

### Authentication Problems 🔑
```bash
# Verify your credentials are set correctly
export PALABRA_CLIENT_ID="your-client-id"
export PALABRA_CLIENT_SECRET="your-client-secret"

# Test with config command
make run-cli ARGS="config list"
```

### Audio Device Issues 🎤
```bash
# List available audio devices
make run-cli ARGS="device --list-devices"

# Test audio system
make run-cli ARGS="device -s EN_US -t ES_MX -d 5"
```

### Network Connectivity 🌐
- Ensure your firewall allows WebSocket connections to `wss://api.palabra.ai`
- Check that your network supports persistent WebSocket connections
- For corporate networks, you may need to configure proxy settings

## Development Status 🛠️

### Current Status ✅
- ✅ Core SDK functionality
- ✅ GitHub Actions CI/CD
- ✅ Java 17+ support with modern APIs
- ✅ Maven Central distribution
- ✅ Production-ready error handling
- ✅ Comprehensive documentation
- ⏳ Code coverage reporting (setup required)

### Current Dev Roadmap 🗺️
- ⏳ TODO: Global timeout support for long-running tasks
- ⏳ TODO: Support for multiple source languages in a single run
- ⏳ TODO: Enhanced audio device management
- ⏳ TODO: Streaming audio processing improvements

### Build Status 🏗️
- 🧪 **Tests**: Running on Java 17+
- 📦 **Release**: Automated releases to Maven Central
- 📊 **Coverage**: Tests implemented, reporting setup needed

## Requirements 📋

- 🚀 Java 17 or higher
- 🔑 Palabra AI API credentials (get them at [palabra.ai](https://palabra.ai))
- 🌐 Network connectivity for Palabra AI API access
- 🎤 Audio device access for DeviceReader/DeviceWriter (optional)

## Support 🤝

- 📚 Documentation: [https://docs.palabra.ai](https://docs.palabra.ai)
- 🐛 Issues: [GitHub Issues](https://github.com/PalabraAI/palabra-ai-java/issues)
- 📧 Email: info@palabra.ai

## License 📄

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

© Palabra.ai, 2025 | 🌍 Breaking down language barriers with AI 🚀
