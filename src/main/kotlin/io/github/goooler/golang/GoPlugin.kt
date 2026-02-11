package io.github.goooler.golang

import io.github.goooler.golang.tasks.GoCompile
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JvmEcosystemPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

public abstract class GoPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.plugins.apply(JvmEcosystemPlugin::class.java)

    val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
    sourceSets.maybeCreate(SourceSet.MAIN_SOURCE_SET_NAME)
    sourceSets.maybeCreate(SourceSet.TEST_SOURCE_SET_NAME)

    sourceSets.configureEach { sourceSet ->
      val goSourceSet =
        sourceSet.extensions.create(
          GoSourceSet::class.java,
          "go",
          DefaultGoSourceSet::class.java,
          sourceSet,
          project.objects,
        )
      goSourceSet.go.srcDir("src/${sourceSet.name}/go")

      project.tasks.register(sourceSet.getTaskName("compile", "Go"), GoCompile::class.java) {
        it.source(goSourceSet.go)
        it.outputFile.convention(project.layout.buildDirectory.file("go/bin/${sourceSet.name}"))
      }
    }
  }
}

public open class DefaultGoSourceSet
@Inject
constructor(private val sourceSet: SourceSet, private val objectFactory: ObjectFactory) :
  GoSourceSet {
  override val go: SourceDirectorySet =
    objectFactory.sourceDirectorySet("go", "${sourceSet.name} Go source").apply {
      filter.include("**/*.go")
    }

  override fun go(action: Action<in SourceDirectorySet>) {
    action.execute(go)
  }
}
