package io.github.goooler.golang.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Merging JNI libs is fast and not worth caching yet")
internal abstract class MergeGoJniLibsTask
@Inject
constructor(private val fileSystemOperations: FileSystemOperations) : DefaultTask() {

  @get:Nested // Can't use MapProperty instead, must wrap properties in a nested bean.
  abstract val libraryFiles: ListProperty<LibraryFile>

  @get:OutputDirectory abstract val destinationDir: DirectoryProperty

  @TaskAction
  fun merge() {
    fileSystemOperations.sync { spec ->
      spec.into(destinationDir)
      libraryFiles.get().forEach { (abi, file) -> spec.from(file) { it.into(abi) } }
    }
  }

  data class LibraryFile(
    @Input val abi: String,
    @InputFile @get:PathSensitive(PathSensitivity.RELATIVE) val file: Provider<RegularFileProperty>,
  )
}
