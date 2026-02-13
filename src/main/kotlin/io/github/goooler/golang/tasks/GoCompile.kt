package io.github.goooler.golang.tasks

import javax.inject.Inject
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Go build is not cacheable yet")
public abstract class GoCompile @Inject constructor(private val execOperations: ExecOperations) :
  SourceTask() {

  @get:Input public abstract val compilerArgs: ListProperty<String>

  @get:Input @get:Optional public abstract val buildMode: Property<GoBuildMode>

  @get:Input @get:Optional public abstract val environment: MapProperty<String, String>

  @get:OutputFile public abstract val outputFile: RegularFileProperty

  @TaskAction
  public fun compile() {
    val output = outputFile.get().asFile.absolutePath
    val buildModeArgs = buildMode.orNull?.let { listOf("-buildmode=${it.mode}") } ?: emptyList()

    execOperations
      .exec { spec ->
        spec.executable("go")
        spec.args(
          listOf("build", "-o", output) +
            buildModeArgs +
            compilerArgs.get() +
            source.files.map { it.absolutePath }
        )

        // Apply environment variables for cross-compilation (e.g., for Android NDK)
        environment.orNull?.let { env ->
          env.forEach { (key, value) -> spec.environment(key, value) }
        }
      }
      .assertNormalExitValue()
  }
}

/**
 * Represents Go build modes.
 *
 * @property mode The build mode flag value for the go build command
 */
public enum class GoBuildMode(public val mode: String) {
  /** Default build mode - builds an executable */
  DEFAULT("default"),

  /** Build a C archive file */
  C_ARCHIVE("c-archive"),

  /** Build a C shared library */
  C_SHARED("c-shared"),

  /** Build a Go shared library */
  SHARED("shared"),

  /** Build a Go plugin */
  PLUGIN("plugin"),
}
