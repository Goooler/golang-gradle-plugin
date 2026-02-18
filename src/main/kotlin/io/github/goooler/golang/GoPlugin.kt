package io.github.goooler.golang

import io.github.goooler.golang.tasks.GoCompile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

public abstract class GoPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
    with(project) {
      val goExtension =
        extensions.create("go", GoExtension::class.java).apply {
          executable.convention(resolveGoExecutable(providers))
          buildMode.convention(GoBuildMode.EXE)
          outputFileName.convention(
            buildMode.map { mode ->
              when (mode) {
                GoBuildMode.C_SHARED,
                GoBuildMode.SHARED,
                GoBuildMode.PLUGIN -> "lib$name.so"
                else -> name
              }
            }
          )
        }

      // org.gradle.api.plugins.JavaBasePlugin
      plugins.withId("org.gradle.java-base") {
        extensions.getByType(SourceSetContainer::class.java).configureEach { sourceSet ->
          val goSourceDirectorySet =
            objects.sourceDirectorySet("go", "${sourceSet.name} Go source").apply {
              srcDir("src/${sourceSet.name}/go")
              filter.include("**/*.go")
            }

          tasks.register(sourceSet.getTaskName("compile", "Go"), GoCompile::class.java) { task ->
            task.source(goSourceDirectorySet)
            task.buildMode.convention(goExtension.buildMode)
            task.packageName.convention(goExtension.packageName)
            task.buildTags.convention(goExtension.buildTags)
            task.compilerArgs.convention(goExtension.compilerArgs)
            task.executable.convention(goExtension.executable)
            task.outputFileName.convention(goExtension.outputFileName)
            val outputFile =
              layout.buildDirectory.zip(task.outputFileName) { dir, fileName ->
                dir.file("go/bin/${sourceSet.name}/$fileName")
              }
            task.outputFile.convention(outputFile)
          }
        }
      }

      pluginManager.withPlugin("com.android.base") { configureAndroidVariants(goExtension) }
    }
}
