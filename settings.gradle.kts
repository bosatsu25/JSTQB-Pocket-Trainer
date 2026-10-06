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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "JSTQB-Pocket-Trainer"
include(":app")
include(":core:model")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":feature:home")
include(":feature:quiz")
include(":feature:explanation")
include(":feature:result")
include(":feature:review")
include(":feature:analytics")
