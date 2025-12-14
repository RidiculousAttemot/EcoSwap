import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.ecoswap"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ecoswap"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Load Supabase credentials from local.properties
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        
        val storageBucket = properties.getProperty("SUPABASE_STORAGE_BUCKET", "ecoswap-images")
        val listingsBucket = properties.getProperty("SUPABASE_LISTINGS_BUCKET", "listing-photos")
        val proofsBucket = properties.getProperty("SUPABASE_PROOFS_BUCKET", "trade-proofs")
        val communityBucket = properties.getProperty("SUPABASE_COMMUNITY_BUCKET", "community-photos")

        buildConfigField("String", "SUPABASE_URL", "\"${properties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${properties.getProperty("SUPABASE_ANON_KEY", "")}\"")
        buildConfigField("String", "SUPABASE_STORAGE_BUCKET", "\"$storageBucket\"")
        buildConfigField("String", "SUPABASE_LISTINGS_BUCKET", "\"$listingsBucket\"")
        buildConfigField("String", "SUPABASE_PROOFS_BUCKET", "\"$proofsBucket\"")
        buildConfigField("String", "SUPABASE_COMMUNITY_BUCKET", "\"$communityBucket\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // HTTP client for REST API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Location services
    implementation("com.google.android.gms:play-services-location:21.3.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}