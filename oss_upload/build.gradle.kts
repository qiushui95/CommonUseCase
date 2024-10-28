plugins {
    id("someone.code.library")
}

android {
    namespace = "com.usecase.oss_upload"
}

dependencies {

    implementation(libs.coroutines.core)
    implementation(libs.drive.oss)
}
