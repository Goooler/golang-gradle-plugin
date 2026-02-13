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
}
