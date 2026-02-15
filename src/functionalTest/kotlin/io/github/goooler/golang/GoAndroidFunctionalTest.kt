package io.github.goooler.golang

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.exists
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

class GoAndroidFunctionalTest : BaseFunctionalTest() {

  @Test
  @EnabledIfEnvironmentVariable(named = "ANDROID_HOME", matches = ".+")
  fun `can run android task`() {
    settingsFile.writeText(
      """
      pluginManagement {
        repositories {
          google()
          mavenCentral()
          gradlePluginPortal()
        }
      }
      dependencyResolutionManagement {
        repositories {
          google()
          mavenCentral()
        }
      }
      rootProject.name = "go-android-test"
      """
        .trimIndent()
    )
    buildFile.writeText(
      """
      plugins {
        id("com.android.library")
        id("io.github.goooler.golang")
      }

      android {
        namespace = "com.example.go"
        compileSdk = 35
        defaultConfig {
          minSdk = 24
        }
      }
      """
        .trimIndent()
    )

    // Create a dummy go file
    val goFile = projectRoot.resolve("src/main/go/main.go")
    goFile.createParentDirectories()
    goFile.writeText(
      """
      package main

      import "C"

      func main() {}
      """
        .trimIndent()
    )

    val result = runWithSuccess("assembleDebug")

    assertThat(result.output).contains("BUILD SUCCESSFUL")

    AndroidArch.values.forEach { abi ->
      val libFile = projectRoot.resolve("build/intermediates/go/debug/$abi/libgo-android-test.so")
      assertThat(libFile).all { exists() }
    }
  }

  @Test
  @EnabledIfEnvironmentVariable(named = "ANDROID_HOME", matches = ".+")
  fun `can run android task with flavors`() {
    settingsFile.writeText(
      """
      pluginManagement {
        repositories {
          google()
          mavenCentral()
          gradlePluginPortal()
        }
      }
      dependencyResolutionManagement {
        repositories {
          google()
          mavenCentral()
        }
      }
      rootProject.name = "go-android-test-flavors"
      """
        .trimIndent()
    )
    buildFile.writeText(
      """
      plugins {
        id("com.android.library")
        id("io.github.goooler.golang")
      }

      android {
        namespace = "com.example.go"
        compileSdk = 35
        defaultConfig {
          minSdk = 24
        }
        flavorDimensions += "version"
        productFlavors {
          create("demo") {
            dimension = "version"
          }
          create("full") {
            dimension = "version"
          }
        }
      }
      """
        .trimIndent()
    )

    // Create a dummy go file in demo source set
    val goFile = projectRoot.resolve("src/demo/go/demo.go")
    goFile.createParentDirectories()
    goFile.writeText(
      """
      package main

      import "C"

      func main() {}
      """
        .trimIndent()
    )

    val result = runWithSuccess("assembleDemoDebug")

    assertThat(result.output).contains("BUILD SUCCESSFUL")

    AndroidArch.values.forEach { abi ->
      val libFile =
        projectRoot.resolve("build/intermediates/go/demoDebug/$abi/libgo-android-test-flavors.so")
      assertThat(libFile).all { exists() }
    }
  }
}
