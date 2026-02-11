package io.github.goooler.golang

import io.github.goooler.golang.tasks.GoCompile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

public abstract class GoPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
    with(project) {
      plugins.apply(JavaBasePlugin::class.java)

      val sourceSets =
        extensions.getByType(SourceSetContainer::class.java).apply {
          maybeCreate(SourceSet.MAIN_SOURCE_SET_NAME)
          maybeCreate(SourceSet.TEST_SOURCE_SET_NAME)
        }

      sourceSets.configureEach { sourceSet ->
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
          it.source(goSourceSet.go)
          it.outputFile.convention(layout.buildDirectory.file("go/bin/${sourceSet.name}"))
        }
      }
    }
}
