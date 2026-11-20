import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

fun String.toBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

fun String.isBetaGoogleServicesTask(): Boolean =
    startsWith("process") && endsWith("GoogleServices") && contains("Beta")

// Параметры beta-подписи: в CI приходят переменными окружения, локально их
// проще держать в local.properties — он в .gitignore, как и сам *.jks.
fun betaSigningProperty(name: String): String? =
    System.getenv(name)?.takeUnless { it.isBlank() }
        ?: localProperties.getProperty(name)?.takeUnless { it.isBlank() }

val betaKeystoreFile = betaSigningProperty("BETA_KEYSTORE_FILE")
val betaKeystorePassword = betaSigningProperty("BETA_KEYSTORE_PASSWORD")
val betaKeyAlias = betaSigningProperty("BETA_KEY_ALIAS")
val betaKeyPassword = betaSigningProperty("BETA_KEY_PASSWORD")
val hasBetaSigning = !betaKeystoreFile.isNullOrBlank() &&
    !betaKeystorePassword.isNullOrBlank() &&
    !betaKeyAlias.isNullOrBlank() &&
    !betaKeyPassword.isNullOrBlank()

android {
    namespace = "com.example.ysukampus"
    compileSdk = 36

    flavorDimensions += "environment"

    val baseUrl = localProperties.getProperty("BASE_URL") ?: ""
    // Адреса для отладочного переключателя серверов. Если в local.properties их
    // нет, оба падают обратно на BASE_URL, и переключатель просто не даёт выбора.
    val baseUrlTest = localProperties.getProperty("BASE_URL_TEST") ?: baseUrl
    val baseUrlProd = localProperties.getProperty("BASE_URL_PROD") ?: baseUrl
    val telegramBotLink = localProperties.getProperty("TELEGRAM_BOT_LINK")
        ?: "https://t.me/ysukampusbot"
    // У тестового и продового бэкендов разные Telegram-боты: код подтверждения,
    // выданный одним сервером, второй бот не знает. Если в local.properties
    // адреса нет, оба падают обратно на TELEGRAM_BOT_LINK.
    val telegramBotLinkTest = localProperties.getProperty("TELEGRAM_BOT_LINK_TEST")
        ?: telegramBotLink
    val telegramBotLinkProd = localProperties.getProperty("TELEGRAM_BOT_LINK_PROD")
        ?: telegramBotLink

    defaultConfig {
        applicationId = "com.example.ysukampus"
        minSdk = 26
        targetSdk = 36
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME") ?: "1.0"
        manifestPlaceholders["appName"] = "ysukampus"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", baseUrl.toBuildConfigString())
        buildConfigField("String", "API_BASE_URL", baseUrl.toBuildConfigString())
        buildConfigField("String", "BASE_URL_TEST", baseUrlTest.toBuildConfigString())
        buildConfigField("String", "BASE_URL_PROD", baseUrlProd.toBuildConfigString())
        // Переключатель серверов доступен только в отладочных сборках.
        buildConfigField("boolean", "SERVER_SWITCH_ENABLED", "false")
        // Лог HTTP в logcat. По умолчанию выключен: в теле ответов ходят личные
        // данные пользователя, а logcat читает любое приложение с adb.
        buildConfigField("boolean", "HTTP_LOG_ENABLED", "false")
        buildConfigField("String", "TELEGRAM_BOT_LINK", telegramBotLink.toBuildConfigString())
        buildConfigField(
            "String",
            "TELEGRAM_BOT_LINK_TEST",
            telegramBotLinkTest.toBuildConfigString()
        )
        buildConfigField(
            "String",
            "TELEGRAM_BOT_LINK_PROD",
            telegramBotLinkProd.toBuildConfigString()
        )
        buildConfigField("String", "AUTH_MODE", "\"telegram\"")
        buildConfigField("int", "DEV_USER_ID", "0")
        buildConfigField("String", "DEV_ROLE", "\"\"")
        buildConfigField("int", "DEV_ENTITY_ID", "0")
        buildConfigField("String", "DEV_FIRST_NAME", "\"\"")
    }

    signingConfigs {
        if (hasBetaSigning) {
            create("beta") {
                storeFile = file(betaKeystoreFile!!)
                storePassword = betaKeystorePassword
                keyAlias = betaKeyAlias
                keyPassword = betaKeyPassword
            }
        }
    }

    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "AUTH_MODE", "\"dev\"")
            // Свежий user_id: бэкенд создаёт пользователя только при первом входе,
            // поэтому у ранее заведённых dev-юзеров entity_id уже не переписать.
            buildConfigField("int", "DEV_USER_ID", "2")
            buildConfigField("String", "DEV_ROLE", "\"student\"")
            // Настоящий groupOid из справочника расписания (группа 1БС30).
            // С несуществующим oid ручки расписания отвечают 404, когда группа
            // берётся из профиля, а не из параметра group.
            buildConfigField("int", "DEV_ENTITY_ID", "10959")
            buildConfigField("String", "DEV_FIRST_NAME", "\"Dev User\"")
        }

        create("staging") {
            dimension = "environment"
            buildConfigField("String", "AUTH_MODE", "\"telegram\"")
        }
    }

    // Порядок блоков внутри buildTypes значим: initWith(getByName("release"))
    // выполняется в момент объявления типа, поэтому `release` должен быть
    // сконфигурирован ВЫШЕ тех, кто от него наследуется. Иначе копируются
    // пустые proguardFiles и правила молча не доезжают до R8.
    buildTypes {
        debug {
            manifestPlaceholders["appName"] = "ysukampus debug"
            buildConfigField("boolean", "SERVER_SWITCH_ENABLED", "true")
            buildConfigField("boolean", "HTTP_LOG_ENABLED", "true")
        }

        release {
            manifestPlaceholders["appName"] = "ysukampus"
            // R8: сборка дольше, APK втрое меньше, старт быстрее. Держим
            // включённым и здесь, чтобы betaRelease проверял ту же
            // конфигурацию, которая поедет в прод, а не соседнюю.
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // signingConfig намеренно не задан: прод-ключ заводится отдельно.
        }

        // Локальная отладочная сборка с тем же applicationId, что и у раздачи.
        // Тестерам НЕ уходит — CI собирает betaRelease.
        create("beta") {
            initWith(getByName("debug"))

            // SERVER_SWITCH_ENABLED и HTTP_LOG_ENABLED приезжают из debug через
            // initWith и остаются включёнными. Тестерам уходит betaRelease, а не
            // beta (см. комментарий выше), и переключатель там включён отдельно;
            // beta — локальная отладочная сборка, и выбор сервера ей нужен тем
            // более, что на тестовом бэкенде выключен /auth/bot/bind и
            // регистрация не доходит до конца. В отличие от debug, beta подписана
            // beta-ключом, а MIUI не даёт ставить по USB debug-подписанные APK.

            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            manifestPlaceholders["appName"] = "ysukampus beta"

            isDebuggable = true
            signingConfig = if (hasBetaSigning) {
                signingConfigs.getByName("beta")
            } else {
                signingConfigs.getByName("debug")
            }

            matchingFallbacks += listOf("debug")
        }

        // Сборка, которая уходит тестерам через App Distribution. Отличается от
        // будущего прода только applicationIdSuffix и ключом подписи: R8,
        // proguard-правила и isDebuggable здесь такие же, как в release.
        // Ставится обновлением поверх предыдущей раздачи.
        //
        // versionCode при локальной сборке надо задавать выше установленного,
        // иначе Android откажется ставить: VERSION_CODE=24 ./gradlew ...
        create("betaRelease") {
            initWith(getByName("release"))

            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            manifestPlaceholders["appName"] = "ysukampus beta"
            // Переключатель серверов оставлен включённым: тестовый бэкенд и
            // прод — разные серверы с разными Telegram-ботами, и без выбора
            // сборка заперта в том, что пришло в BASE_URL. На тестовом сейчас
            // выключен /auth/bot/bind (пуст BOT_INTERNAL_SECRET), поэтому
            // вход там не доходит до конца, и переключиться на прод надо
            // прямо из приложения.
            buildConfigField("boolean", "SERVER_SWITCH_ENABLED", "true")

            isDebuggable = false
            isMinifyEnabled = true
            // setProguardFiles, а не proguardFiles: initWith уже принёс список
            // из release, и append задублировал бы файлы.
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            )

            if (hasBetaSigning) {
                signingConfig = signingConfigs.getByName("beta")
            }

            matchingFallbacks += listOf("beta", "release")

            firebaseAppDistribution {
                artifactType = "APK"
                groups = "testers"
                releaseNotes = "Beta build from GitHub Actions"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // Retrofit
    implementation(libs.retrofit)
    // Retrofit with Kotlin serialization Converter
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    // Не debugImplementation: лог нужен и в типе сборки beta, а он не debug.
    // В release вызов стоит под `if (BuildConfig.HTTP_LOG_ENABLED)` с константой
    // false, поэтому R8 сворачивает ветку и выбрасывает библиотеку целиком.
    implementation(libs.okhttp.logging.interceptor)
    // Kotlin serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)
    implementation(libs.coil.compose)
    // Firebase distribution
    implementation(libs.firebase.appdistribution.api)

    // Полный SDK (с UI обновления) — только в сборках, которые уходят тестерам.
    "betaImplementation"(libs.firebase.appdistribution)
    "betaReleaseImplementation"(libs.firebase.appdistribution)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.configureEach {
    if (name.startsWith("process") && name.endsWith("GoogleServices") && !name.isBetaGoogleServicesTask()) {
        enabled = false
    }
}
