plugins {
    java
    application
    // The OpenJFX plugin automatically pulls in platform-specific JavaFX native libs
    // (Windows, Mac, Linux) so you don't have to manage that yourself.
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "designlint"
version = "1.0.0"

// We're targeting Java 21 (LTS). This unlocks modern features like:
// records, sealed classes, pattern matching, text blocks, var, etc.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

// JavaFX module configuration
// We only need 'controls' (buttons, tables, etc.) and 'fxml' (declarative UI layouts).
// The plugin handles downloading the right platform-specific JARs.
javafx {
    version = "21.0.2"
    modules("javafx.controls", "javafx.fxml")
}

dependencies {
    // --- SootUp: our bytecode analysis engine ---
    // core: fundamental types like SootClass, SootMethod, Body, StmtGraph
    implementation("org.soot-oss:sootup.core:1.3.0")
    // java.core: Java-specific extensions (JavaView, JavaSootClass, etc.)
    implementation("org.soot-oss:sootup.java.core:1.3.0")
    // java.bytecode: the frontend that reads .class files and converts to Jimple
    implementation("org.soot-oss:sootup.java.bytecode:1.3.0")

    // --- Logging (SootUp uses SLF4J internally) ---
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // --- Testing ---
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    // Entry point for `gradle run`
    mainClass.set("designlint.DesignLintApp")
}

tasks.test {
    useJUnitPlatform()
}

// Package the app as a fat JAR (all dependencies included) for easy distribution.
// Run with: java -jar designlint-1.0.0-all.jar
tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "designlint.DesignLintApp"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get())
}
