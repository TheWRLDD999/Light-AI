
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
        versionCode = 2
        versionName = "1.1"

        ndk {
            abiFilters += listOf("armeabi-v7a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}
