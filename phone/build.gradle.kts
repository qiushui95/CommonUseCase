plugins {
    id("someone.code.library")
}

android {
    namespace = "com.usecase.phone"
}

dependencies {

    compileOnly(libs.androidUtils)
}
