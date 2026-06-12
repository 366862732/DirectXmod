import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("fabric-loom") version "1.10.3"
    id("maven-publish")
    id("org.jetbrains.kotlin.jvm") version "2.0.20"
}

version = "0.1.0"
group = "com.dx12"

base {
    archivesName = "gl4dx12"
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("gl4dx12") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

repositories {
    maven {
        url = uri("https://maven.aliyun.com/repository/public")
    }
    maven {
        url = uri("https://maven.fabricmc.net/")
    }
    mavenCentral()
    flatDir {
        dirs("libs")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.3")
    mappings("net.fabricmc:yarn:1.21.3+build.1:v2")
    modImplementation("net.fabricmc:fabric-loader:0.16.10")
    
    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.0+kotlin.2.1.0")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
