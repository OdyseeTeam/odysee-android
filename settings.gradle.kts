pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Odysee"
include(":app")
include(":core:common")
include(":core:designsystem")
include(":core:model")
include(":core:datastore")
include(":core:database")
include(":core:network")
include(":core:data")
include(":feature:home")
include(":feature:channel")
include(":feature:search")
include(":feature:wallet")
include(":feature:notifications")
include(":feature:settings")
include(":feature:library")
include(":feature:shorts")
