plugins {
    id("someone.code.library")
}

android {
    namespace = "com.usecase.app"
}

dependencies {

    implementation(libs.core.ktx)
    implementation(libs.androidUtils)
}
