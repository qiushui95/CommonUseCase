plugins {
    id("someone.code.library")
}

android {
    namespace = "com.usecase.app_file"
}

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.androidUtils)
}
