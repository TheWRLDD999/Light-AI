
plugins {
    id("com.android.application")
}

android {
    namespace = "com.thewrldd999.lightai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.thewrldd999.lightaicheck"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters += listOf("armeabi-v7a")
        }
    }
}
