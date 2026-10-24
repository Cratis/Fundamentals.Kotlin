// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

plugins {
    kotlin("jvm")
    `java-library`
    id("com.vanniktech.maven.publish")
}

val jacksonVersion = "3.2.2"

kotlin {
    compilerOptions {
        jvmDefault.set(JvmDefaultMode.ENABLE)
    }
}

dependencies {
    api(project(":Source:Fundamentals"))
    api("tools.jackson.core:jackson-databind:$jacksonVersion")
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.cratis", "fundamentals-jackson", version.toString())
    pom {
        name.set("Cratis.Fundamentals.Jackson")
        description.set("Jackson 3 module for Cratis Fundamentals concepts")
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
