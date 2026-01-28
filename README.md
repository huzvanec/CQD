# CQD

Terminal video player

## Usage

### Requirements

- [JRE 21](https://adoptium.net/temurin/releases/?version=21)

### Run

```shell
$ java -jar ./CQD-VERSION.jar /path/to/video.mp4

Reading file...
Play video file? (Y/n): Y
```

## Development

### Requirements

- [Git](https://git-scm.com/install/)
- [JDK 17](https://adoptium.net/temurin/releases/?version=17)

### Build

```shell
./gradlew build
```

The output JAR is now located in `./build/libs`.