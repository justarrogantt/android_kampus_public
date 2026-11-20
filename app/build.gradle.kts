import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

fun String.toBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

// google-services.json лежит только в app/src/beta (betaRelease берёт его
// симлинком), поэтому и задачи Firebase (google-services, Crashlytics) имеют
// смысл только у вариантов с «Beta» в имени: devBeta, devBetaRelease,
// stagingBeta, stagingBetaRelease.
fun String.isBetaVariantTask(): Boolean = contains("Beta")

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

// Имена параметров подписи в том же порядке, что и значения выше: нужны, чтобы
// сказать в ошибке, какого именно не хватает. Пустой BETA_KEY_PASSWORD две
// недели ронял hasBetaSigning в false молча.
val betaSigningPropertyNames = listOf(
    "BETA_KEYSTORE_FILE" to betaKeystoreFile,
    "BETA_KEYSTORE_PASSWORD" to betaKeystorePassword,
    "BETA_KEY_ALIAS" to betaKeyAlias,
    "BETA_KEY_PASSWORD" to betaKeyPassword
)

// CI собирает VERSION_NAME как «0.1.0-beta.<номер запуска>», а типы сборки beta
// и betaRelease добавляли к нему свой «-beta» — в плашке версии получалось
// «0.1.0-beta.29-beta». Суффикс нужен только там, где про бету в имени ещё
// ничего не сказано: у локальной сборки с versionName «1.0».
val versionNameValue = System.getenv("VERSION_NAME")?.takeUnless { it.isBlank() } ?: "1.0"
val betaVersionNameSuffix = "-beta"
    .takeUnless { versionNameValue.contains("beta", ignoreCase = true) }

android {
    namespace = "com.example.ysukampus"
    compileSdk = 36

    flavorDimensions += "environment"

    val baseUrl = localProperties.getProperty("BASE_URL") ?: ""
    // Адреса для переключателя серверов. Если в local.properties их нет, оба
    // падают обратно на BASE_URL, и переключатель просто не даёт выбора.
    // takeUnless(isBlank), а не только `?:` — в CI ключ приезжает из секрета и
    // при незаполненном секрете строка `BASE_URL_TEST=` даёт пустое значение,
    // а не отсутствующее: без этой проверки сборка молча уехала бы с пустым
    // базовым URL, на котором падает создание Retrofit.
    val baseUrlTest = localProperties.getProperty("BASE_URL_TEST")
        ?.takeUnless { it.isBlank() } ?: baseUrl
    val baseUrlProd = localProperties.getProperty("BASE_URL_PROD")
        ?.takeUnless { it.isBlank() } ?: baseUrl
    val telegramBotLink = localProperties.getProperty("TELEGRAM_BOT_LINK")
        ?.takeUnless { it.isBlank() } ?: "https://t.me/ysukampusbot"
    // У тестового и продового бэкендов разные Telegram-боты: код подтверждения,
    // выданный одним сервером, второй бот не знает. Если в local.properties
    // адреса нет, оба падают обратно на TELEGRAM_BOT_LINK.
    val telegramBotLinkTest = localProperties.getProperty("TELEGRAM_BOT_LINK_TEST")
        ?.takeUnless { it.isBlank() } ?: telegramBotLink
    val telegramBotLinkProd = localProperties.getProperty("TELEGRAM_BOT_LINK_PROD")
        ?.takeUnless { it.isBlank() } ?: telegramBotLink

    defaultConfig {
        applicationId = "com.example.ysukampus"
        minSdk = 26
        targetSdk = 36
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = versionNameValue
        manifestPlaceholders["appName"] = "ysukampus"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", baseUrl.toBuildConfigString())
        buildConfigField("String", "API_BASE_URL", baseUrl.toBuildConfigString())
        buildConfigField("String", "BASE_URL_TEST", baseUrlTest.toBuildConfigString())
        buildConfigField("String", "BASE_URL_PROD", baseUrlProd.toBuildConfigString())
        // Переключатель серверов доступен только в отладочных сборках.
        buildConfigField("boolean", "SERVER_SWITCH_ENABLED", "false")
        // Проверка и установка бета-обновления со своего бэкенда
        // (builds/beta/latest, см. ui/main/update). Включена только у бета-типов
        // сборки — beta и betaRelease: у них applicationId с суффиксом .beta, и
        // CI заливает на бэкенд именно их. Раньше BetaUpdateManager сверял
        // BuildConfig.BUILD_TYPE со строкой "beta", и когда раздачу перевели на
        // минифицированный betaRelease, проверка молча выключилась у всех
        // тестеров. Флагом это видно в том же месте, где задаются остальные.
        buildConfigField("boolean", "BETA_UPDATES_ENABLED", "false")
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
            // Crashlytics в debug нет ни в зависимостях, ни в исходниках
            // (srcDir crashOff ниже): краши отладочной сборки видно в logcat.
            // Флаг всё равно выключаем явно — иначе плагин повесит задачу
            // выгрузки маппинга на тип сборки, у которого нет
            // google-services.json, и debug перестанет собираться в чистом клоне.
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
                nativeSymbolUploadEnabled = false
            }
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

            // Тот же расчёт, что и в debug: release тестерам не раздаётся,
            // Crashlytics к нему не подключён, google-services.json для него
            // нет. Без явного false плагин включил бы выгрузку маппинга
            // (сборка минифицирована) и уронил бы assembleStagingRelease.
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
                nativeSymbolUploadEnabled = false
            }
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
            versionNameSuffix = betaVersionNameSuffix
            manifestPlaceholders["appName"] = "ysukampus beta"
            buildConfigField("boolean", "BETA_UPDATES_ENABLED", "true")

            isDebuggable = true
            signingConfig = if (hasBetaSigning) {
                signingConfigs.getByName("beta")
            } else {
                signingConfigs.getByName("debug")
            }

            matchingFallbacks += listOf("debug")

            // SDK Crashlytics здесь есть (см. betaImplementation ниже), отчёты
            // уходят. Маппинг не выгружается: сборка не минифицирована, R8 его
            // не создаёт, и стектрейсы и так читаемы.
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
                nativeSymbolUploadEnabled = false
            }
        }

        // Сборка, которая уходит тестерам (CI заливает её на бэкенд, откуда
        // приложение само себя обновляет, и пока ещё в App Distribution — см.
        // .github/workflows/firebase-app-distribution.yml). Отличается от
        // будущего прода только applicationIdSuffix и ключом подписи: R8,
        // proguard-правила и isDebuggable здесь такие же, как в release.
        // Ставится обновлением поверх предыдущей раздачи.
        //
        // versionCode при локальной сборке надо задавать выше установленного,
        // иначе Android откажется ставить: VERSION_CODE=24 ./gradlew ...
        create("betaRelease") {
            initWith(getByName("release"))

            applicationIdSuffix = ".beta"
            versionNameSuffix = betaVersionNameSuffix
            manifestPlaceholders["appName"] = "ysukampus beta"
            // Переключатель серверов оставлен включённым: тестовый бэкенд и
            // прод — разные серверы с разными Telegram-ботами, и без выбора
            // сборка заперта в том, что пришло в BASE_URL. На тестовом сейчас
            // выключен /auth/bot/bind (пуст BOT_INTERNAL_SECRET), поэтому
            // вход там не доходит до конца, и переключиться на прод надо
            // прямо из приложения.
            buildConfigField("boolean", "SERVER_SWITCH_ENABLED", "true")
            // Тип сборки, который и уходит тестерам: проверка обновления
            // нужна здесь в первую очередь.
            buildConfigField("boolean", "BETA_UPDATES_ENABLED", "true")

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

            // Единственный тип сборки, который выгружает mapping.txt в
            // Crashlytics. Задача uploadCrashlyticsMappingFileStagingBetaRelease
            // цепляется к assemble и отправляет тот же файл, который CI кладёт
            // артефактом на 90 дней; артефакт оставлен запасным путём на случай,
            // когда выгрузка не прошла (нет сети, протух service account).
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = true
                // NDK-кода в проекте нет, символы выгружать нечего.
                nativeSymbolUploadEnabled = false
            }
        }
    }
    // Crashlytics подключён не ко всем типам сборки, поэтому класс
    // `crash.CrashReporting` существует в двух вариантах: рабочем (crashOn) и
    // пустышке (crashOff). Остальной код зовёт его одинаково и не знает, какая
    // из реализаций попала в вариант; в debug/release ссылки на классы
    // Firebase просто не компилируются — их нет на classpath.
    sourceSets {
        // kotlin.srcDir, а не java.srcDir: .kt-файлы из каталога, добавленного
        // в java-набор, компилятор Kotlin не подхватывает, и класс просто не
        // находится.
        getByName("beta").kotlin.srcDir("src/crashOn/java")
        getByName("betaRelease").kotlin.srcDir("src/crashOn/java")
        getByName("debug").kotlin.srcDir("src/crashOff/java")
        getByName("release").kotlin.srcDir("src/crashOff/java")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Без этого любой android.jar-стаб (Log.d из AuthLog и так далее),
    // вызванный кодом под тестом, падает с "Method ... not mocked" — тесты
    // на auth/refresh-логику неизбежно проходят через AuthLog.d, потому что
    // HTTP_LOG_ENABLED включён в debug/beta. Настоящей логики стаб не несёт,
    // поэтому дефолтные значения (0/null/false) ничего не подменяют.
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

// Room складывает сюда JSON-схему каждой версии базы. Схемы коммитятся: без
// них нельзя ни написать миграцию, ни проверить её тестом.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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
    // Room: локальный кэш ответов (data/local/db). Процессор один на все
    // флейворы — ksp(...) раскладывается по всем вариантам сам.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.coil.compose)

    // Crashlytics — только в сборках, которые уходят тестерам. Иначе раздачи
    // нет, а зависимость и её
    // фоновый сборщик отчётов есть. Никакой Analytics: firebase-crashlytics
    // работает без него, теряется только «последние N событий перед крашем»,
    // а это ровно тот сбор поведения, который здесь не нужен.
    "betaImplementation"(libs.firebase.crashlytics)
    "betaReleaseImplementation"(libs.firebase.crashlytics)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Задачи Firebase выключаются у всех не-beta вариантов.
//
// processGoogleServices — потому что без этого debug и release требовали бы
// google-services.json, которого для них нет, и сборка в чистом клоне падала бы
// на конфигурации Firebase, не имеющей к ним отношения.
//
// injectCrashlytics* — потому что Crashlytics подключён только к beta и
// betaRelease. Плагин по умолчанию зашивает свои строковые ресурсы
// (mapping_file_id, version_control_info с данными о git-ревизии) во все
// варианты подряд, даже туда, где SDK нет и читать их некому.
tasks.configureEach {
    val isFirebaseVariantTask = (name.startsWith("process") && name.endsWith("GoogleServices")) ||
        name.startsWith("injectCrashlytics")
    if (isFirebaseVariantTask && !name.isBetaVariantTask()) {
        enabled = false
    }
}

// betaRelease без подписи собираться не должен.
//
// Так уже было: секрет BETA_KEY_PASSWORD оказался пустым, hasBetaSigning стал
// false, а у betaRelease — в отличие от beta — отката на debug-ключ нет, и
// signingConfig просто не проставлялся. R8 отрабатывал, App Distribution
// принимал APK и рапортовал успехом, пайплайн был зелёный — а поставить такую
// сборку нельзя ни одному тестеру: Android не ставит неподписанные APK. Две
// недели раздач ушли впустую, и заметить это было нечем.
//
// Проверка висит на упаковке, а не на конфигурации: в клоне без beta-ключа
// остальные варианты обязаны собираться, ключ есть не у каждого.
tasks.configureEach {
    if (name.startsWith("package") && name.endsWith("BetaRelease") && !hasBetaSigning) {
        doFirst {
            val missing = betaSigningPropertyNames
                .filter { (_, value) -> value.isNullOrBlank() }
                .map { (key, _) -> key }

            throw GradleException(
                "betaRelease собирается без подписи, такой APK не установится. " +
                    "Не заданы: ${missing.joinToString(", ")}. " +
                    "В CI это секреты репозитория, локально — строки в local.properties."
            )
        }
    }
}
