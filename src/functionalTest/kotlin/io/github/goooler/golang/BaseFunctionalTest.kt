package io.github.goooler.golang

import assertk.assertThat
import assertk.assertions.doesNotContain
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseFunctionalTest {
  @TempDir
  lateinit var projectRoot: Path
    private set

  val buildFile: Path
    get() = projectRoot.resolve("build.gradle.kts")

  val settingsFile: Path
    get() = projectRoot.resolve("settings.gradle.kts")

  fun runWithSuccess(vararg arguments: String, block: GradleRunner.() -> Unit = {}): BuildResult {
    return runner(arguments = arguments.toList(), block = block)
      .build()
      .assertNoDeprecationWarnings()
  }

  fun runWithFailure(vararg arguments: String, block: GradleRunner.() -> Unit = {}): BuildResult {
    return runner(arguments = arguments.toList(), block = block)
      .buildAndFail()
      .assertNoDeprecationWarnings()
  }

  fun BuildResult.assertNoDeprecationWarnings() = apply {
    assertThat(output)
      .doesNotContain(
        "has been deprecated and is scheduled to be removed in Gradle",
        "has been deprecated. This is scheduled to be removed in Gradle",
        "will fail with an error in Gradle",
      )
  }

  private fun runner(arguments: Iterable<String>, block: GradleRunner.() -> Unit): GradleRunner {
    return gradleRunner(
      projectDir = projectRoot,
      arguments = commonGradleArgs + arguments,
      warningsAsErrors = false, // TODO: enable it after bumping AGP to 9.0+.
      block = block,
    )
  }

  private fun gradleRunner(
    projectDir: Path,
    arguments: Iterable<String>,
    warningsAsErrors: Boolean = true,
    block: GradleRunner.() -> Unit = {},
  ): GradleRunner =
    GradleRunner.create()
      .withGradleVersion(testGradleVersion)
      .forwardOutput()
      .withPluginClasspath()
      .withTestKitDir(testKitDir.toFile())
      .withArguments(
        buildList {
          addAll(arguments)
          if (warningsAsErrors) {
            add("--warning-mode=fail")
          }
        }
      )
      .withProjectDir(projectDir.toFile())
      .apply(block)
}

private val commonGradleArgs =
  setOf(
    "--configuration-cache",
    "--build-cache",
    "--parallel",
    "--stacktrace",
    // https://docs.gradle.org/current/userguide/configuration_cache.html#config_cache:usage:parallel
    "-Dorg.gradle.configuration-cache.parallel=true",
  )

private val testGradleVersion by lazy {
  System.getProperty("TEST_GRADLE_VERSION")
    ?: error("TEST_GRADLE_VERSION system property is not set.")
}

private val testKitDir by lazy {
  val gradleUserHome =
    System.getenv("GRADLE_USER_HOME")
      ?: Path(System.getProperty("user.home"), ".gradle").absolutePathString()
  Path(gradleUserHome, "testkit")
}
