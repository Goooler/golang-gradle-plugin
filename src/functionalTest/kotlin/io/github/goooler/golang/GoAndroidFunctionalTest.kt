package io.github.goooler.golang

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.exists
import assertk.assertions.isNotNull
import java.util.Properties
import java.util.jar.JarFile
import kotlin.io.path.Path
import kotlin.io.path.appendText
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.writeText
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GoAndroidFunctionalTest : BaseFunctionalTest() {

  lateinit var ndkVersion: String
    private set

  @BeforeEach
  fun setupNdkVersion() {
    System.getenv("ANDROID_SDK_ROOT")
      ?: System.getenv("ANDROID_HOME")
      ?: error(
        "SDK path not found. Please set ANDROID_SDK_ROOT or ANDROID_HOME environment variable."
      )

    val ndkHome =
      System.getenv("ANDROID_NDK")
        ?: System.getenv("ANDROID_NDK_HOME")
        ?: System.getenv("ANDROID_NDK_LATEST_HOME")
        ?: error(
          "NDK path not found. Please set ANDROID_NDK, ANDROID_NDK_HOME, or ANDROID_NDK_LATEST_HOME environment variable."
        )

    val propsFile = Path(ndkHome).resolve("source.properties")
    if (propsFile.exists()) {
      val props = Properties().apply { load(propsFile.inputStream()) }
      ndkVersion = checkNotNull(props.getProperty("Pkg.Revision"))
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

      //export Noop
      func Noop() {}

      func main() {}
      """
        .trimIndent()
    )

    val result = runWithSuccess("assembleDebug")

    assertThat(result.output).contains("BUILD SUCCESSFUL")

    AndroidArch.values.forEach { abi ->
      val libFile = projectRoot.resolve("build/intermediates/go/debug/$abi/libgo-android-test.so")
      assertThat(libFile).exists()
      val headerFile = projectRoot.resolve("build/intermediates/go/debug/$abi/libgo-android-test.h")
      assertThat(headerFile).exists()

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

      //export Noop
      func Noop() {}

      func main() {}
      """
        .trimIndent()
    )

    val result = runWithSuccess("assembleDebug")

    assertThat(result.output).contains("BUILD SUCCESSFUL")

    AndroidArch.values.forEach { abi ->
      val libFile = projectRoot.resolve("build/intermediates/go/debug/$abi/libgo-android-test.so")
      assertThat(libFile).exists()
      assertThat(projectRoot.resolve("build/intermediates/go/debug/$abi/libgo-android-test.h"))
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
  fun `cmake tasks only depend on their own variant's go compile tasks`() {
    settingsFile.appendText(
      """
      rootProject.name = "go-cmake-variant-isolation-test"
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
        namespace = "com.example.go.isolated"
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

      // Simulate CMake tasks that AGP would normally create for each flavor's debug variant
      tasks.register("buildCMakeDemoDebug[armeabi-v7a]")
      tasks.register("buildCMakeDemoDebug[x86_64]")
      tasks.register("buildCMakeFullDebug[armeabi-v7a]")
      tasks.register("buildCMakeFullDebug[x86_64]")
      // Simulate tasks with numeric suffixes (AGP may append -N when multiple CMake configs exist)
      tasks.register("buildCMakeDemoDebug[armeabi-v7a]-2")
      tasks.register("buildCMakeFullDebug[armeabi-v7a]-2")
      // Simulate configureCMake tasks (AGP creates these alongside buildCMake tasks)
      tasks.register("configureCMakeDemoDebug[armeabi-v7a]")
      tasks.register("configureCMakeFullDebug[armeabi-v7a]")
      """
        .trimIndent()
    )

    val demoResult = runWithSuccess("--dry-run", "buildCMakeDemoDebug[armeabi-v7a]")

    // Should depend on the demo debug Go compile task
    assertThat(demoResult.output).contains(":compileGoDemoDebugArm32 SKIPPED")
    // Must NOT pull in the full variant's Go compile task
    assertThat(demoResult.output).doesNotContain(":compileGoFullDebugArm32")
    assertThat(demoResult.output).contains(":buildCMakeDemoDebug[armeabi-v7a] SKIPPED")

    val fullResult = runWithSuccess("--dry-run", "buildCMakeFullDebug[armeabi-v7a]")

    // Should depend on the full debug Go compile task
    assertThat(fullResult.output).contains(":compileGoFullDebugArm32 SKIPPED")
    // Must NOT pull in the demo variant's Go compile task
    assertThat(fullResult.output).doesNotContain(":compileGoDemoDebugArm32")
    assertThat(fullResult.output).contains(":buildCMakeFullDebug[armeabi-v7a] SKIPPED")

    // Additional coverage: ensure x86_64 CMake tasks only depend on x86_64 Go compile tasks,
    // and do not accidentally depend on x86 Go compile tasks (substring ABI name edge case).
    val demoX86_64Result = runWithSuccess("--dry-run", "buildCMakeDemoDebug[x86_64]")

    // Should depend on the demo debug Go compile task for x86_64
    assertThat(demoX86_64Result.output).contains(":compileGoDemoDebugX64 SKIPPED")
    // Must NOT pull in the demo debug Go compile task for x86
    assertThat(demoX86_64Result.output).doesNotContain(":compileGoDemoDebugX86 ")
    assertThat(demoX86_64Result.output).contains(":buildCMakeDemoDebug[x86_64] SKIPPED")

    val fullX86_64Result = runWithSuccess("--dry-run", "buildCMakeFullDebug[x86_64]")

    // Should depend on the full debug Go compile task for x86_64
    assertThat(fullX86_64Result.output).contains(":compileGoFullDebugX64 SKIPPED")
    // Must NOT pull in the full debug Go compile task for x86
    assertThat(fullX86_64Result.output).doesNotContain(":compileGoFullDebugX86 ")
    assertThat(fullX86_64Result.output).contains(":buildCMakeFullDebug[x86_64] SKIPPED")

    // Additional coverage: tasks with numeric suffixes (e.g., -2) must also depend on the
    // correct variant's Go compile task.
    val demoNumericSuffixResult = runWithSuccess("--dry-run", "buildCMakeDemoDebug[armeabi-v7a]-2")

    assertThat(demoNumericSuffixResult.output).contains(":compileGoDemoDebugArm32 SKIPPED")
    assertThat(demoNumericSuffixResult.output).doesNotContain(":compileGoFullDebugArm32")
    assertThat(demoNumericSuffixResult.output)
      .contains(":buildCMakeDemoDebug[armeabi-v7a]-2 SKIPPED")

    val fullNumericSuffixResult = runWithSuccess("--dry-run", "buildCMakeFullDebug[armeabi-v7a]-2")

    assertThat(fullNumericSuffixResult.output).contains(":compileGoFullDebugArm32 SKIPPED")
    assertThat(fullNumericSuffixResult.output).doesNotContain(":compileGoDemoDebugArm32")
    assertThat(fullNumericSuffixResult.output)
      .contains(":buildCMakeFullDebug[armeabi-v7a]-2 SKIPPED")

    // Additional coverage: configureCMake tasks must also depend on the correct variant's Go
    // compile task (not just buildCMake tasks).
    val demoConfigureResult = runWithSuccess("--dry-run", "configureCMakeDemoDebug[armeabi-v7a]")

    assertThat(demoConfigureResult.output).contains(":compileGoDemoDebugArm32 SKIPPED")
    assertThat(demoConfigureResult.output).doesNotContain(":compileGoFullDebugArm32")
    assertThat(demoConfigureResult.output).contains(":configureCMakeDemoDebug[armeabi-v7a] SKIPPED")

    val fullConfigureResult = runWithSuccess("--dry-run", "configureCMakeFullDebug[armeabi-v7a]")

    assertThat(fullConfigureResult.output).contains(":compileGoFullDebugArm32 SKIPPED")
    assertThat(fullConfigureResult.output).doesNotContain(":compileGoDemoDebugArm32")
    assertThat(fullConfigureResult.output).contains(":configureCMakeFullDebug[armeabi-v7a] SKIPPED")
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
