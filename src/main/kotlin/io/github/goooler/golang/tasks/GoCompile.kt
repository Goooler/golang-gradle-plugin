package io.github.goooler.golang.tasks

import io.github.goooler.golang.GoBuildMode
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
  @get:Input public abstract val environment: MapProperty<String, String>
  @get:Input public abstract val buildMode: Property<GoBuildMode>
  @get:Input @get:Optional public abstract val packageName: Property<String>
  @get:Input public abstract val buildTags: ListProperty<String>
  @get:Input public abstract val goExecutable: Property<String>
  @get:OutputFile public abstract val outputFile: RegularFileProperty

  @TaskAction
  public fun compile() {
    val output = outputFile.get().asFile.absolutePath
    val tags = buildTags.get()
    val pkg = packageName.orNull
    execOperations
      .exec { spec ->
        spec.environment(environment.get())
        spec.executable(goExecutable.get())
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
}
