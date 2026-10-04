plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Minutos desde 2026-01-01, como no LibertyX: sempre cresce, venha o APK
// desta máquina ou da loja.
val minutos = ((System.currentTimeMillis() - 1_767_225_600_000L) / 60_000L).coerceAtLeast(1L)

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
        versionCode = minutos.toInt()
        versionName = "0.1.0"

        // O Pro já liberado, sem teste nem compra: é o APK do dono, o da pasta
        // `dist/`. Liberado por padrão no que se compila nesta máquina e
        // travado no que vai para a loja (o tipo `loja` força), como no
        // LibertyX Player: esquecer uma opção não pode fazer o dono cair no
        // próprio paywall. `-PlibertyxLoja` trava aqui também, para testar.
        val liberado = System.getenv("CI") == null && !project.hasProperty("libertyxLoja")
        buildConfigField("boolean", "LIBERADO", liberado.toString())
        // Só para testar o fim do teste sem esperar uma semana: `-PlibertyxDiasDeTeste=0`.
        buildConfigField("int", "DIAS_DE_TESTE", project.findProperty("libertyxDiasDeTeste")?.toString() ?: "7")
    }

    // Dois APKs do mesmo código, como no LibertyX Player: o de TV exige
    // `leanback` (não instala em celular) e aparece na tela inicial da TV; o de
    // celular não aparece na TV. O app não tem biblioteca nativa, então os dois
    // servem para qualquer arquitetura — ARM 32/64 do Fire Stick incluídos.
    flavorDimensions += "aparelho"
    //
    // O que muda entre os dois, além da tela de entrada, são três chaves:
    // - PRO: a TV vai para a Play com 7 dias grátis e a rede como compra única;
    //   o celular ainda não vai para a loja e segue sem nada disso.
    // - PASTA_SEGURA: não existe na TV (decisão do dono do app).
    // - GUIA: o guia de uso da primeira abertura, feito para o controle remoto.
    productFlavors {
        create("celular") {
            dimension = "aparelho"
            buildConfigField("boolean", "PRO", "false")
            buildConfigField("boolean", "PASTA_SEGURA", "true")
            buildConfigField("boolean", "GUIA", "false")
        }
        create("tv") {
            dimension = "aparelho"
            buildConfigField("boolean", "PRO", "true")
            buildConfigField("boolean", "PASTA_SEGURA", "false")
            buildConfigField("boolean", "GUIA", "true")
            // Termina em 2, como a TV do LibertyX Player: quando o celular for
            // para a mesma página da loja (terminando em 1), os números nunca
            // colidem — a Play recusa dois envios com o mesmo número.
            versionCode = (minutos * 10 + 2).toInt()
            versionName = "1.0.0"
        }
    }

    signingConfigs {
        create("arquivos") {
            storeFile = rootProject.file("keystore/arquivos.jks")
            storePassword = "arquivos123"
            keyAlias = "arquivos"
            keyPassword = "arquivos123"
        }
        // A chave de ENVIO da Play Store, a mesma do LibertyX Player: não
        // assina o app que chega ao usuário (a Play reassina com a dela), só
        // prova ao Google que o envio veio do dono da conta. Mora fora do
        // repositório; a senha vem de ~/.gradle/gradle.properties. Sem ela,
        // só o `bundleLoja` falha.
        val envio = project.findProperty("libertyxEnvioArquivo")?.toString()
        if (envio != null) create("envio") {
            storeFile = file(envio)
            storePassword = project.property("libertyxEnvioSenha").toString()
            keyAlias = project.property("libertyxEnvioApelido").toString()
            keyPassword = project.property("libertyxEnvioSenha").toString()
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("arquivos")
            // R8 ligado: sem ele o pacote passava de 18 MB, quase tudo ícone
            // do Material que o app não usa e criptografia que o smbj não
            // chama. As regras do que não pode encolher estão em proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            signingConfig = signingConfigs.getByName("arquivos")
        }
        // O que vai para a Play Store: `./gradlew bundleLoja`. Um tipo de
        // build, e não opções, porque as duas diferenças não podem ser
        // esquecidas no envio: a assinatura é a de envio e o Pro nunca vem
        // liberado.
        create("loja") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.findByName("envio")
            buildConfigField("boolean", "LIBERADO", "false")
            buildConfigField("int", "DIAS_DE_TESTE", "7")
            matchingFallbacks += "release"
        }
    }

    // O idioma segue o do aparelho (inglês, português ou espanhol; inglês para
    // os demais), e a partir do Android 13 dá para escolher um só para o app
    // em Configurações › Apps › Idioma. A lista sai das pastas values-xx.
    androidResources {
        generateLocaleConfig = true
    }

    // Na loja, todos os idiomas vão juntos: a Play entregaria só o do
    // aparelho, e quem escolhesse outro idioma para o app cairia no inglês.
    bundle {
        language { enableSplit = false }
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

// Só a TV vai para a loja por enquanto: a variante celular+loja nem existe.
androidComponents {
    beforeVariants { v ->
        if (v.buildType == "loja" && v.productFlavors.any { it.second == "celular" }) v.enable = false
    }
}

/** O pacote da Play Store, em `dist/play/`. */
val publicarPacotes = tasks.register<Copy>("publicarPacotes") {
    val saida = layout.buildDirectory.dir("outputs/bundle")
    from(saida.map { it.dir("tvLoja") }) { include("*.aab"); rename { "LibertyXFolder-TV.aab" } }
    into(rootProject.layout.projectDirectory.dir("dist/play"))
    outputs.upToDateWhen { false }
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
    if (name == "bundleLoja") finalizedBy(publicarPacotes)
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

    // Compra pela Google Play — só na TV, a única que vai para a loja.
    "tvImplementation"(libs.billing)

    implementation(libs.smbj)
    // Só para listar os compartilhamentos de um servidor (RPC srvsvc) — o
    // smbj conecta num share pelo nome, mas não sabe perguntar quais existem.
    implementation(libs.dcerpc) {
        exclude(group = "com.hierynomus", module = "smbj")
    }
}
