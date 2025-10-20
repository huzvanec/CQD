plugins {
    kotlin("jvm") version "2.2.10"
}

group = "cz.jeme.cqd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks {
    test {
        useJUnitPlatform()
    }

    jar {
        manifest {
            attributes["Main-Class"] = "cz.jeme.cqd.MainKt"
        }
    }
}