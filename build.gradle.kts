plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

group = "cz.jeme.cqd"
version = "1.0.0"

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

    shadowJar {
        enableAutoRelocation = true
        relocationPrefix = "cz.jeme.cqd.shaded"
        archiveClassifier = ""
    }
    
    assemble {
        dependsOn(shadowJar)
    }
}