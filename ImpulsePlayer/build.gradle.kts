plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "io.getimpulse.player"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.getimpulse"
            artifactId = "impulse_player_android"
            version = "0.3.1"

            afterEvaluate {
                from(components["release"])
            }

            // Additional metadata for the POM
            pom {
                name.set("Impulse Player")
                description.set("The Impulse Player makes using a video player in Android easy.")
                url.set("https://github.com/getimpulse/impulse_player_android")

//                licenses {
//                    license {
//                        name.set("The Apache License, Version 2.0")
//                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
//                    }
//                }
//
//                developers {
//                    developer {
//                        id.set("username")
//                        name.set("Your Name")
//                        email.set("your.email@example.com")
//                    }
//                }
//
//                scm {
//                    connection.set("scm:git:git://github.com/username/mylibrary.git")
//                    developerConnection.set("scm:git:ssh://github.com:username/mylibrary.git")
//                    url.set("https://github.com/username/mylibrary")
//                }
            }
        }
    }

    repositories {
        mavenLocal()
//        maven {
//            url = uri("https://maven.example.com/repository/maven-releases/")
//            credentials {
//                username = project.findProperty("maven.username") as String? ?: ""
//                password = project.findProperty("maven.password") as String? ?: ""
//            }
//        }
    }
}

dependencies {
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("com.google.android.gms:play-services-cast-framework:21.4.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}