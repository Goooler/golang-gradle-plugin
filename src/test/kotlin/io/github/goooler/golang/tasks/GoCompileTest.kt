package io.github.goooler.golang.tasks

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.endsWith
import assertk.assertions.exists
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isExecutable
import assertk.assertions.key
import com.android.build.api.dsl.LibraryExtension
import io.github.goooler.golang.GoBuildMode
import io.github.goooler.golang.GoExtension
import io.github.goooler.golang.GoPlugin
import java.io.File
import kotlin.io.path.Path
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.io.TempDir

class GoCompileTest {
  @Test
  fun `GoCompile properties for desktop are configured correctly`() {
    val project = ProjectBuilder.builder().withName("gc").build()
    project.plugins.apply(JavaPlugin::class.java)
    project.plugins.apply(GoPlugin::class.java)

    val compileGo = project.tasks.named("compileGo", GoCompile::class.java).get()

    assertThat(compileGo.buildMode.get()).isEqualTo(GoBuildMode.EXE)
    assertThat(compileGo.outputFile.get().asFile.invariantSeparatorsPath).endsWith("go/bin/main/gc")
    assertThat(compileGo.workingDir.get().asFile.invariantSeparatorsPath).endsWith("src/main/go")
    assertThat(compileGo.compilerArgs.get()).isEqualTo(emptyList<String>())
    assertThat(compileGo.source.isEmpty).isEqualTo(true)
  }

  @Test
  fun `GoCompile task can be instantiated and configured manually`() {
    val project = ProjectBuilder.builder().build()
    val task = project.tasks.register("testCompile", GoCompile::class.java).get()

    val outputFile = project.file("out/lib.so")
    task.outputFile.set(outputFile)
    task.buildMode.set(GoBuildMode.C_SHARED)
    task.environment.put("GOOS", "linux")

    assertThat(task.outputFile.get().asFile).isEqualTo(outputFile)
    assertThat(task.buildMode.get()).isEqualTo(GoBuildMode.C_SHARED)
    assertThat(task.environment.get()).contains("GOOS", "linux")
  }

  @EnabledIfEnvironmentVariable(named = "ANDROID_HOME", matches = ".+")
  @Test
  fun `GoCompile properties for Android variants are configured correctly`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("com.android.library")
    project.plugins.apply("io.github.goooler.golang")

    val android = project.extensions.getByType(LibraryExtension::class.java)
    android.compileSdk = 35
    android.namespace = "com.example.go"
    android.defaultConfig { minSdk = 24 }

    project.extensions.configure(GoExtension::class.java) {
      it.workingDir.set(project.layout.projectDirectory.dir("ext-work"))
    }

    project.afterEvaluate {
      val taskName = "compileGoDebugArm64"
      val task = project.tasks.named(taskName, GoCompile::class.java).get()

      assertThat(task.buildMode.get()).isEqualTo(GoBuildMode.C_SHARED)
      assertThat(task.environment.get()).all {
        hasSize(5)
        contains("CGO_ENABLED", "1")
        contains("GOOS", "android")
        contains("GOARCH", "arm64")
        key("GOARM").isEmpty()
        key("CC")
          .transform { Path(it) }
          .all {
            exists()
            isExecutable()
          }
      }
      assertThat(task.workingDir.get().asFile).isEqualTo(project.projectDir.resolve("ext-work"))
    }
  }

  @EnabledIfEnvironmentVariable(named = "ANDROID_HOME", matches = ".+")
  @Test
  fun `GoCompile debug variant has no trimpath arg`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("com.android.library")
    project.plugins.apply("io.github.goooler.golang")

    val android = project.extensions.getByType(LibraryExtension::class.java)
    android.compileSdk = 35
    android.namespace = "com.example.go"
    android.defaultConfig { minSdk = 24 }

    project.afterEvaluate {
      val task = project.tasks.named("compileGoDebugArm64", GoCompile::class.java).get()
      assertThat(task.compilerArgs.get()).isEqualTo(emptyList<String>())
    }
  }

  @EnabledIfEnvironmentVariable(named = "ANDROID_HOME", matches = ".+")
  @Test
  fun `GoCompile release variant has release args`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("com.android.library")
    project.plugins.apply("io.github.goooler.golang")

    val android = project.extensions.getByType(LibraryExtension::class.java)
    android.compileSdk = 35
    android.namespace = "com.example.go"
    android.defaultConfig { minSdk = 24 }

    project.afterEvaluate {
      val task = project.tasks.named("compileGoReleaseArm64", GoCompile::class.java).get()
      assertThat(task.compilerArgs.get()).isEqualTo(listOf("-trimpath", "-ldflags", "-s -w"))
    }
  }

  @EnabledIfEnvironmentVariable(named = "ANDROID_HOME", matches = ".+")
  @Test
  fun `GoCompile release variant appends release args to user-provided args`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("com.android.library")
    project.plugins.apply("io.github.goooler.golang")

    val android = project.extensions.getByType(LibraryExtension::class.java)
    android.compileSdk = 35
    android.namespace = "com.example.go"
    android.defaultConfig { minSdk = 24 }

    project.extensions.configure(GoExtension::class.java) { it.compilerArgs.add("-v") }

    project.afterEvaluate {
      val task = project.tasks.named("compileGoReleaseArm64", GoCompile::class.java).get()
      assertThat(task.compilerArgs.get()).isEqualTo(listOf("-v", "-trimpath", "-ldflags", "-s -w"))
    }
  }

  @EnabledIfEnvironmentVariable(named = "ANDROID_HOME", matches = ".+")
  @Test
  fun `GoCompile environment reads ndk dir from local properties`(@TempDir tempDir: File) {
    val ndkDir =
      System.getenv("ANDROID_NDK")
        ?: System.getenv("ANDROID_NDK_HOME")
        ?: System.getenv("ANDROID_NDK_LATEST_HOME")
        ?: run {
          // Discover the NDK from the Android SDK if no env var is set
          val androidHome = System.getenv("ANDROID_HOME")
          File(androidHome, "ndk").listFiles()?.sortedDescending()?.firstOrNull()?.absolutePath
        }
        ?: return // skip if no NDK found at all

    val localProperties = File(tempDir, "local.properties")
    localProperties.writeText("ndk.dir=$ndkDir\n")

    val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
    project.plugins.apply("com.android.library")
    project.plugins.apply("io.github.goooler.golang")

    val android = project.extensions.getByType(LibraryExtension::class.java)
    android.compileSdk = 35
    android.namespace = "com.example.go"
    android.defaultConfig { minSdk = 24 }

    project.afterEvaluate {
      val task = project.tasks.named("compileGoDebugArm64", GoCompile::class.java).get()
      assertThat(task.environment.get()).all {
        contains("CGO_ENABLED", "1")
        contains("GOOS", "android")
        key("CC").transform { Path(it) }.exists()
      }
    }
  }

  @Test
  fun `workingDir can be set manually on GoCompile task`() {
    val project = ProjectBuilder.builder().build()
    val task = project.tasks.register("testCompile", GoCompile::class.java).get()
    val workDir = project.layout.projectDirectory.dir("custom-work")

    task.workingDir.set(workDir)

    assertThat(task.workingDir.get()).isEqualTo(workDir)
  }
}
