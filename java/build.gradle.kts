plugins {
    id("java")
    id("com.gradleup.shadow") version "9.6.1"
    application
}

group = "com.github.username000101"
version = "1.0-SNAPSHOT"


application {
    mainClass.set("com.github.username000101.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-logging:commons-logging:1.3.0")
    implementation("com.github.zhkl0228:unidbg-android:0.9.9")
    implementation("com.github.zhkl0228:unidbg-unicorn2:0.9.9")
}

tasks.test {
    useJUnitPlatform()
}