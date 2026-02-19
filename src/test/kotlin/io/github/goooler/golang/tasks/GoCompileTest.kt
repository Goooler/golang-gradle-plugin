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
import kotlin.io.path.Path
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

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

  @Test
  fun `workingDir can be set manually on GoCompile task`() {
    val project = ProjectBuilder.builder().build()
    val task = project.tasks.register("testCompile", GoCompile::class.java).get()
    val workDir = project.layout.projectDirectory.dir("custom-work")

    task.workingDir.set(workDir)

    assertThat(task.workingDir.get()).isEqualTo(workDir)
  }

  @Test
  fun `GoCompile task has SkipWhenEmpty annotation on source property`() {
    val project = ProjectBuilder.builder().build()
    val task = project.tasks.register("testCompile", GoCompile::class.java).get()

    // Verify that the source property has @SkipWhenEmpty annotation
    val getSourceMethod = GoCompile::class.java.getDeclaredMethod("getSource")
    val skipWhenEmptyAnnotation =
      getSourceMethod.getAnnotation(org.gradle.api.tasks.SkipWhenEmpty::class.java)
    val inputFilesAnnotation =
      getSourceMethod.getAnnotation(org.gradle.api.tasks.InputFiles::class.java)

    assertThat(skipWhenEmptyAnnotation).isNotNull()
    assertThat(inputFilesAnnotation).isNotNull()
  }
}
