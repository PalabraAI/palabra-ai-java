plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    id("application")
    id("jacoco")
}

group = "ai.palabra"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.2")
    
    // WebSocket client
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    
    // CLI parsing
    implementation("info.picocli:picocli:4.7.5")
    annotationProcessor("info.picocli:picocli-codegen:4.7.5")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.12")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.14")
    
    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
    
    // Explicit test runtime dependencies (required for Gradle 9.0 compatibility)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    
    // Exclude integration tests by default unless credentials are available
    val hasCredentials = System.getenv("PALABRA_CLIENT_ID") != null && 
                        System.getenv("PALABRA_CLIENT_SECRET") != null
    
    if (!hasCredentials) {
        exclude("**/integration/**")
        println("⏭️  Skipping integration tests - API credentials not available")
    } else {
        println("🧪 Running all tests including integration tests")
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.compileTestJava {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.javadoc {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        charSet = "UTF-8"
        docEncoding = "UTF-8"
        addStringOption("Xdoclint:none", "-quiet")
    }
}

// Application configuration for running the CLI
application {
    mainClass.set("ai.palabra.cli.PalabraCLI")
    
    // Configure JVM arguments for UTF-8 support
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=UTF-8",
        "-Dconsole.encoding=UTF-8",
        "-Djava.awt.headless=true"
    )
}

// Publishing configuration for Maven Central
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("Palabra AI Java Client")
                description.set("Java client library for Palabra AI real-time speech-to-speech translation API")
                url.set("https://github.com/PalabraAI/palabra-ai-java")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("palabraai")
                        name.set("PalabraAI")
                        email.set("support@palabra.ai")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/PalabraAI/palabra-ai-java.git")
                    developerConnection.set("scm:git:ssh://github.com:PalabraAI/palabra-ai-java.git")
                    url.set("https://github.com/PalabraAI/palabra-ai-java")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "OSSRH"
            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            
            credentials {
                username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

// Signing configuration (required for Maven Central)
signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}

// Only sign if publishing to Maven Central
tasks.withType<Sign>().configureEach {
    onlyIf { gradle.taskGraph.hasTask("publish") }
}

// JaCoCo configuration
jacoco {
    toolVersion = "0.8.8"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}
