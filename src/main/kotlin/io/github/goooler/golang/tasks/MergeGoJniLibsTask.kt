package io.github.goooler.golang.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Merging JNI libs is fast and not worth caching yet")
internal abstract class MergeGoJniLibsTask
@Inject
constructor(private val fileSystemOperations: FileSystemOperations) : DefaultTask() {

  @get:Input abstract val abis: ListProperty<String>

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val libraryFiles: ListProperty<RegularFile>

  @get:OutputDirectory abstract val destinationDir: DirectoryProperty

  @TaskAction
  fun merge() {
    val abisList = abis.get()
    val filesList = libraryFiles.get()
    check(abisList.size == filesList.size) {
      "Number of ABIs (${abisList.size}) does not match number of files (${filesList.size})"
    }
    fileSystemOperations.sync { spec ->
      spec.into(destinationDir)
      abisList.zip(filesList).forEach { (abi, file) -> spec.from(file) { it.into(abi) } }
    }
  }
}
