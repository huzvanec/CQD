plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
}

application {
    mainClass = "cz.jeme.cqd.MainKt"
}

group = "cz.jeme.cqd"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.ffmpeg)
    implementation(libs.javacv)
    implementation(libs.jline)
}

kotlin {
    jvmToolchain(21)
}

tasks {
    test {
        useJUnitPlatform()
    }

    build {
        dependsOn(shadowJar)
    }
    
    shadowJar {
        archiveClassifier = ""
    }

    jar {
        manifest {
            attributes["Main-Class"] = application.mainClass
        }
    }
}