/*
 * Copyright 2022 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import com.android.build.gradle.LibraryExtension
import nbe.someone.code.configSamePlugins
import nbe.someone.code.configSpotless
import nbe.someone.code.configureKotlin
import nbe.someone.code.configureKotlinAndroid
import nbe.someone.code.kotlinOptions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.internal.DefaultPublishingExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import java.net.URI

@Suppress("unused")
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configSamePlugins()

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            with(pluginManager) {
                apply(libs.findPlugin("library").get().get().pluginId)
                apply(libs.findPlugin("maven").get().get().pluginId)
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.consumerProguardFiles("consumer-rules.pro")
                defaultConfig.targetSdk = 34

                kotlinOptions {
                    compilerOptions {
                        freeCompilerArgs.add("-Xexplicit-api=strict")
                    }
                }
            }

            extensions.configure<PublishingExtension> {
                repositories {
                    maven {
                        name = "Hero"
                        url = URI.create(System.getenv("HERO_NEXUS_URL"))
                        credentials {
                            username = System.getenv("HERO_NEXUS_USERNAME")
                            password = System.getenv("HERO_NEXUS_PASSWORD")
                        }
                    }
                }
            }

            configSpotless()

            configureKotlin()
        }
    }
}