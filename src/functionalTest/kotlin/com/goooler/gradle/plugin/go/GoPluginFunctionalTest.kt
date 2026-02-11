package com.goooler.gradle.plugin.go

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GoPluginFunctionalTest {

  @field:TempDir lateinit var projectDir: File

  private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
  private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

  @Test
  fun `can run task`() {
    settingsFile.writeText("")
    buildFile.writeText(
      """
      plugins {
          id("com.goooler.gradle.plugin.go")
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
    // Task might be UP-TO-DATE or SUCCESS depending on things, but initially it should be SUCCESS
    // Actually, executed task is what we care about.
  }
}
