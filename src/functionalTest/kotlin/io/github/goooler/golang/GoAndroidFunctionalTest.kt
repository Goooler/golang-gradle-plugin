package io.github.goooler.golang

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.exists
import assertk.assertions.isNotNull
import java.util.Properties
import java.util.jar.JarFile
import kotlin.collections.firstOrNull
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.writeText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GoAndroidFunctionalTest : BaseFunctionalTest() {

  lateinit var ndkVersion: String
    private set

  @BeforeEach
  fun setupLocalProperties() {
    val ndkHome =
      System.getenv("ANDROID_NDK")
        ?: System.getenv("ANDROID_NDK_HOME")
        ?: System.getProperty("ANDROID_HOME")?.let {
          Path(it).resolve("ndk").firstOrNull()?.absolutePathString()
        }
        ?: error(
          "NDK path not found. Please set ANDROID_NDK, ANDROID_NDK_HOME, or ANDROID_HOME environment variable."
        )

    val properties = Properties().apply { setProperty("ndk.dir", ndkHome) }
    projectRoot.resolve("local.properties").outputStream().use { properties.store(it, null) }

    val propsFile = Path(ndkHome).resolve("source.properties")
    if (propsFile.exists()) {
      val props = Properties().apply { load(propsFile.inputStream()) }
      ndkVersion = props.getProperty("Pkg.Revision")
    } else {
      error("source.properties not found in NDK directory: $ndkHome")
    }
  }

  @BeforeEach
  fun beforeEach() {
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
      """
        .trimIndent() + System.lineSeparator()
    )
  }

  @Test
  fun `can run android task`() {
    settingsFile.appendText(
      """
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
        ndkVersion = "$ndkVersion"
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
      assertThat(projectRoot.resolve("build/intermediates/go/debug/$abi/libgo-android-test.so"))
        .exists()

      val mergedLibFile =
        projectRoot.resolve("build/generated/jniLibs/mergeGoJniLibsDebug/$abi/${libFile.name}")
      assertThat(mergedLibFile).exists()
    }

    val aarFile = projectRoot.resolve("build/outputs/aar/go-android-test-debug.aar")
    assertThat(aarFile).exists()

    JarFile(aarFile.toFile()).use { jar ->
      AndroidArch.values.forEach { abi ->
        val entry = jar.getJarEntry("jni/$abi/libgo-android-test.so")
        assertThat(entry).all { isNotNull() }
      }
    }
  }

  @Test
  fun `can run android task with golang source dir`() {
    settingsFile.appendText(
      """
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
        ndkVersion = "$ndkVersion"
        defaultConfig {
          minSdk = 24
        }
      }
      """
        .trimIndent()
    )

    // Create a dummy go file in the golang directory
    val goFile = projectRoot.resolve("src/main/golang/main.go")
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
      assertThat(projectRoot.resolve("build/intermediates/go/debug/$abi/libgo-android-test.so"))
        .exists()

      val mergedLibFile =
        projectRoot.resolve("build/generated/jniLibs/mergeGoJniLibsDebug/$abi/${libFile.name}")
      assertThat(mergedLibFile).exists()
    }

    val aarFile = projectRoot.resolve("build/outputs/aar/go-android-test-debug.aar")
    assertThat(aarFile).exists()

    JarFile(aarFile.toFile()).use { jar ->
      AndroidArch.values.forEach { abi ->
        val entry = jar.getJarEntry("jni/$abi/libgo-android-test.so")
        assertThat(entry).all { isNotNull() }
      }
    }
  }

  @Test
  fun `cmake tasks depend on go compile tasks`() {
    settingsFile.appendText(
      """
      rootProject.name = "go-cmake-deps-test"
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
        ndkVersion = "$ndkVersion"
        defaultConfig {
          minSdk = 24
        }
      }

      // Simulate CMake tasks that AGP would normally create for each ABI
      tasks.register("buildCMakeDebug[armeabi-v7a]")
      """
        .trimIndent()
    )

    val result = runWithSuccess("--dry-run", "buildCMakeDebug[armeabi-v7a]")

    assertThat(result.output).contains(":compileGoDebugArm32 SKIPPED")
    assertThat(result.output).contains(":buildCMakeDebug[armeabi-v7a] SKIPPED")
  }

  @Test
  fun `cmake release tasks depend on go compile tasks`() {
    settingsFile.appendText(
      """
      rootProject.name = "go-cmake-deps-test"
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
        ndkVersion = "$ndkVersion"
        defaultConfig {
          minSdk = 24
        }
      }

      // Simulate CMake tasks that AGP would normally create for each ABI
      tasks.register("buildCMakeRelWithDebInfo[armeabi-v7a]")
      """
        .trimIndent()
    )

    val result = runWithSuccess("--dry-run", "buildCMakeRelWithDebInfo[armeabi-v7a]")

    assertThat(result.output).contains(":compileGoReleaseArm32 SKIPPED")
    assertThat(result.output).contains(":buildCMakeRelWithDebInfo[armeabi-v7a] SKIPPED")
  }

  @Test
  fun `cmake flavored release tasks depend on go compile tasks`() {
    settingsFile.appendText(
      """
      rootProject.name = "go-cmake-flavored-release-deps-test"
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
        namespace = "com.example.go.flavored"
        compileSdk = 35
        ndkVersion = "$ndkVersion"
        defaultConfig {
          minSdk = 24
        }

        flavorDimensions += "version"
        productFlavors {
          create("demo") {
            dimension = "version"
          }
        }
      }

      // Simulate CMake tasks that AGP would normally create for each ABI for demoRelease
      tasks.register("buildCMakeDemoRelWithDebInfo[armeabi-v7a]")
      """
        .trimIndent()
    )

    val result = runWithSuccess("--dry-run", "buildCMakeDemoRelWithDebInfo[armeabi-v7a]")

    assertThat(result.output).contains(":compileGoDemoReleaseArm32 SKIPPED")
    assertThat(result.output).contains(":buildCMakeDemoRelWithDebInfo[armeabi-v7a] SKIPPED")
  }

  @Test
  fun `can run android task with flavors`() {
    settingsFile.appendText(
      """
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
        ndkVersion = "$ndkVersion"
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
      assertThat(libFile).exists()

      val mergedLibFile =
        projectRoot.resolve("build/generated/jniLibs/mergeGoJniLibsDemoDebug/$abi/${libFile.name}")
      assertThat(mergedLibFile).exists()
    }

    val aarFile = projectRoot.resolve("build/outputs/aar/go-android-test-flavors-demo-debug.aar")
    assertThat(aarFile).exists()

    JarFile(aarFile.toFile()).use { jar ->
      AndroidArch.values.forEach { abi ->
        val entry = jar.getJarEntry("jni/$abi/libgo-android-test-flavors.so")
        assertThat(entry).all { isNotNull() }
      }
    }
  }
}
