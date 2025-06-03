plugins {
    id("someone.code.library")
}

android {
    namespace = "com.usecase.app"
}

dependencies {

    compileOnly(libs.core.ktx)
    compileOnly(libs.androidUtils)
}
