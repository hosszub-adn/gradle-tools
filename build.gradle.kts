plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    signing
    alias(libs.plugins.changelog)
    alias(libs.plugins.dokka)
}

group = "ru.mipt.npm"
version = "0.10.2-fixrelease-1"

description = "Build tools for DataForge and kscience projects"

changelog.version.set(project.version.toString())

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.kotlin.link")
}

java.targetCompatibility = JavaVersion.VERSION_11

dependencies {
    api(libs.kotlin.gradle)
    implementation(libs.atomicfu.gradle)
    implementation(libs.binary.compatibility.validator)
    implementation(libs.changelog.gradle)
    implementation(libs.dokka.gradle)
    implementation(libs.kotlin.jupyter.gradle)
    implementation(libs.kotlin.serialization)
}

gradlePlugin {
    plugins {
        create("common") {
            id = "ru.mipt.npm.gradle.common"
            description = "The generalized kscience plugin that works in conjunction with any kotlin plugin"
            implementationClass = "ru.mipt.npm.gradle.KScienceCommonPlugin"
        }

        create("project") {
            id = "ru.mipt.npm.gradle.project"
            description = "The root plugin for multimodule project infrastructure"
            implementationClass = "ru.mipt.npm.gradle.KScienceProjectPlugin"
        }

        create("mpp") {
            id = "ru.mipt.npm.gradle.mpp"
            description = "Pre-configured multiplatform project"
            implementationClass = "ru.mipt.npm.gradle.KScienceMPPlugin"
        }

        create("jvm") {
            id = "ru.mipt.npm.gradle.jvm"
            description = "Pre-configured JVM project"
            implementationClass = "ru.mipt.npm.gradle.KScienceJVMPlugin"
        }

        create("js") {
            id = "ru.mipt.npm.gradle.js"
            description = "Pre-configured JS project"
            implementationClass = "ru.mipt.npm.gradle.KScienceJSPlugin"
        }

        create("native") {
            id = "ru.mipt.npm.gradle.native"
            description = "Additional native targets to be use alongside mpp"
            implementationClass = "ru.mipt.npm.gradle.KScienceNativePlugin"
        }

        create("node") {
            id = "ru.mipt.npm.gradle.node"
            description = "Additional nodejs target to be use alongside mpp"
            implementationClass = "ru.mipt.npm.gradle.KScienceNodePlugin"
        }
    }
}

afterEvaluate {
    publishing {
        val vcs = "https://github.com/mipt-npm/gradle-tools"

        val sourcesJar by tasks.creating(Jar::class) {
            archiveClassifier.set("sources")
            from(sourceSets.named("main").get().allSource)
        }

        val javadocsJar by tasks.creating(Jar::class) {
            group = "documentation"
            archiveClassifier.set("javadoc")
            from(tasks.dokkaHtml)
        }

        // Process each publication we have in this project
        publications.filterIsInstance<MavenPublication>().forEach { publication ->
            publication.apply {
                artifact(sourcesJar)
                artifact(javadocsJar)

                pom {
                    name.set(project.name)
                    description.set(project.description)
                    url.set(vcs)

                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set("MIPT-NPM")
                            name.set("MIPT nuclear physics methods laboratory")
                            organization.set("MIPT")
                            organizationUrl.set("https://npm.mipt.ru")
                        }
                    }

                    scm {
                        url.set(vcs)
                        tag.set(project.version.toString())
                    }
                }
            }
        }

        val spaceRepo = "https://maven.pkg.jetbrains.space/mipt-npm/p/mipt-npm/maven"
        val spaceUser: String? = project.findProperty("publishing.space.user") as? String
        val spaceToken: String? = project.findProperty("publishing.space.token") as? String

        if (spaceUser != null && spaceToken != null) {
            project.logger.info("Adding mipt-npm Space publishing to project [${project.name}]")

            repositories.maven {
                name = "space"
                url = uri(spaceRepo)

                credentials {
                    username = spaceUser
                    password = spaceToken
                }
            }
        }

        val sonatypeUser: String? = project.findProperty("publishing.sonatype.user") as? String
        val sonatypePassword: String? = project.findProperty("publishing.sonatype.password") as? String

        if (sonatypeUser != null && sonatypePassword != null) {
            val sonatypeRepo: String = if (project.version.toString().contains("dev")) {
                "https://oss.sonatype.org/content/repositories/snapshots"
            } else {
                "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            }

            if (plugins.findPlugin("signing") == null) {
                plugins.apply("signing")
            }

            repositories.maven {
                name = "sonatype"
                url = uri(sonatypeRepo)

                credentials {
                    username = sonatypeUser
                    password = sonatypePassword
                }
            }

            signing {
                //useGpgCmd()
                sign(publications)
            }
        }
    }
}
