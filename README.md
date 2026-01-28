# CQD

Terminal video player written in Kotlin.

![showcase](https://github.com/user-attachments/assets/517eb1e1-1dfc-4e4f-890d-3647694cb4d5)

## Usage

### Requirements

- [JRE 21](https://adoptium.net/temurin/releases/?version=21)
- Terminal with truecolor support

### Run

```shell
$ java -jar ./CQD-VERSION.jar /path/to/video.mp4

Reading file...
Play video file? (Y/n): Y
```

## Development

### Requirements

- [Git](https://git-scm.com/install/)
- [JDK 21](https://adoptium.net/temurin/releases/?version=17)

### Build

```shell
./gradlew build
```

The output JAR is now located in `./build/libs`.
