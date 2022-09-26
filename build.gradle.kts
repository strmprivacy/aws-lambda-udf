import org.ajoberstar.grgit.Grgit
import java.util.*
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-library")
    id("org.ajoberstar.grgit")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

val branch = System.getenv("CI_COMMIT_REF_NAME") ?: Grgit.open(mapOf("dir" to project.file("."))).branch.current().name
val tag = System.getenv("CI_COMMIT_TAG")
ext["tag"] = tag

rootProject.version = if (tag != null || branch == "master") project.version else "${project.version}-SNAPSHOT"

val scmPath by extra { "strmprivacy/libraries/aws-lambda-decrypter" }

buildscript {
    tasks.named<Wrapper>("wrapper") {
        gradleVersion = "7.3.2"
        distributionType = Wrapper.DistributionType.ALL
    }
}

tasks.withType(GradleBuild::class) {
    // To prevent clashing with directory names and project names
    buildName = project.name
}

nexusPublishing {
    packageGroup.set("io.strmprivacy")

    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(base64Decode("sonatypeUsername"))
            password.set(base64Decode("sonatypePassword"))
        }
    }
}

allprojects {
    version = rootProject.version

    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jlleitschuh.gradle.ktlint")
    }

    ktlint {
        filter {
            exclude { entry ->
                entry.file.toString().contains("generated")
            }
        }
    }

    buildscript {
        repositories {
            mavenLocal()
            mavenCentral()
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }

    java.sourceCompatibility = JavaVersion.VERSION_11
    java.targetCompatibility = JavaVersion.VERSION_11

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Jar> {
        onlyIf { sourceSets["main"].allSource.files.isNotEmpty() }
    }
}
fun base64Decode(prop: String): String? {
    return project.findProperty(prop)?.let {
        String(Base64.getDecoder().decode(it.toString())).trim()
    }
}
