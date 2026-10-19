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
        // Não é com.mauricio.libertyx.folder: em 2026-10-04 o Google Play
        // Protect marcou aquele nome de pacote como nocivo ("pode instalar
        // outros apps") — toda versão, qualquer chave —, e o mesmo código
        // com outro nome passava. O código continua no pacote Kotlin
        // com.mauricio.libertyx.folder; só a identidade do app mudou.
        applicationId = "com.mauricio.libertyx.files"
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
    signingConfigs {
        create("arquivos") {
            storeFile = rootProject.file("keystore/arquivos.jks")
            storePassword = "arquivos123"
            keyAlias = "arquivos"
            keyPassword = "arquivos123"
        }
        // A chave dos APKs instalados à mão (pasta dist/), celular e TV —
        // registrada na Verificação de desenvolvedor Android do Play Console
        // para com.mauricio.libertyx.files. Sem o registro, desde 30/09/2026
        // o Android recusa APK instalado por outro app. A de cima (`arquivos`)
        // só assinava o pacote antigo, com.mauricio.libertyx.folder.
        create("tv") {
            storeFile = rootProject.file("keystore/folder-tv.jks")
            storePassword = "foldertv2026"
            keyAlias = "foldertv"
            keyPassword = "foldertv2026"
        }
        // A chave de ENVIO da Play Store, a mesma do LibertyX Player: não
        // assina o app que chega ao usuário (a Play reassina com a dela), só
        // prova ao Google que o envio veio do dono da conta. Mora fora do
        // repositório; a senha vem de ~/.gradle/gradle.properties. Sem ela,
        // só o `pacotesPlay` falha.
        val envio = project.findProperty("libertyxEnvioArquivo")?.toString()
        if (envio != null) create("envio") {
            storeFile = file(envio)
            storePassword = project.property("libertyxEnvioSenha").toString()
            keyAlias = project.property("libertyxEnvioApelido").toString()
            keyPassword = project.property("libertyxEnvioSenha").toString()
        }
    }

    flavorDimensions += listOf("aparelho", "loja")
    //
    // Os dois vão para a MESMA página da Play (mesmo pacote, mesma compra): a
    // loja entrega a cada aparelho o pacote compatível de número mais alto. O
    // de TV exige `leanback`, então celular só recebe o de celular; numa TV os
    // dois servem, e vence o de TV porque termina em 2 e o de celular em 1 —
    // desde que os dois saiam do MESMO `pacotesPlay` (mesmo `minutos`).
    //
    // O que muda entre os dois, além da tela de entrada, são três chaves:
    // - PRO: a TV tem 7 dias grátis e a rede como compra única; no celular é
    //   tudo grátis (decisão do dono do app).
    // - PASTA_SEGURA: não existe em nenhum dos dois na loja; quem criou uma
    //   antes ainda acha em Configurações como retirar os arquivos.
    // - GUIA: o guia de uso da primeira abertura, feito para o controle remoto.
    productFlavors {
        create("celular") {
            dimension = "aparelho"
            buildConfigField("boolean", "PRO", "false")
            buildConfigField("boolean", "PASTA_SEGURA", "false")
            buildConfigField("boolean", "GUIA", "false")
            versionCode = (minutos * 10 + 1).toInt()
            versionName = "1.0.1"
            signingConfig = signingConfigs.getByName("tv")
        }
        create("tv") {
            dimension = "aparelho"
            buildConfigField("boolean", "PRO", "true")
            buildConfigField("boolean", "PASTA_SEGURA", "false")
            buildConfigField("boolean", "GUIA", "true")
            // Termina em 2 e o celular em 1, como no LibertyX Player: os
            // números nunca colidem (a Play recusa números repetidos) e, numa
            // TV, o de TV vence (ver o comentário de `productFlavors`).
            versionCode = (minutos * 10 + 2).toInt()
            versionName = "1.0.1"
            signingConfig = signingConfigs.getByName("tv")
        }

        // A segunda dimensão: de qual loja o app sai. Só a cobrança muda —
        // a Google Play Billing não existe no Fire TV, que não tem Play; lá a
        // compra do Pro é pela Amazon Appstore (`src/tvAmazon`). O celular é
        // grátis nas duas lojas (no tablet Fire também), então para ele as duas
        // variantes são o mesmo app.
        create("play") { dimension = "loja" }
        create("amazon") { dimension = "loja" }
    }

    buildTypes {
        release {
            // A chave vem da variante (celular ou tv), não do tipo de build.
            signingConfig = null
            // R8 ligado: sem ele o pacote passava de 18 MB, quase tudo ícone
            // do Material que o app não usa e criptografia que o smbj não
            // chama. As regras do que não pode encolher estão em proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            signingConfig = null
        }
        // O que vai para as lojas: `./gradlew pacotesPlay` e
        // `./gradlew pacotesAmazon`. Um tipo de
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

// Na Amazon a assinatura não é a de envio da Play (que só serve para provar ao
// Google quem enviou): é a mesma dos APKs instalados à mão.
androidComponents {
    onVariants(selector().withBuildType("loja")) { v ->
        if (v.productFlavors.any { it.second == "amazon" }) v.signingConfig.setConfig(android.signingConfigs.getByName("tv"))
    }
}

/**
 * A chave pública que a Amazon gera para cada app (console da Amazon ›
 * o app › APK Files › "View public key"). Sem ela a cobrança da Amazon não
 * funciona — então o pacote da loja nem compila sem ela.
 */
val chaveAmazon = file("src/tvAmazon/assets/AppstoreAuthenticationKey.pem")
tasks.matching { it.name == "preTvAmazonLojaBuild" }.configureEach {
    doFirst {
        if (!chaveAmazon.exists()) throw GradleException(
            "Falta ${chaveAmazon.relativeTo(rootDir)}: baixe a chave pública do app no console da Amazon."
        )
    }
}

/** Os APKs da Amazon Appstore, em `dist/amazon/`: Fire TV (com o Pro) e tablet Fire (grátis). */
val pacotesAmazon = tasks.register<Copy>("pacotesAmazon") {
    dependsOn("assembleTvAmazonLoja", "assembleCelularAmazonLoja")
    val saida = layout.buildDirectory.dir("outputs/apk")
    from(saida.map { it.dir("tvAmazon/loja") }) { include("*.apk"); rename { "LibertyXFolder-FireTV.apk" } }
    from(saida.map { it.dir("celularAmazon/loja") }) { include("*.apk"); rename { "LibertyXFolder-FireTablet.apk" } }
    into(rootProject.layout.projectDirectory.dir("dist/amazon"))
    outputs.upToDateWhen { false }
}

/** Os pacotes da Play Store, em `dist/play/` — sobem os dois na mesma versão. */
val publicarPacotes = tasks.register<Copy>("publicarPacotes") {
    val saida = layout.buildDirectory.dir("outputs/bundle")
    from(saida.map { it.dir("celularPlayLoja") }) { include("*.aab"); rename { "LibertyXFolder-Celular.aab" } }
    from(saida.map { it.dir("tvPlayLoja") }) { include("*.aab"); rename { "LibertyXFolder-TV.aab" } }
    into(rootProject.layout.projectDirectory.dir("dist/play"))
    outputs.upToDateWhen { false }
}

/**
 * `./gradlew pacotesPlay`: celular e TV da Play no mesmo build. Não usar
 * `bundleLoja`, que gera também a variante Amazon (e para sem a chave dela).
 */
tasks.register("pacotesPlay") {
    dependsOn("bundleCelularPlayLoja", "bundleTvPlayLoja")
    finalizedBy(publicarPacotes)
}

val publicarApk = tasks.register<Copy>("publicarApk") {
    val saida = layout.buildDirectory.dir("outputs/apk")
    from(saida.map { it.dir("celularPlay/release") }) { include("*-release.apk"); rename { "LibertyXFolder-Celular.apk" } }
    from(saida.map { it.dir("tvPlay/release") }) { include("*-release.apk"); rename { "LibertyXFolder-TV.apk" } }
    into(rootProject.layout.projectDirectory.dir("dist"))
    outputs.upToDateWhen { false }
    doFirst { rootProject.layout.projectDirectory.dir("dist").asFile.listFiles { f -> f.name.endsWith(".apk") }?.forEach { it.delete() } }
}

tasks.whenTaskAdded {
    if (name == "assembleRelease") finalizedBy(publicarApk)
}

// Dependências de uma combinação de sabores (tv + loja) pedem a configuração
// declarada antes; as de um sabor só (`tvImplementation`) nascem sozinhas.
val tvPlayImplementation by configurations.creating
val tvAmazonImplementation by configurations.creating

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

    // Compra pela Google Play — só na TV da Play; o celular é grátis.
    tvPlayImplementation(libs.billing)
    // A cobrança 8.3.0 ainda puxa o fragment 1.1.0, de 2019, e a Play marca
    // essa versão como desatualizada no pacote. Declarar a atual aqui faz o
    // Gradle usar ela no lugar.
    tvPlayImplementation(libs.androidx.fragment)
    // Compra pela Amazon Appstore — só no Fire TV.
    tvAmazonImplementation(libs.amazon.appstore)

    implementation(libs.smbj)
    // Só para listar os compartilhamentos de um servidor (RPC srvsvc) — o
    // smbj conecta num share pelo nome, mas não sabe perguntar quais existem.
    implementation(libs.dcerpc) {
        exclude(group = "com.hierynomus", module = "smbj")
    }
}
