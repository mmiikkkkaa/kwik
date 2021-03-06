@file:Suppress("UNUSED_VARIABLE")

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayPlugin
import kr.motd.gradle.sphinx.gradle.SphinxTask
import org.codehaus.plexus.util.Os
import java.util.Date

plugins {
    `maven-publish`
    id("org.jetbrains.kotlin.multiplatform") version "1.4.10"
    id("org.jetbrains.dokka") version "1.4.10"
    id("com.github.ben-manes.versions") version "0.33.0"
    id("io.gitlab.arturbosch.detekt") version "1.14.1"
    id("com.jfrog.bintray") version "1.8.5" apply false
    id("kr.motd.sphinx") version "2.9.0"
}

detekt {
    input = files(
        subprojects.flatMap { project ->
            listOf(
                "${project.projectDir}/src/commonMain/kotlin",
                "${project.projectDir}/src/jvmMain/kotlin"
            )
        }
    )
    buildUponDefaultConfig = true
    config = files("$rootDir/detekt-config.yml")
}

val currentVersion = rootDir.resolve("VERSION").readText().trim()

allprojects {
    group = "com.github.jcornaz.kwik"
    version = currentVersion

    repositories {
        mavenCentral()
        jcenter()
    }
}

// Hack so that we can configure all subprojects from this file
kotlin { jvm() }

subprojects {
    apply(plugin = "org.jetbrains.kotlin.multiplatform")
    apply(plugin = "org.jetbrains.dokka")
    apply<BintrayPlugin>()
    apply<MavenPublishPlugin>()
    apply<JacocoPlugin>()
    apply<JavaPlugin>()

    kotlin {
        jvm {
            compilations.all {
                kotlinOptions {
                    jvmTarget = "1.8"
                }
            }
        }
        linuxX64("linux")
        mingwX64("windows")

        @Suppress("SuspiciousCollectionReassignment")
        targets.all {
            compilations.all {
                kotlinOptions {
                    allWarningsAsErrors = findProperty("warningAsError") != null
                    freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
                }
            }
        }

        sourceSets {
            commonTest {
                dependencies {
                    api(kotlin("test-common"))
                    api(kotlin("test-annotations-common"))
                }
            }

            val jvmTest by existing {
                dependencies {
                    implementation(kotlin("test"))
                    implementation(kotlin("test-junit"))
                }
            }
        }
    }

    configure<JacocoPluginExtension> {
        toolVersion = "0.8.6"
    }

    publishing {
        publications.withType<MavenPublication>().apply {
            val metadata by getting {
                artifactId = "kwik-${project.name}-common"
            }

            val jvm by getting {
                artifactId = "kwik-${project.name}-jvm"
            }

            if (Os.isFamily(Os.FAMILY_UNIX)) {
                val linux by getting {
                    artifactId = "kwik-${project.name}-linux"
                }
            }

            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                val windows by getting {
                    artifactId = "kwik-${project.name}-windows"
                }
            }
        }
    }

    configure<BintrayExtension> {
        user = System.getenv("BINTRAY_USER")
        key = System.getenv("BINTRAY_KEY")
        publish = true

        with(pkg) {
            userOrg = "kwik"
            name = "kwik"
            repo = when {
                '-' in project.version.toString() -> "preview"
                else -> "stable"
            }

            setLicenses("Apache-2.0")

            vcsUrl = "https://github.com/jcornaz/kwik"
            githubRepo = "jcornaz/kwik"

            with(version) {
                name = project.version.toString()
                released = Date().toString()
                if ('+' !in project.version.toString()) {
                    vcsTag = project.version.toString()
                }
            }
        }

        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            setPublications("windows")
        } else {
            setPublications("metadata", "jvm", "linux")
        }
    }

    tasks {
        val jvmTest by existing {
            finalizedBy("jacocoTestReport")
        }

        val check by existing {
            dependsOn("publishToMavenLocal")
        }

        val jacocoTestReport by existing(JacocoReport::class) {
            dependsOn(jvmTest)

            classDirectories.setFrom(File("$buildDir/classes/kotlin/jvm/main").walkBottomUp().toSet())
            sourceDirectories.setFrom(files("src/commonMain/kotlin", "src/jvmMain/kotlin"))
            executionData.setFrom(files("${buildDir}/jacoco/jvmTest.exec"))

            reports {
                xml.isEnabled = true
                html.isEnabled = true
            }
        }
    }
}

tasks {
    val version by registering {
        group = "Help"
        description = "Prints version of kwik"

        doLast {
            println(project.version)
        }
    }

    val detekt by existing {
        description = "Performs static code analysis and report detected code smells"
    }

    val sphinx by existing(SphinxTask::class) {
        setWarningsAsErrors(true)

        setSourceDirectory("$rootDir/docs")
        inputs.file("$rootDir/README.rst")
        inputs.dir("$rootDir/example")

        outputs.cacheIf { true }
    }

    val testReport by registering(TestReport::class) {
        group = "Verification"
        description = "Create an aggregated report of all test tasks"

        destinationDir = file("$buildDir/reports/allTests")
        reportOn(subprojects.flatMap { it.tasks.withType(Test::class) })

        dependsOn.clear()
        mustRunAfter(subprojects.flatMap { it.tasks.withType(Test::class) })
    }

    val check by existing {
        dependsOn(sphinx)

        finalizedBy(testReport)
    }
}
