package nbe.someone.code

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal fun Project.configSpotless() {

    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    extensions.configure(SpotlessExtension::class.java) {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**/*.kt")
            endWithNewline()
            leadingTabsToSpaces()
            trimTrailingWhitespace()
            ktlint(libs.findVersion("ktlint").get().requiredVersion)
        }
        format("kts") {
            target("**/*.kts")
            targetExclude("**/build/**/*.kts")
        }
        format("xml") {
            target("**/*.xml")
            targetExclude("**/build/**/*.xml")
        }
    }
}
