package io.github.goooler.golang.tasks

import io.github.goooler.golang.GoBuildMode
import javax.inject.Inject
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.language.nativeplatform.tasks.AbstractNativeSourceCompileTask
import org.gradle.nativeplatform.toolchain.NativeToolChain
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Go build is not cacheable yet")
public abstract class GoCompile @Inject constructor(private val execOperations: ExecOperations) :
  AbstractNativeSourceCompileTask() {

  // Note: compilerArgs is inherited from AbstractNativeCompileTask as ListProperty<String>
  @get:Input public abstract val environment: MapProperty<String, String>
  @get:Input public abstract val buildMode: Property<GoBuildMode>
  @get:Input @get:Optional public abstract val packageName: Property<String>
  @get:Input public abstract val buildTags: org.gradle.api.provider.ListProperty<String>
  @get:OutputFile public abstract val outputFile: RegularFileProperty

  init {
    // Initialize toolChain and targetPlatform with empty/null values
    // as they are required by AbstractNativeCompileTask but not used by Go compilation
    toolChain.convention(null as NativeToolChain?)
    targetPlatform.convention(null)
    // Set objectFileDir to a default location (required by AbstractNativeCompileTask)
    objectFileDir.convention(project.layout.buildDirectory.dir("go/obj"))
  }

  @TaskAction
  public fun compile() {
    val output = outputFile.get().asFile.absolutePath
    val tags = buildTags.get()
    val pkg = packageName.orNull
    execOperations
      .exec { spec ->
        spec.environment(environment.get())
        spec.executable("go")
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
