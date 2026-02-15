package io.github.goooler.golang

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.exists
import assertk.assertions.isNotNull
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import org.junit.jupiter.api.Test

class GoPluginFunctionalTest : BaseFunctionalTest() {

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
    val goFile = projectRoot.resolve("src/main/go/main.go")
    goFile.createParentDirectories()
    goFile.writeText("package main\nfunc main() {}")

    val result = runWithSuccess("compileGo")

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
    val goFile = projectRoot.resolve("src/main/go/main.go")
    goFile.createParentDirectories()
    goFile.writeText("package main\nfunc main() {}")

    val result = runWithSuccess("compileGo")

    assertThat(result.output).contains("BUILD SUCCESSFUL")
    val task = result.task(":compileGo")
    assertThat(task).isNotNull()

    val outputFile = projectRoot.resolve("build/custom-output")
    assertThat(outputFile).all { exists() }
  }
}
