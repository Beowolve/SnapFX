# SnapFX Setup Guide

## Prerequisites

### Java Installation

SnapFX requires Java 21 (LTS). Download it from:
- **Oracle JDK 21**: https://www.oracle.com/java/technologies/downloads/#java21
- **OpenJDK 21**: https://adoptium.net/temurin/releases/?version=21

After installation:

#### Windows
1. Set the `JAVA_HOME` environment variable:
   ```
   JAVA_HOME=C:\Program Files\Java\jdk-21
   ```
2. Add `%JAVA_HOME%\bin` to your PATH

#### Linux/Mac
```bash
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
```

## Build the project

### Using the Gradle Wrapper (recommended)

```bash
# Windows
.\gradlew build

# Linux/Mac
./gradlew build
```

### Run the demo

```bash
# Windows
.\gradlew run

# Linux/Mac
./gradlew run
```

### Run tests

```bash
# Windows
.\gradlew test

# Linux/Mac
./gradlew test
```

## IntelliJ IDEA Setup

1. **Open project**: `File` → `Open` → select the SnapFX folder
2. **Gradle sync**: IntelliJ will automatically run `gradle build`
3. **Configure JDK**: `File` → `Project Structure` → `Project` → set SDK to Java 21
4. **Run demo**: Right-click `MainDemo.java` → `Run 'MainDemo.main()'`

## Eclipse Setup

1. **Import project**: `File` → `Import` → `Gradle` → `Existing Gradle Project`
2. **Configure JDK**: Project → `Properties` → `Java Build Path` → Libraries → set JRE to Java 21
3. **Run demo**: Right-click `MainDemo.java` → `Run As` → `Java Application`

## VS Code Setup

Required extensions:
- Extension Pack for Java
- Gradle for Java

After installation:
1. Open the SnapFX folder
2. VS Code will detect the Gradle project automatically
3. Press `F5` to debug or use the "Run" button

## Gradle Tasks

```bash
# Compile project
.\gradlew compileJava

# Run tests (headless)
.\gradlew test --tests "*.model.*"

# Build JAR
.\gradlew jar

# Build distribution
.\gradlew distZip

# Show all tasks
.\gradlew tasks
```

## Troubleshooting

### "Cannot find JavaFX modules"
JavaFX is downloaded automatically by the Gradle plugin. Run:
```bash
.\gradlew clean build --refresh-dependencies
```

### "JAVA_HOME is not set"
Set the JAVA_HOME environment variable as described above.

### Tests fail (headless)
For GUI tests with TestFX:
```bash
.\gradlew test -Dtestfx.robot=glass -Dtestfx.headless=true
```

### Gradle wrapper missing
If `gradlew.bat` is missing:
```bash
gradle wrapper --gradle-version 8.5
```

## Development

### Project Structure

```
SnapFX/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/github/beowolve/snapfx/
│   │   │       ├── model/          # Data model
│   │   │       ├── view/           # View layer
│   │   │       ├── dnd/            # Drag & drop
│   │   │       ├── persistence/    # Serialization
│   │   │       ├── demo/           # Demo app
│   │   │       └── SnapFX.java     # Main entry point
│   │   └── resources/
│   │       └── snapfx.css          # Styling
│   └── test/
│       └── java/                   # JUnit tests
├── build.gradle.kts                # Build configuration
└── settings.gradle.kts
```

### Code Style

- **Java 21 features**: Records, pattern matching, text blocks
- **SOLID principles**: Strict separation of model/view
- **JavaDoc**: All public APIs documented
- **Tests**: Comprehensive JUnit 5 + TestFX tests

### Adding new features

1. **Model first**: Implement logic in the model layer
2. **Update view**: Adapt `DockLayoutEngine`
3. **Write tests**: Add tests
4. **Extend demo**: Showcase the feature in `MainDemo`

## License

This project is licensed under the MIT License.

SnapFX is intended for personal and commercial use, including large applications.
