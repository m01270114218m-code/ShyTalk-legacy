// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.play.publisher) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt)
    id("org.sonarqube") version "7.5.0.8588"
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "androidx.lifecycle" &&
                (requested.name == "lifecycle-runtime-compose" ||
                    requested.name == "lifecycle-viewmodel-compose" ||
                    requested.name == "lifecycle-runtime-ktx" ||
                    requested.name == "lifecycle-viewmodel" ||
                    requested.name == "lifecycle-process")) {
                useVersion("2.10.0")
                because("Keep Android builds on compileSdk 36; lifecycle 2.11.0 requires API 37.")
            }
        }
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.8.0")
        android.set(true)
        outputToConsole.set(true)
        ignoreFailures.set(false)
        filter {
            exclude { element ->
                element.file.absolutePath
                    .replace("\\", "/")
                    .contains("/build/")
            }
        }
    }
}

detekt {
    buildUponDefaultConfig = false
    config.setFrom(files("detekt.yml"))
    parallel = true
    source.setFrom(
        files(
            "shared/src/commonMain/kotlin",
            "shared/src/androidMain/kotlin",
            "app/src/main/java",
        ),
    )
}

// Skip sonar on the app module — SonarQube plugin v7 uses AppExtension (old AGP API)
// which doesn't exist in AGP 8+. Analyze shared + express-api only until plugin is updated.
project(":app") {
    sonar {
        isSkipProject = true
    }
}

sonar {
    properties {
        property("sonar.projectKey", "ShydenMcM_ShyTalk")
        property("sonar.organization", "shydenmcm")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.gradle.skipCompile", "true")
        property("sonar.qualitygate.wait", "false")
        property("sonar.sources", "express-api/src")
        property("sonar.tests", "express-api/tests")
        property(
            "sonar.junit.reportPaths",
            "${rootProject.projectDir}/shared/build/test-results/jvmTest",
        )
        property(
            "sonar.exclusions",
            listOf(
                "**/build/**",
                "**/node_modules/**",
                "**/*.json",
                "**/composeResources/**",
                "**/iosMain/**",
                "**/androidMain/**",
                "public/js/event-translations.js",
                "public/js/legal-translations.js",
                "public/js/suggestions-i18n.js",
            ).joinToString(","),
        )
        property(
            "sonar.coverage.exclusions",
            listOf(
                "shared/src/commonMain/**",
                "shared/src/androidMain/**",
                "shared/src/iosMain/**",
                "app/src/main/**",
                "iosApp/**",
            ).joinToString(","),
        )
        property(
            "sonar.cpd.exclusions",
            "public/js/event-translations.js,public/js/legal-translations.js,public/js/suggestions-i18n.js",
        )
        property("sonar.javascript.lcov.reportPaths", "express-api/coverage/lcov.info")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${rootProject.projectDir}/app/build/reports/jacoco/devDebug/jacoco.xml",
        )
    }
}
