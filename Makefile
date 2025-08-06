SHELL := /bin/bash
.SHELLFLAGS := -euxo pipefail -c

# Palabra AI Java Client - Makefile
# Provides convenient commands for building, testing, and running the library and CLI
#
# Usage:
#   make help          - Show this help message
#   make build         - Build the library
#   make cli           - Build CLI application
#   make test          - Run all tests
#   make run-cli       - Run CLI application
#   make clean         - Clean build artifacts

# Default Java and Gradle settings
JAVA_VERSION := 17
GRADLE := ./gradlew
PROJECT_NAME := palabra-ai-java
VERSION := 0.1.0

# CLI application settings
CLI_MAIN_CLASS := ai.palabra.cli.PalabraCLI
CLI_JAR := build/libs/$(PROJECT_NAME)-$(VERSION).jar

# Default target
.DEFAULT_GOAL := help

# Help target
.PHONY: help
help: ## Show this help message
	@echo "Palabra AI Java Client - Available Commands"
	@echo ""
	@echo "Building:"
	@awk 'BEGIN {FS = ":.*##"; print "  make build         - Build the library and CLI"} /^[a-zA-Z_-]+:.*?##/ { if ($$1 == "build" || $$1 == "cli" || $$1 == "lib" || $$1 == "compile") printf "  %-15s - %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""
	@echo "Testing:"
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z_-]+:.*?##/ { if ($$1 == "test" || $$1 == "test-unit" || $$1 == "test-integration" || $$1 == "test-cli") printf "  %-15s - %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""
	@echo "Running:"
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z_-]+:.*?##/ { if ($$1 == "run-cli" || $$1 == "run-example" || $$1 == "demo") printf "  %-15s - %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""
	@echo "Documentation:"
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z_-]+:.*?##/ { if ($$1 == "docs" || $$1 == "javadoc") printf "  %-15s - %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""
	@echo "Utility:"
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z_-]+:.*?##/ { if ($$1 == "clean" || $$1 == "check-java" || $$1 == "format" || $$1 == "dist") printf "  %-15s - %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""
	@echo "Examples:"
	@echo "  make cli && make run-cli help               - Build and show CLI help"
	@echo "  make run-cli file test.wav -t ES_MX        - Translate a file"
	@echo "  make run-cli device --list-devices         - List audio devices"
	@echo "  make test && make docs                     - Run tests and generate docs"

# Check Java version
.PHONY: check-java
check-java: ## Check Java version compatibility
	@echo "Checking Java version..."
	@java -version 2>&1 | head -n 1
	@java -version 2>&1 | grep -q "$(JAVA_VERSION)" || (echo "Error: Java $(JAVA_VERSION) required" && exit 1)
	@echo "✓ Java version OK"

# Build targets
.PHONY: build
build: check-java ## Build the library and CLI application
	@echo "Building Palabra AI Java Client..."
	$(GRADLE) build
	@echo "✓ Build completed successfully"

.PHONY: lib
lib: check-java ## Build library only (no tests)
	@echo "Building library..."
	$(GRADLE) compileJava
	@echo "✓ Library compiled"

.PHONY: cli
cli: check-java ## Build CLI application specifically
	@echo "Building CLI application..."
	$(GRADLE) build -x test
	@echo "✓ CLI application built"
	@echo "CLI JAR: $(CLI_JAR)"

.PHONY: compile
compile: check-java ## Compile sources only (fastest)
	@echo "Compiling sources..."
	$(GRADLE) compileJava compileTestJava
	@echo "✓ Compilation completed"

# Test targets
.PHONY: test
test: ## Run all tests
	@echo "Running all tests..."
	$(GRADLE) test
	@echo "✓ Tests completed"

.PHONY: test-unit
test-unit: ## Run unit tests only
	@echo "Running unit tests..."
	$(GRADLE) test --tests "*Test"
	@echo "✓ Unit tests completed"

.PHONY: test-integration
test-integration: ## Run integration tests only
	@echo "Running integration tests..."
	$(GRADLE) test --tests "*IntegrationTest"
	@echo "✓ Integration tests completed"

.PHONY: test-cli
test-cli: cli ## Test CLI application functionality
	@echo "Testing CLI application..."
	@echo "Testing CLI help..."
	$(GRADLE) run --args="--help" || true
	@echo "Testing CLI commands..."
	$(GRADLE) run --args="config --help" || true
	$(GRADLE) run --args="file --help" || true
	$(GRADLE) run --args="device --help" || true
	$(GRADLE) run --args="async --help" || true
	@echo "✓ CLI tests completed"

# Run targets
.PHONY: run-cli
run-cli: cli ## Run CLI application with arguments (use: make run-cli ARGS="--help")
	@echo "Running CLI application..."
	@if [ -n "$(ARGS)" ]; then \
		echo "Command: $(CLI_MAIN_CLASS) $(ARGS)"; \
		$(GRADLE) run --args="$(ARGS)"; \
	else \
		echo "Usage: make run-cli ARGS=\"command options\""; \
		echo "Example: make run-cli ARGS=\"--help\""; \
		echo "Example: make run-cli ARGS=\"device --list-devices\""; \
		$(GRADLE) run --args="--help"; \
	fi

.PHONY: run-example
run-example: build ## Run the example application
	@echo "Running example application..."
	@echo "Make sure to set PALABRA_CLIENT_ID and PALABRA_CLIENT_SECRET"
	$(GRADLE) run --main-class="ai.palabra.example.NanoPalabraExample"

.PHONY: demo
demo: cli ## Run CLI demonstration
	@echo "Running CLI demonstration..."
	@echo "=== CLI Help ==="
	$(GRADLE) run --args="--help" || true
	@echo ""
	@echo "=== File Command Help ==="
	$(GRADLE) run --args="file --help" || true
	@echo ""
	@echo "=== Device Command Help ==="
	$(GRADLE) run --args="device --help" || true
	@echo ""
	@echo "=== Available Audio Devices ==="
	$(GRADLE) run --args="device --list-devices" || true

# Documentation targets
.PHONY: docs
docs: ## Generate all documentation
	@echo "Generating documentation..."
	$(GRADLE) javadoc
	@echo "✓ Documentation generated"
	@echo "JavaDoc: build/docs/javadoc/index.html"

.PHONY: javadoc
javadoc: docs ## Generate JavaDoc (alias for docs)

# Distribution targets
.PHONY: dist
dist: build ## Create distribution packages
	@echo "Creating distribution packages..."
	$(GRADLE) distTar distZip
	@echo "✓ Distribution packages created"
	@echo "TAR: build/distributions/$(PROJECT_NAME)-$(VERSION).tar"
	@echo "ZIP: build/distributions/$(PROJECT_NAME)-$(VERSION).zip"

# Utility targets
.PHONY: clean
clean: ## Clean all build artifacts
	@echo "Cleaning build artifacts..."
	$(GRADLE) clean
	@echo "✓ Clean completed"

.PHONY: format
format: ## Format source code (if formatter is available)
	@echo "Formatting source code..."
	@if command -v google-java-format >/dev/null 2>&1; then \
		find src -name "*.java" -exec google-java-format --replace {} \; && \
		echo "✓ Code formatted"; \
	else \
		echo "google-java-format not found, skipping formatting"; \
	fi

.PHONY: check
check: test ## Run all checks (tests, linting, etc.)
	@echo "Running all checks..."
	$(GRADLE) check
	@echo "✓ All checks passed"

# Publish targets (for future use)
.PHONY: publish-local
publish-local: build ## Publish to local Maven repository
	@echo "Publishing to local Maven repository..."
	$(GRADLE) publishToMavenLocal
	@echo "✓ Published to local repository"

.PHONY: publish-snapshot
publish-snapshot: build ## Publish snapshot to repository
	@echo "Publishing snapshot..."
	@echo "Note: This requires proper repository configuration"
	$(GRADLE) publish
	@echo "✓ Snapshot published"

# Development convenience targets
.PHONY: dev-setup
dev-setup: ## Set up development environment
	@echo "Setting up development environment..."
	@echo "Checking prerequisites..."
	@command -v java >/dev/null 2>&1 || (echo "Java not found" && exit 1)
	@command -v git >/dev/null 2>&1 || (echo "Git not found" && exit 1)
	@echo "✓ Prerequisites OK"
	@echo "Building project..."
	$(MAKE) build
	@echo "✓ Development environment ready"

.PHONY: quick-test
quick-test: ## Quick compilation and unit test check
	@echo "Quick test run..."
	$(GRADLE) compileJava compileTestJava test --fail-fast
	@echo "✓ Quick tests passed"

# CLI convenience commands
.PHONY: cli-help
cli-help: cli ## Show CLI help
	@$(GRADLE) run --args="--help"

.PHONY: cli-config
cli-config: cli ## Show CLI config command help
	@$(GRADLE) run --args="config --help"

.PHONY: cli-devices
cli-devices: cli ## List available audio devices
	@$(GRADLE) run --args="device --list-devices"

# Information targets
.PHONY: info
info: ## Show project information
	@echo "Palabra AI Java Client"
	@echo "Version: $(VERSION)"
	@echo "Java Version: $(JAVA_VERSION)"
	@echo "Project: $(PROJECT_NAME)"
	@echo "Main Class: $(CLI_MAIN_CLASS)"
	@echo ""
	@echo "Project Structure:"
	@echo "  src/main/java/       - Main source code"
	@echo "  src/test/java/       - Test source code"
	@echo "  build/libs/          - Built JAR files"
	@echo "  build/docs/javadoc/  - Generated documentation"
	@echo "  build/distributions/ - Distribution packages"

.PHONY: status
status: ## Show build status
	@echo "Build Status:"
	@if [ -f "$(CLI_JAR)" ]; then \
		echo "✓ JAR exists: $(CLI_JAR)"; \
		echo "  Size: $$(du -h $(CLI_JAR) | cut -f1)"; \
		echo "  Modified: $$(stat -f "%Sm" $(CLI_JAR))"; \
	else \
		echo "✗ JAR not found"; \
	fi
	@if [ -d "build/docs/javadoc" ]; then \
		echo "✓ Documentation generated"; \
	else \
		echo "? Documentation not generated"; \
	fi

# Prevent make from treating files with these names as targets
.PHONY: all clean build test help
