import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")
val productionAdmobAppId = providers.gradleProperty("MYBET_ADMOB_APP_ID").orElse("").get()
val debugBackendUrl = providers.gradleProperty("MYBET_DEBUG_BACKEND_URL")
    .orElse("https://mybetlivebackend-production.up.railway.app").get().trimEnd('/')
require(debugBackendUrl.matches(Regex("https?://[^\\s\"\\\\]+"))) { "Invalid MYBET_DEBUG_BACKEND_URL" }
val signingPropertiesFile = rootProject.file(".local/signing/release-signing.properties")
val releaseSigning = Properties().apply {
    if (signingPropertiesFile.exists()) signingPropertiesFile.inputStream().use { load(it) }
}
fun signingValue(name: String): String = releaseSigning.getProperty(name)?.takeIf { it.isNotBlank() }
    ?: throw GradleException("Missing release signing property: $name")

android {
    namespace = "com.mybetlive"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.mybetlive"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "AUTH_BACKEND_URL", "\"https://mybetlivebackend-production.up.railway.app\"")
        buildConfigField("boolean", "DEBUG_BACKEND_OVERRIDE", "false")
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
    }
    signingConfigs {
        if (signingPropertiesFile.exists()) create("production") {
            storeFile = rootProject.file(signingValue("storeFile"))
            storePassword = signingValue("storePassword")
            keyAlias = signingValue("keyAlias")
            keyPassword = signingValue("keyPassword")
            storeType = "PKCS12"
        }
    }
    buildTypes {
        debug {
            // Backend override is limited to debug; release always uses production.
            buildConfigField("String", "AUTH_BACKEND_URL", "\"$debugBackendUrl\"")
            buildConfigField("boolean", "DEBUG_BACKEND_OVERRIDE", providers.gradleProperty("MYBET_DEBUG_BACKEND_URL").isPresent.toString())
        }
        release {
            isMinifyEnabled = false
            if (signingPropertiesFile.exists()) signingConfig = signingConfigs.getByName("production")
            manifestPlaceholders["admobAppId"] = productionAdmobAppId
            buildConfigField("String", "ADMOB_APP_ID", "\"$productionAdmobAppId\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED", "--add-opens=java.base/java.security=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED", "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED", "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED")
        }
    }
}
val validateProductionConfig by tasks.registering {
    doLast {
        check(productionAdmobAppId.matches(Regex("ca-app-pub-[0-9]{16}~[0-9]{10}")) &&
            !productionAdmobAppId.startsWith("ca-app-pub-3940256099942544~")) {
            "Release requires your own valid MYBET_ADMOB_APP_ID."
        }
        check(signingPropertiesFile.exists()) {
            "Release signing is missing. Run scripts/Prepare-ReleaseSigning.ps1 or restore your existing signing files."
        }
        check(rootProject.file(signingValue("storeFile")).isFile) { "Release keystore file is missing." }
    }
}
tasks.configureEach {
    if (name == "preReleaseBuild") dependsOn(validateProductionConfig)
}
dependencies {
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.09.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation(platform("androidx.compose:compose-bom:2025.09.01"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("com.android.billingclient:billing:9.1.0")
    implementation(platform("androidx.compose:compose-bom:2025.09.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-svg:3.3.0")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
}
