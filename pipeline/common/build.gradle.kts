plugins {
    jacoco
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt").version("1.8.0")
    id("com.github.johnrengelman.shadow").version("5.2.0")
}

jacoco {
    toolVersion = "0.8.5"
    reportsDir = file("$buildDir/jacoco")
}

dependencies {
    // Yaml/JSON deserialization
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.9.4")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.10.2")
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.9.4")

    // Logging
    implementation("io.github.microutils", "kotlin-logging", "1.7.9")
    implementation("org.slf4j", "slf4j-simple", "1.7.28")

    // ZeroMQ
    implementation("org.zeromq", "jeromq", "0.5.2")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.3.5")
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = false
        csv.isEnabled = false
        html.destination = file("${buildDir}/jacocoHtml")
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "BRANCH"
                minimum = "0.8".toBigDecimal()
            }

            limit {
                counter = "LINE"
                minimum = "0.8".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.build {
    dependsOn(tasks.jacocoTestReport)
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    destinationDir = File("./");
}