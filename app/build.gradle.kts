import java.util.Properties

plugins {
    id("com.android.application")
}

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { load(it) }
    }
}
val bundledApiKey = localProperties.getProperty("aknsporApiKey", "")
val generatedWebAssets = layout.buildDirectory.dir("generated/aknsporAssets")
val prepareWebAssets = tasks.register("prepareWebAssets") {
    val sourceAssets = file("src/main/assets/www")
    inputs.dir(sourceAssets)
    inputs.property("bundledApiKey", bundledApiKey)
    outputs.dir(generatedWebAssets)
    doLast {
        val outputDirectory = generatedWebAssets.get().asFile
        if (outputDirectory.exists()) outputDirectory.deleteRecursively()
        val webAssets = outputDirectory.resolve("www")
        sourceAssets.copyRecursively(webAssets, overwrite = true)
        val htmlFile = webAssets.resolve("index.html")
        val html = htmlFile.readText()
        htmlFile.writeText(
            html.replace("__AKNSPOR_API_CONFIGURED__", bundledApiKey.isNotBlank().toString()),
            Charsets.UTF_8
        )
    }
}

android {
    namespace = "com.aknozgn.aknspor"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.aknozgn.aknspor"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "API_SPORTS_KEY",
            groovy.json.JsonOutput.toJson(bundledApiKey)
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets.getByName("main").assets.setSrcDirs(listOf(generatedWebAssets))
}

tasks.named("preBuild").configure {
    dependsOn(prepareWebAssets)
}

dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
