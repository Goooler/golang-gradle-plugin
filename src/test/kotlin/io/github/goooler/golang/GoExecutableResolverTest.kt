package io.github.goooler.golang

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.File
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GoExecutableResolverTest {

  @TempDir lateinit var tempDir: File

  @Test
  fun `resolves go from PATH first even if GOROOT is set`() {
    val os = OS.current
    val goName = if (os == OS.WINDOWS) "go.exe" else "go"

    val pathDir = File(tempDir, "path-bin").apply { mkdirs() }
    val pathGo =
      File(pathDir, goName).apply {
        createNewFile()
        setExecutable(true)
      }

    val gorootDir = File(tempDir, "goroot-go").apply { mkdirs() }
    val gorootGo =
      File(gorootDir, "bin/$goName").apply {
        parentFile.mkdirs()
        createNewFile()
        setExecutable(true)
      }

    val envVars =
      mapOf(
        "PATH" to pathDir.absolutePath,
        "GOROOT" to gorootDir.absolutePath,
      )

    val project = ProjectBuilder.builder().build()
    val testProviders = createMockProviderFactory(project.providers, envVars)

    val resolved = resolveGoExecutable(testProviders).get()
    assertThat(resolved).isEqualTo(pathGo.absolutePath)
  }

  @Test
  fun `resolves go from GOROOT if not found in PATH`() {
    val os = OS.current
    val goName = if (os == OS.WINDOWS) "go.exe" else "go"

    val gorootDir = File(tempDir, "goroot-go").apply { mkdirs() }
    val gorootGo =
      File(gorootDir, "bin/$goName").apply {
        parentFile.mkdirs()
        createNewFile()
        setExecutable(true)
      }

    val envVars =
      mapOf(
        "PATH" to "",
        "GOROOT" to gorootDir.absolutePath,
      )

    val project = ProjectBuilder.builder().build()
    val testProviders = createMockProviderFactory(project.providers, envVars)

    val resolved = resolveGoExecutable(testProviders).get()
    assertThat(resolved).isEqualTo(gorootGo.absolutePath)
  }

  private fun createMockProviderFactory(
    delegate: ProviderFactory,
    envVars: Map<String, String>,
  ): ProviderFactory {
    return object : ProviderFactory by delegate {
      override fun environmentVariable(variableName: String): Provider<String> {
        return delegate.provider { envVars[variableName] }
      }

      override fun environmentVariable(variableName: Provider<String>): Provider<String> {
        return delegate.provider { envVars[variableName.orNull] }
      }
    }
  }
}
