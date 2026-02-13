package io.github.goooler.golang

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.exists
import assertk.assertions.isNotNull
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GoPluginFunctionalTest {

  @field:TempDir lateinit var projectDir: File

  private val buildFile
    get() = projectDir.resolve("build.gradle.kts")

  private val settingsFile
    get() = projectDir.resolve("settings.gradle.kts")

  @Test
  fun `can run task`() {
    settingsFile.writeText("")
    buildFile.writeText(
      """
      plugins {
          id("io.github.goooler.golang")
      }
      """
        .trimIndent()
    )

    // Create a dummy go file
    val goFile = projectDir.resolve("src/main/go/main.go")
    goFile.parentFile.mkdirs()
    goFile.writeText("package main\nfunc main() {}")

    val runner = GradleRunner.create()
    runner.forwardOutput()
    runner.withPluginClasspath()
    runner.withArguments("compileGo", "--stacktrace")
    runner.withProjectDir(projectDir)
    val result = runner.build()

    assertThat(result.output).contains("BUILD SUCCESSFUL")
    val task = result.task(":compileGo")
    assertThat(task).isNotNull()
  }

  @Test
  fun `can configure output file`() {
    settingsFile.writeText("")
    buildFile.writeText(
      """
      plugins {
          id("io.github.goooler.golang")
      }

      tasks.named<io.github.goooler.golang.tasks.GoCompile>("compileGo") {
          outputFile.set(layout.buildDirectory.file("custom-output"))
      }
      """
        .trimIndent()
    )

    // Create a dummy go file
    val goFile = projectDir.resolve("src/main/go/main.go")
    goFile.parentFile.mkdirs()
    goFile.writeText("package main\nfunc main() {}")

    val runner = GradleRunner.create()
    runner.forwardOutput()
    runner.withPluginClasspath()
    runner.withArguments("compileGo", "--stacktrace")
    runner.withProjectDir(projectDir)
    val result = runner.build()

    assertThat(result.output).contains("BUILD SUCCESSFUL")
    val task = result.task(":compileGo")
    assertThat(task).isNotNull()

    val outputFile = projectDir.resolve("build/custom-output")
    assertThat(outputFile).all { exists() }
  }

  @Test
  fun `can configure build mode for shared library`() {
    settingsFile.writeText("")
    buildFile.writeText(
      """
      plugins {
          id("io.github.goooler.golang")
      }

      tasks.named<io.github.goooler.golang.tasks.GoCompile>("compileGo") {
          buildMode.set(io.github.goooler.golang.tasks.GoBuildMode.C_SHARED)
          outputFile.set(layout.buildDirectory.file("lib/libmain.so"))
      }
      """
        .trimIndent()
    )

    // Create a dummy go file with export for C shared library
    val goFile = projectDir.resolve("src/main/go/main.go")
    goFile.parentFile.mkdirs()
    goFile.writeText(
      """
      package main

      import "C"

      //export HelloWorld
      func HelloWorld() {
      }

      func main() {}
      """
        .trimIndent()
    )

    val runner = GradleRunner.create()
    runner.forwardOutput()
    runner.withPluginClasspath()
    runner.withArguments("compileGo", "--stacktrace")
    runner.withProjectDir(projectDir)
    val result = runner.build()

    assertThat(result.output).contains("BUILD SUCCESSFUL")
    val task = result.task(":compileGo")
    assertThat(task).isNotNull()

    val outputFile = projectDir.resolve("build/lib/libmain.so")
    assertThat(outputFile).all { exists() }
  }

  @Test
  fun `can configure Android extension`() {
    settingsFile.writeText("")
    buildFile.writeText(
      """
      plugins {
          id("io.github.goooler.golang")
      }

      goAndroid {
          apiLevel.set(21)
          architectures.set(listOf(
              io.github.goooler.golang.AndroidArchitecture.ARM64,
              io.github.goooler.golang.AndroidArchitecture.ARM
          ))
      }
      """
        .trimIndent()
    )

    // Create a dummy go file
    val goFile = projectDir.resolve("src/main/go/main.go")
    goFile.parentFile.mkdirs()
    goFile.writeText("package main\nfunc main() {}")

    val runner = GradleRunner.create()
    runner.forwardOutput()
    runner.withPluginClasspath()
    runner.withArguments("compileGo", "--stacktrace")
    runner.withProjectDir(projectDir)
    val result = runner.build()

    assertThat(result.output).contains("BUILD SUCCESSFUL")
    val task = result.task(":compileGo")
    assertThat(task).isNotNull()
  }

  @Test
  fun `can configure environment variables for cross-compilation`() {
    settingsFile.writeText("")
    buildFile.writeText(
      """
      plugins {
          id("io.github.goooler.golang")
      }

      tasks.named<io.github.goooler.golang.tasks.GoCompile>("compileGo") {
          environment.put("CGO_ENABLED", "0")
          outputFile.set(layout.buildDirectory.file("bin/main"))
      }
      """
        .trimIndent()
    )

    // Create a simple go file without C dependencies
    val goFile = projectDir.resolve("src/main/go/main.go")
    goFile.parentFile.mkdirs()
    goFile.writeText("package main\nfunc main() {}")

    val runner = GradleRunner.create()
    runner.forwardOutput()
    runner.withPluginClasspath()
    runner.withArguments("compileGo", "--stacktrace")
    runner.withProjectDir(projectDir)
    val result = runner.build()

    assertThat(result.output).contains("BUILD SUCCESSFUL")
    val task = result.task(":compileGo")
    assertThat(task).isNotNull()

    val outputFile = projectDir.resolve("build/bin/main")
    assertThat(outputFile).all { exists() }
  }
}
