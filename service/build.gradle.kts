plugins {
    id("someone.code.library")
}

android {
    namespace = "com.usecase.service"
}

dependencies {

    implementation(libs.coroutines.core)
    implementation(libs.lifecycle.service)
}
