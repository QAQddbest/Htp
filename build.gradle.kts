plugins {
    kotlin("jvm") version "1.4.10"
}

group = "dd.oliver"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
//    implementation(kotlin("reflect"))
    implementation("io.netty:netty-all:4.1.52.Final")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.2.3") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.2.3") // for kotest core jvm assertions
    testImplementation("io.kotest:kotest-property-jvm:4.2.3") // for kotest property test
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
