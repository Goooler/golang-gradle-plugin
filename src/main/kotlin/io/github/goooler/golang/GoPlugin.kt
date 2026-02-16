package io.github.goooler.golang

import io.github.goooler.golang.tasks.GoCompile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

public abstract class GoPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
    with(project) {
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
              .apply { go.srcDir("src/${sourceSet.name}/go") }

          tasks.register(sourceSet.getTaskName("compile", "Go"), GoCompile::class.java) {
            it.buildMode.convention(GoBuildMode.EXE)
            it.source(goSourceSet.go)
            it.outputFile.convention(layout.buildDirectory.file("go/bin/${sourceSet.name}"))
          }
        }
      }

      pluginManager.withPlugin("com.android.base") { configureAndroidVariants() }
    }
}
