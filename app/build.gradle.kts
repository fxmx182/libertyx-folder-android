plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mauricio.libertyx.folder"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mauricio.libertyx.folder"
        // 26 é onde nasce o `openProxyFileDescriptor`: é ele que deixa outro
        // app (player, visualizador de PDF) ler um arquivo do SMB aos pedaços,
        // com busca, sem baixar tudo antes.
        minSdk = 26
        targetSdk = 36
        // Minutos desde 2026-01-01, como no LibertyX: sempre cresce, venha o
        // APK desta máquina ou do CI.
        versionCode = (((System.currentTimeMillis() - 1_767_225_600_000L) / 60_000L)
            .coerceAtLeast(1L)).toInt()
        versionName = "0.1.0"
    }

    // Dois APKs do mesmo código, como no LibertyX Player: o de TV exige
    // `leanback` (não instala em celular) e aparece na tela inicial da TV; o de
    // celular não aparece na TV. O app não tem biblioteca nativa, então os dois
    // servem para qualquer arquitetura — ARM 32/64 do Fire Stick incluídos.
    flavorDimensions += "aparelho"
    productFlavors {
        create("celular") { dimension = "aparelho" }
        create("tv") { dimension = "aparelho" }
    }

    signingConfigs {
        create("arquivos") {
            storeFile = rootProject.file("keystore/arquivos.jks")
            storePassword = "arquivos123"
            keyAlias = "arquivos"
            keyPassword = "arquivos123"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("arquivos")
            isMinifyEnabled = false
        }
        debug {
            signingConfig = signingConfigs.getByName("arquivos")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/*.kotlin_module",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
            )
        }
    }
}

val publicarApk = tasks.register<Copy>("publicarApk") {
    val saida = layout.buildDirectory.dir("outputs/apk")
    from(saida.map { it.dir("celular/release") }) { include("*-release.apk"); rename { "LibertyXFolder-Celular.apk" } }
    from(saida.map { it.dir("tv/release") }) { include("*-release.apk"); rename { "LibertyXFolder-TV.apk" } }
    into(rootProject.layout.projectDirectory.dir("dist"))
    outputs.upToDateWhen { false }
    doFirst { rootProject.layout.projectDirectory.dir("dist").asFile.listFiles { f -> f.name.endsWith(".apk") }?.forEach { it.delete() } }
}

tasks.whenTaskAdded {
    if (name == "assembleRelease") finalizedBy(publicarApk)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.security.crypto)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // SMB de verdade: ler, gravar, renomear, apagar.
    testImplementation("junit:junit:4.13.2")

    implementation(libs.smbj)
    // Só para listar os compartilhamentos de um servidor (RPC srvsvc) — o
    // smbj conecta num share pelo nome, mas não sabe perguntar quais existem.
    implementation(libs.dcerpc) {
        exclude(group = "com.hierynomus", module = "smbj")
    }
}
