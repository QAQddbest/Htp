plugins {
    kotlin("jvm") version "1.4.10"
    application
}

group = "dd.oliver"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
//    implementation(kotlin("reflect"))
    implementation("org.slf4j:slf4j-simple:1.7.30")
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("io.netty:netty-all:4.1.52.Final")
    implementation("com.github.ajalt.clikt:clikt:3.0.1")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.2.3") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.2.3") // for kotest core jvm assertions
    testImplementation("io.kotest:kotest-property-jvm:4.2.3") // for kotest property test
}

application {
    mainClassName = "dd.oliver.htp.EntryKt"
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
