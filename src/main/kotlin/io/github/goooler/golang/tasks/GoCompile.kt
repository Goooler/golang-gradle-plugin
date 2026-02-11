package io.github.goooler.golang.tasks

import javax.inject.Inject
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Go build is not cacheable yet")
public abstract class GoCompile @Inject constructor(private val execOperations: ExecOperations) :
  SourceTask() {

  @get:Input public abstract val compilerArgs: ListProperty<String>
  @get:OutputFile public abstract val outputFile: RegularFileProperty

  @TaskAction
  public fun compile() {
    val output = outputFile.get().asFile.absolutePath
    execOperations
      .exec { spec ->
        spec.executable("go")
        spec.args(
          listOf("build", "-o", output) + compilerArgs.get() + source.files.map { it.absolutePath }
        )
      }
      .assertNormalExitValue()
  }
}
