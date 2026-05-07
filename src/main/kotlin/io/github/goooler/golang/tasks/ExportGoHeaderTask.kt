package io.github.goooler.golang.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Not worth caching")
internal abstract class ExportGoHeaderTask
@Inject
constructor(private val fileSystemOperations: FileSystemOperations) : DefaultTask() {

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val headerFile: RegularFileProperty

  @get:OutputDirectory abstract val destinationDir: DirectoryProperty

  @TaskAction
  fun export() {
    fileSystemOperations.sync { spec ->
      spec.into(destinationDir)
      spec.from(headerFile)
    }
  }
}
