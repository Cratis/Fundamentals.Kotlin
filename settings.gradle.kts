// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        if (providers.gradleProperty("useMavenLocal").orNull?.toBoolean() == true) {
            mavenLocal()
        }
        mavenCentral()
    }
}

rootProject.name = "fundamentals-kotlin-workspace"

include("Source:Fundamentals")
include("Source:Jackson")
