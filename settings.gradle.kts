pluginManagement {
    repositories {
        google()
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

rootProject.name = "Cochilo"
include(":app")
// Cochilo on the watch: one tile (0.53.0, docs/wear.md).
include(":wear")
// Ask Cochilo's on-device model as a Play asset pack (docs/assistant.md).
include(":assistant_model")
