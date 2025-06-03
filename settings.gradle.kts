enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
rootProject.name = "CommonUseCase"

include(":launcher")


include(":app")
//include(":app_file")
include(":dloader")
include(":oss_upload")
include(":file")
include(":phone")
include(":service")
include(":google")
