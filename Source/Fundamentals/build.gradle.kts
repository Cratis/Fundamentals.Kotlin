// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

plugins {
    kotlin("jvm")
    `java-library`
    id("com.vanniktech.maven.publish")
}

kotlin {
    compilerOptions {
        jvmDefault.set(JvmDefaultMode.ENABLE)
    }
}

dependencies {
    implementation("org.atteo:evo-inflector:2.0")
    testImplementation("tools.jackson.core:jackson-databind:3.2.4")
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.cratis", "fundamentals", version.toString())
    pom {
        name.set("Cratis.Fundamentals")
        description.set("Cratis Fundamentals for Kotlin and Java: concepts, correlation and .NET-compatible casing")
        url.set("https://github.com/Cratis/Fundamentals.Kotlin")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("cratis")
                name.set("Cratis")
                email.set("post@cratis.io")
            }
        }
        scm {
            url.set("https://github.com/Cratis/Fundamentals.Kotlin")
            connection.set("scm:git:git://github.com/Cratis/Fundamentals.Kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/Cratis/Fundamentals.Kotlin.git")
        }
    }
}
