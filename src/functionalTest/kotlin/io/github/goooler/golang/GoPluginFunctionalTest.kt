package io.github.goooler.golang

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import org.junit.jupiter.api.Test

class GoPluginFunctionalTest : BaseFunctionalTest() {

  @Test
  fun `can run task`() {
    settingsFile.writeText("")
    buildFile.writeText(
      """
      plugins {
          id("java")
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
  fun `can run task with golang source dir`() {
    settingsFile.writeText("")
    buildFile.writeText(
      """
      plugins {
          id("java")
          id("io.github.goooler.golang")
      }
      """
        .trimIndent()
    )

    // Create a dummy go file in the golang directory
    val goFile = projectRoot.resolve("src/main/golang/main.go")
    goFile.createParentDirectories()
    goFile.writeText("package main\nfunc main() {}")

    val result = runWithSuccess("compileGo")

    assertThat(result.output).contains("BUILD SUCCESSFUL")
    val task = result.task(":compileGo")
    assertThat(task).isNotNull()
  }

  @Test
    settingsFile.writeText("")
    buildFile.writeText(
      """
      plugins {
          id("java")
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

  @Test
  fun `can configure packageName and buildTags`() {
    settingsFile.writeText("")
    buildFile.writeText(
      """
      plugins {
          id("java")
          id("io.github.goooler.golang")
      }

      go {
          packageName.set("example.com/myapp")
          buildTags.set(listOf("mytag"))
      }
      """
        .trimIndent()
    )

    // Create a dummy go.mod
    projectRoot.resolve("go.mod").writeText("module example.com/myapp\ngo 1.16")

    // Create a dummy go file
    val goFile = projectRoot.resolve("main.go")
    goFile.writeText(
      """
      //go:build mytag
      package main
      func main() {}
      """
        .trimIndent()
    )

    val result = runWithSuccess("compileGo")

    assertThat(result.output).contains("BUILD SUCCESSFUL")
    val task = result.task(":compileGo")
    assertThat(task).isNotNull()
  }

  @Test
  fun `task is skipped when no source files exist`() {
    settingsFile.writeText("")
    buildFile.writeText(
      """
      plugins {
          id("java")
          id("io.github.goooler.golang")
      }
      """
        .trimIndent()
    )

    // Don't create any .go files - leave the source directory empty
    val result = runWithSuccess("compileGo")

    assertThat(result.task(":compileGo")).isNotNull().transform { it.outcome }.isEqualTo(NO_SOURCE)
  }
}
