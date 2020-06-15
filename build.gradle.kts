import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    kotlin("jvm") version "1.3.71"
}

group = "nl.tudelft.hyperion"
version = "0.1.0"

allprojects {
    apply(plugin = "kotlin")

    repositories {
        mavenCentral()
        jcenter()
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    tasks.test {
        useJUnitPlatform()
        failFast = true
        reports.html.isEnabled = false
        reports.junitXml.isEnabled = false
    }
}

subprojects {
    apply(plugin = "kotlin")

    sourceSets {
        create("integrationTest") {
            compileClasspath += sourceSets.main.get().output
            compileClasspath += project.files("src/integrationTest/")
            runtimeClasspath += sourceSets.main.get().output
            runtimeClasspath += project.files("src/integrationTest/")
        }
    }

    configurations["integrationTestImplementation"].extendsFrom(configurations.testImplementation.get())
    configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

    val integrationTest = task<Test>("integrationTest") {
        description = "Runs integration tests"
        group = "verification"

        useJUnitPlatform()

        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        shouldRunAfter("test")
    }

    tasks.check {
        dependsOn(integrationTest)
    }
}

tasks.register<DefaultTask>("build-artifacts-release") {
    group = "build"
    description = "Produces all necessary artifacts for a GitHub Release"

    // Ensure that shadow outputs to one directory.
    allprojects {
        try {
            tasks.getByName<AbstractArchiveTask>("shadowJar") {
                destinationDirectory.set(File(project.rootDir.absolutePath + "/release-artifacts/"))
            }
        } catch (ex: UnknownDomainObjectException) {

        }
    }

    // build all artifacts with shadowJar
    dependsOn(":pluginmanager:shadowJar")
    dependsOn(":datasource:plugins:elasticsearch:shadowJar")
    dependsOn(":aggregator:shadowJar")

    dependsOn(":pipeline:plugins:adder:shadowJar")
    dependsOn(":pipeline:plugins:extractor:shadowJar")
    dependsOn(":pipeline:plugins:loadbalancer:shadowJar")
    dependsOn(":pipeline:plugins:pathextractor:shadowJar")
    dependsOn(":pipeline:plugins:printer:shadowJar")
    dependsOn(":pipeline:plugins:rate:shadowJar")
    dependsOn(":pipeline:plugins:reader:shadowJar")
    dependsOn(":pipeline:plugins:renamer:shadowJar")
    dependsOn(":pipeline:plugins:stresser:shadowJar")
    dependsOn(":pipeline:plugins:versiontracker:shadowJar")
}
