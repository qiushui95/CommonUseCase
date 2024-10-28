package nbe.someone.code

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal fun Project.configSamePlugins() {
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    with(pluginManager) {
        apply(libs.findPlugin("kotlin").get().get().pluginId)
        apply(libs.findPlugin("spotless").get().get().pluginId)
    }
}