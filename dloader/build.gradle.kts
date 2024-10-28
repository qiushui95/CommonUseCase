plugins {
    id("someone.code.library")
}

android {
    namespace = "com.usecase.dloader"
}

dependencies {
    implementation(libs.okhttp.core)

    implementation(libs.coroutines.core)

    implementation(libs.androidUtils)
}
