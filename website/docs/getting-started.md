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

- Read the architecture overview in this docs portal.
- Use JavaDoc at `/api` for full public API details.
