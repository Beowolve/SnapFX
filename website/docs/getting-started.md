# Getting Started

SnapFX is currently in a public-preview release-readiness phase (`0.x`).
Until Maven Central publication is live, use source checkout or GitHub Release demo assets.

## Run Tests

```bash
./gradlew test
```

## Run Demo

```bash
./gradlew run
```

## Quick Localization Setup

```java
SnapFX snapFX = new SnapFX();
snapFX.setLocale(Locale.GERMAN);

// Optional: provide your own language pack
snapFX.setLocalizationProvider(
    new DockResourceBundleLocalizationProvider("com.example.i18n.snapfx")
);
```

See the full guide at [Localization](/localization).

## Build Demo Runtime Image (`jlink`)

```bash
./gradlew :snapfx-demo:jlink
```

## Build Demo Package Image (`jpackage`)

```bash
./gradlew :snapfx-demo:jpackageImage
./gradlew :snapfx-demo:packageJPackageImageZip
```

## Next

- Continue with the [User Guide](/user-guide).
- Follow [Tutorial: First Layout](/tutorial-first-layout) for a complete start-to-finish flow.
- Browse [Examples](/examples) for reusable snippets.
- Use [API JavaDoc](https://snapfx.org/api/) for full public API details.
