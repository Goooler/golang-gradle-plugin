@file:Suppress("UnstableApiUsage")

import org.gradle.api.plugins.JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.android.lint)
  alias(libs.plugins.jetbrains.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.pluginPublish)
  alias(libs.plugins.spotless)
}

version = providers.gradleProperty("VERSION_NAME").get()

group = providers.gradleProperty("GROUP").get()

description = providers.gradleProperty("POM_DESCRIPTION").get()

dokka { dokkaPublications.html { outputDirectory = rootDir.resolve("docs/api") } }

kotlin {
  explicitApi()
  @OptIn(ExperimentalAbiValidation::class) abiValidation { enabled = true }
  compilerOptions {
    allWarningsAsErrors = true
    // https://docs.gradle.org/current/userguide/compatibility.html#kotlin
    apiVersion = KotlinVersion.KOTLIN_2_2
    languageVersion = apiVersion
    jvmTarget = JvmTarget.fromTarget(libs.versions.jdkRelease.get())
    jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
    freeCompilerArgs.add("-Xjdk-release=${libs.versions.jdkRelease.get()}")
  }
}

lint {
  baseline = file("gradle/lint-baseline.xml")
  ignoreTestFixturesSources = true
  ignoreTestSources = true
  warningsAsErrors = true
  disable += "NewerVersionAvailable"
  disable += "GradleDependency"
  disable += "AndroidGradlePluginVersion"
}

spotless {
  kotlin { ktfmt(libs.ktfmt.get().version).googleStyle() }
  kotlinGradle { ktfmt(libs.ktfmt.get().version).googleStyle() }
}

val testPluginClasspath by
  configurations.registering {
    isCanBeResolved = true
    description = "Plugins used in integration tests could be resolved in classpath."
  }

configurations.named(API_ELEMENTS_CONFIGURATION_NAME) {
  attributes.attribute(
    // TODO: https://github.com/gradle/gradle/issues/24608
    GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
    objects.named(libs.versions.minGradle.get()),
  )
}

val testGradleVersion: String =
  providers.gradleProperty("testGradleVersion").orNull.let {
    val value = if (it == null || it == "current") GradleVersion.current().version else it
    logger.lifecycle("Using Gradle $value in tests")
    value
  }

dependencies {
  compileOnly(libs.android.gradle.api)

  testPluginClasspath(libs.android.gradle)

  lintChecks(libs.androidx.gradlePluginLints)
}

testing.suites {
  getByName<JvmTestSuite>("test") { dependencies { implementation(libs.android.gradle) } }

  register<JvmTestSuite>("functionalTest") {
    targets.configureEach {
      testTask {
        // Required to test configuration cache in tests when using withDebug().
        // See https://github.com/gradle/gradle/issues/22765#issuecomment-1339427241.
        jvmArgs(
          "--add-opens=java.base/java.util=ALL-UNNAMED",
          "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
          "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
          "--add-opens=java.base/java.net=ALL-UNNAMED",
        )
      }
    }
  }

  withType<JvmTestSuite>().configureEach {
    useJUnitJupiter(libs.junit.bom.map { checkNotNull(it.version) })
    dependencies { implementation(libs.assertk) }
    targets.configureEach {
      testTask {
        systemProperty("TEST_GRADLE_VERSION", testGradleVersion)
        develocity {
          testRetry {
            maxRetries = 2
            maxFailures = 10
          }
        }
      }
    }
  }
}

// This part should be placed after testing.suites to ensure the test sourceSets are created.
kotlin.target.compilations {
  val main by getting
  getByName("functionalTest") {
    // Share main's output with functionalTest.
    associateWith(main)
  }
}

gradlePlugin {
  website = providers.gradleProperty("POM_URL")
  vcsUrl = providers.gradleProperty("POM_URL")

  plugins {
    create("goPlugin") {
      id = group.toString()
      implementationClass = "io.github.goooler.golang.GoPlugin"
      displayName = providers.gradleProperty("POM_NAME").get()
      description = providers.gradleProperty("POM_DESCRIPTION").get()
      tags = listOf("go", "golang")
    }
  }

  testSourceSets(sourceSets["functionalTest"])
}

tasks.withType<JavaCompile>().configureEach {
  options.release = libs.versions.jdkRelease.get().toInt()
}

tasks.pluginUnderTestMetadata { pluginClasspath.from(testPluginClasspath) }

tasks.validatePlugins {
  // TODO: https://github.com/gradle/gradle/issues/22600
  enableStricterValidation = true
}

tasks.check { dependsOn(tasks.withType<Test>()) }
