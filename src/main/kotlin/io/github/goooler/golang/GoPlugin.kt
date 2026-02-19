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
        }

      // org.gradle.api.plugins.JavaBasePlugin
      plugins.withId("org.gradle.java-base") {
        extensions.getByType(SourceSetContainer::class.java).configureEach { sourceSet ->
          val goSourceDirectorySet =
            objects.sourceDirectorySet("go", "${sourceSet.name} Go source").apply {
              srcDir(
                provider {
                  val goDir = layout.projectDirectory.dir("src/${sourceSet.name}/go").asFile
                  val golangDir = layout.projectDirectory.dir("src/${sourceSet.name}/golang").asFile
                  when {
                    goDir.exists() -> goDir
                    golangDir.exists() -> golangDir
                    else -> goDir
                  }
                }
              )
              filter.include("**/*.go")
            }

          tasks.register(sourceSet.getTaskName("compile", "Go"), GoCompile::class.java) { task ->
            task.source(goSourceDirectorySet)
            task.workingDir.convention(
              goExtension.workingDir.orElse(
                layout.projectDirectory.dir(provider { goSourceDirectorySet.srcDirs.first().path })
              )
            )
            task.buildMode.convention(goExtension.buildMode.orElse(GoBuildMode.EXE))
            task.packageName.convention(goExtension.packageName)
            task.buildTags.convention(goExtension.buildTags)
            task.compilerArgs.convention(goExtension.compilerArgs)
            task.executable.convention(goExtension.executable)
            task.outputFileName.convention(goExtension.outputFileName.orElse(project.name))
            task.outputFile.convention(
              layout.buildDirectory.zip(task.outputFileName) { build, fileName ->
                build.file("go/bin/${sourceSet.name}/$fileName")
              }
            )
          }
        }
      }

      pluginManager.withPlugin("com.android.base") { configureAndroidVariants(goExtension) }
    }
}
