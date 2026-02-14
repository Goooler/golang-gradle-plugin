package io.github.goooler.golang

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.exists
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.io.TempDir

class GoAndroidFunctionalTest {

  @field:TempDir lateinit var projectDir: File

  private val buildFile
    get() = projectDir.resolve("build.gradle.kts")

  private val settingsFile
    get() = projectDir.resolve("settings.gradle.kts")

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
        id("com.android.library") version "8.8.0"
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
    val goFile = projectDir.resolve("src/main/go/main.go")
    goFile.parentFile.mkdirs()
    goFile.writeText(
      """
      package main

      import "C"

      func main() {}
      """
        .trimIndent()
    )

    val runner = GradleRunner.create()
    runner.forwardOutput()
    runner.withPluginClasspath()
    runner.withArguments("assembleDebug", "--stacktrace")
    runner.withProjectDir(projectDir)
    val result = runner.build()

    assertThat(result.output).contains("BUILD SUCCESSFUL")

    val abis = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
    abis.forEach { abi ->
      val libFile = projectDir.resolve("build/intermediates/go/debug/$abi/libgo-android-test.so")
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
        id("com.android.library") version "8.8.0"
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
    val goFile = projectDir.resolve("src/demo/go/demo.go")
    goFile.parentFile.mkdirs()
    goFile.writeText(
      """
      package main

      import "C"

      func main() {}
      """
        .trimIndent()
    )

    val runner = GradleRunner.create()
    runner.forwardOutput()
    runner.withPluginClasspath()
    runner.withArguments("assembleDemoDebug", "--stacktrace")
    runner.withProjectDir(projectDir)
    val result = runner.build()

    assertThat(result.output).contains("BUILD SUCCESSFUL")

    val abis = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
    abis.forEach { abi ->
      val libFile =
        projectDir.resolve("build/intermediates/go/demoDebug/$abi/libgo-android-test-flavors.so")
      assertThat(libFile).all { exists() }
    }
  }
}
