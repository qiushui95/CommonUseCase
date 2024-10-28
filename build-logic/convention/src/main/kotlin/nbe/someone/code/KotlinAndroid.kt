/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nbe.someone.code

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Configure base Kotlin with Android options
 */
internal fun configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    javaVersion: JavaVersion = JavaVersion.VERSION_21,
) {

    commonExtension.apply {
        compileSdk = 34


        defaultConfig {
            minSdk = 26
        }

        compileOptions {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
    }
}

internal fun Project.configureKotlin(jvmTarget: JvmTarget = JvmTarget.JVM_21) {
    kotlinOptions {
        compilerOptions {
            this.jvmTarget.set(jvmTarget)
        }
    }
}

internal fun Project.kotlinOptions(block: KotlinAndroidProjectExtension.() -> Unit) {
    (this as ExtensionAware).extensions.configure("kotlin", block)
}
