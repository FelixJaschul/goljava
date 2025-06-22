plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.libsdl4j:libsdl4j:2.28.4-1.6")
}

tasks.test {
    useJUnitPlatform()
}