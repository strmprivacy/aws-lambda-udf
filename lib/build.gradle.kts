import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import java.util.Base64

val scmPath: String by rootProject.extra

description = "STRM Privacy AWS Redshift UDF"

plugins {
    id("maven-publish")
    id("com.github.johnrengelman.shadow")
    id("signing")
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    testLogging {

        // set options for log level LIFECYCLE
        events(FAILED)

        // set options for log level DEBUG
        debug {
            events(STARTED, SKIPPED, FAILED)
        }

        info.events = setOf(FAILED, SKIPPED)
    }
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.amazonaws:aws-lambda-java-core:1.1.0")
    implementation("org.apache.maven.plugins:maven-shade-plugin:3.2.2")
    implementation("com.google.crypto.tink:tink:1.7.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.0")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

val sourcesJar = tasks.register("sourcesJar", Jar::class) {
    from(sourceSets["main"].allSource)
    archiveClassifier.set("sources")
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar)

            groupId = "io.strmprivacy.aws-lambda-udfs"
            artifactId = "decrypter"
            version = project.version.toString()

            pom {
                description.set(project.description)
                url.set("https://strmprivacy.io")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        name.set("Stream Machine B.V.")
                        email.set("apis@strmprivacy.io")
                        organization.set("Stream Machine B.V.")
                        organizationUrl.set("https://strmprivacy.io")
                    }
                }

                scm {
                    url.set("https://github.com/$scmPath")
                    connection.set("scm:git:git@github.com:$scmPath.git")
                    developerConnection.set("scm:git:git@github.com:$scmPath.git")
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(base64Decode("gpgPrivateKey"), base64Decode("gpgPassphrase"))
    sign(*publishing.publications.toTypedArray())
}

tasks.withType<Sign>().configureEach {
    onlyIf { rootProject.extra["tag"] != null }
}

tasks.findByName("publish")?.dependsOn("build")

fun base64Decode(prop: String): String? {
    return project.findProperty(prop)?.let {
        String(Base64.getDecoder().decode(it.toString())).trim()
    }
}
val shadowJar = tasks.withType<ShadowJar> {
    isZip64 = true
    mergeServiceFiles()

    manifest {
        attributes["Implementation-Title"] = description
        attributes["Implementation-Version"] = archiveVersion
    }
}
tasks.getByName("build").dependsOn(shadowJar)
