package io.github.goooler.golang.tasks

import io.github.goooler.golang.GoBuildMode
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

@CacheableTask
public abstract class GoCompile @Inject constructor(private val execOperations: ExecOperations) :
  SourceTask() {

  @get:Input public abstract val compilerArgs: ListProperty<String>
  @get:Input public abstract val environment: MapProperty<String, String>
  @get:Input public abstract val buildMode: Property<GoBuildMode>
  @get:Input @get:Optional public abstract val packageName: Property<String>
  @get:Input public abstract val buildTags: ListProperty<String>
  @get:Input public abstract val executable: Property<String>
  @get:Input public abstract val outputFileName: Property<String>
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  public abstract val workingDir: DirectoryProperty
  @get:OutputFile public abstract val outputFile: RegularFileProperty
  @get:OutputFile @get:Optional public abstract val outputHeaderFile: RegularFileProperty

  @TaskAction
  public fun compile() {
    failFast()
    logGoVersion()

    val output = outputFile.get().asFile.absolutePath
    val tags = buildTags.get()
    val pkg = packageName.orNull
    execOperations
      .exec { spec ->
        spec.environment(environment.get())
        spec.executable(executable.get())
        spec.workingDir(workingDir.get())
        val args = mutableListOf("build", "-buildmode=${buildMode.get().mode}")
        if (tags.isNotEmpty()) {
          args.add("-tags")
          args.add(tags.joinToString(","))
        }
        args.add("-o")
        args.add(output)
        args.addAll(compilerArgs.get())
        if (pkg != null) {
          args.add(pkg)
        } else {
          args.addAll(source.files.map { it.absolutePath })
        }
        spec.args(args)
      }
      .assertNormalExitValue()
  }

  private fun failFast() {
    val installTip = "Please install Go: https://go.dev/dl/"
    val executablePath =
      executable.orNull
        ?: throw GradleException("Go is not installed or could not be found. $installTip")
    val goFile = File(executablePath)
    val isPath = goFile.isAbsolute || executablePath.contains(File.pathSeparator)
    if (isPath && !goFile.canExecute()) {
      throw GradleException(
        "Go executable not found or not executable at '$executablePath'. $installTip"
      )
    }
  }

  private fun logGoVersion() {
    val outputStream = ByteArrayOutputStream()
    execOperations
      .exec { spec ->
        spec.environment(environment.get())
        spec.executable(executable.get())
        spec.workingDir(workingDir.get())
        spec.args("version")
        spec.standardOutput = outputStream
      }
      .assertNormalExitValue()
    val version = outputStream.toString().trim()
    logger.lifecycle("Using $version for compiling")
  }

  public companion object {
    @JvmStatic
    public val Project.baseOutputDir: Provider<Directory>
      get() = layout.buildDirectory.dir("intermediates/go")
  }
}
