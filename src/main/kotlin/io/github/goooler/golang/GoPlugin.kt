package io.github.goooler.golang

import io.github.goooler.golang.tasks.GoCompile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

public abstract class GoPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
    with(project) {
      val goExtension =
        extensions.create("golang", GoExtension::class.java).apply {
          executable.convention(resolveGoExecutable(providers))
          outputFileName.convention("lib$name.so")
        }

      // org.gradle.api.plugins.JavaBasePlugin
      plugins.withId("org.gradle.java-base") {
        extensions.getByType(SourceSetContainer::class.java).configureEach { sourceSet ->
          val goSourceSet =
            sourceSet.extensions
              .create(
                GoSourceSet::class.java,
                "go",
                DefaultGoSourceSet::class.java,
                sourceSet,
                objects,
              )
              .apply {
                go.srcDir("src/${sourceSet.name}/go")
                packageName.convention(goExtension.packageName)
                buildTags.convention(goExtension.buildTags)
              }

          tasks.register(sourceSet.getTaskName("compile", "Go"), GoCompile::class.java) {
            it.buildMode.convention(GoBuildMode.EXE)
            it.source(goSourceSet.go)
            it.packageName.convention(goSourceSet.packageName)
            it.buildTags.convention(goSourceSet.buildTags)
            it.executable.convention(goExtension.executable)
            it.outputFileName.convention(goExtension.outputFileName)
            val outputFile =
              layout.buildDirectory.zip(it.outputFileName) { dir, fileName ->
                dir.file("go/bin/${sourceSet.name}/$fileName")
              }
            it.outputFile.convention(outputFile)
          }
        }
      }

      pluginManager.withPlugin("com.android.base") { configureAndroidVariants(goExtension) }
    }
}
