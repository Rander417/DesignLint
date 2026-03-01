# DesignLint

A static analysis tool for Java bytecodes that checks design guideline compliance.  
Built with **SootUp** (bytecode analysis), **JavaFX** (UI), and **Gradle** (build system).

## Prerequisites

- **Java 21 JDK** — Download from [Adoptium](https://adoptium.net/) or install via your package manager
  - macOS: `brew install openjdk@21`
  - Windows: Download from Adoptium and add to PATH
  - Linux: `sudo apt install openjdk-21-jdk`
- **Gradle 8.x** — Installed automatically via the Gradle Wrapper (see below)

## Quick Start

### 1. Install the Gradle Wrapper

If you have Gradle installed globally, run this once from the project root:

```bash
gradle wrapper --gradle-version 8.5
```

If you don't have Gradle installed, download it from [gradle.org](https://gradle.org/install/)
or use your package manager (`brew install gradle`, `choco install gradle`, etc.).

After running the wrapper command, you'll have `gradlew` (Linux/Mac) and `gradlew.bat` (Windows)
scripts that download and use the correct Gradle version automatically.

### 2. Build the Project

```bash
./gradlew build
```

This downloads all dependencies (SootUp, JavaFX, etc.), compiles the code, and runs tests.

### 3. Run DesignLint

```bash
./gradlew run
```

### 4. Try It With Sample Classes

Compile the included sample classes, then point DesignLint at them:

```bash
# Compile sample classes to bytecode
mkdir -p samples/classes
javac -d samples/classes samples/src/*.java

# Run DesignLint and use the GUI to load samples/classes
./gradlew run
```

In the GUI:
1. Click **Load Classes...**
2. Navigate to the `samples/classes/samples` directory
3. All sample classes will appear in the class list
4. Ensure all three guidelines are checked
5. Click **Run Analysis**

You should see:
- `BadCloneable` → Cloneable Check violation
- `BadEqualsOnly` → equals/hashCode Check violation  
- `BadEqualsPattern` → equals() Pattern Check violations (missing null check, instanceof)
- `GoodClass` → all checks pass ✓

## Project Structure

```
designlint/
├── build.gradle.kts          # Build configuration and dependencies
├── settings.gradle.kts        # Project name
├── samples/                   # Sample .java files for testing
│   └── src/
│       ├── BadCloneable.java
│       ├── BadEqualsOnly.java
│       ├── BadEqualsPattern.java
│       └── GoodClass.java
└── src/main/java/designlint/
    ├── DesignLintApp.java     # Entry point (JavaFX Application)
    ├── core/
    │   ├── AnalysisResult.java      # Sealed interface: Pass | Violation
    │   ├── AnalysisEngine.java      # Orchestrates analysis runs
    │   ├── ClassLoadingService.java # Wraps SootUp class loading
    │   └── DesignGuideline.java     # Plugin interface for checks
    ├── guidelines/
    │   ├── CloneableCheck.java      # Check #1: warns on Cloneable
    │   ├── EqualsHashCodeCheck.java # Check #2: equals/hashCode pair
    │   └── EqualsPatternCheck.java  # Check #3: equals() body pattern
    └── ui/
        └── MainWindow.java          # JavaFX GUI
```

## Architecture

### Three Subsystems (modernized from the original design)

1. **Core** (`designlint.core`) — The framework: plugin interface, result types, 
   analysis orchestration, and SootUp integration. No UI dependencies.

2. **Guidelines** (`designlint.guidelines`) — Concrete check implementations.
   Each implements `DesignGuideline` and is self-contained.

3. **UI** (`designlint.ui`) — JavaFX-based GUI. Depends on Core but knows 
   nothing about specific guidelines.

### Key Design Decisions

- **Sealed interface for results** — `AnalysisResult` is either `Pass` or `Violation`. 
  The compiler guarantees exhaustive handling.
- **Records for data** — Immutable, concise, with auto-generated equals/hashCode/toString.
- **Strategy pattern for checks** — Each guideline is a pluggable strategy. 
  Adding a new check requires zero changes to existing code.
- **Engine/Service separation** — The analysis can run without a GUI (batch mode, testing).

## Adding a New Design Guideline

1. Create a new class in `designlint/guidelines/`:

```java
public class MyNewCheck implements DesignGuideline {
    @Override
    public String name() { return "My New Check"; }
    
    @Override
    public String description() { return "Checks for..."; }
    
    @Override
    public List<AnalysisResult> analyze(SootClass<?> sootClass, JavaView view) {
        // Your analysis logic here
    }
}
```

2. Register it in `DesignLintApp.createGuidelines()`:

```java
private List<DesignGuideline> createGuidelines() {
    return List.of(
        new CloneableCheck(),
        new EqualsHashCodeCheck(),
        new EqualsPatternCheck(),
        new MyNewCheck()          // ← add here
    );
}
```

## Modern Java Features Used

This project uses Java 17+ features throughout. If you're coming back to Java
after a long break, here's what's new:

| Feature | Where Used | Python Equivalent |
|---------|-----------|-------------------|
| `record` | `AnalysisResult.Pass`, `Violation` | `@dataclass(frozen=True)` |
| `sealed interface` | `AnalysisResult` | Abstract class with `__init_subclass__` restrictions |
| `var` (type inference) | Local variables | Python's default (no type needed) |
| Pattern matching (`instanceof X x`) | `EqualsPatternCheck` | `match/case` (Python 3.10+) |
| `List.of()`, `Map.of()` | Everywhere | `tuple()`, `frozenset()` |
| Streams + lambdas | `AnalysisEngine`, checks | List comprehensions |
| `Optional<T>` | `ClassLoadingService` | `Optional` or `None` checks |
| Text blocks (`"""`) | Not used yet | Triple-quoted strings |

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| SootUp Core | 1.3.0 | Bytecode analysis framework |
| SootUp Java Core | 1.3.0 | Java-specific SootUp extensions |
| SootUp Java Bytecode | 1.3.0 | .class file frontend |
| JavaFX Controls | 21.0.2 | UI widgets |
| JavaFX FXML | 21.0.2 | Declarative UI layouts |
| SLF4J + Logback | 2.0.9 / 1.4.14 | Logging |
| JUnit 5 | 5.10.1 | Testing |

## Troubleshooting

**"JavaFX runtime components are missing"**  
Make sure you're running with `./gradlew run` (not `java -jar`). The Gradle JavaFX
plugin adds the required module path arguments automatically.

**SootUp ClassCastException on getBody()**  
This is a known SootUp issue with certain bytecode patterns. The EqualsPatternCheck
has defensive error handling for this. If you hit it, try with different target classes.

**Classes not showing up after loading**  
Make sure you're pointing at a directory that contains `.class` files (not `.java`).
The files should be in a directory structure matching their package (e.g., 
`com/example/MyClass.class`).
