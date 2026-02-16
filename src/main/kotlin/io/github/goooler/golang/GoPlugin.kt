package io.github.goooler.golang

import io.github.goooler.golang.tasks.GoCompile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer

public abstract class GoPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit =
    with(project) {
      val goExtension = extensions.create("golang", GoExtension::class.java)

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
            it.outputFile.convention(layout.buildDirectory.file("go/bin/${sourceSet.name}"))
          }
        }
      }

      pluginManager.withPlugin("com.android.base") { configureAndroidVariants(goExtension) }
    }

  public companion object {
    @JvmStatic
    public fun outputDirOf(
      project: Project,
      variantName: String? = null,
      abiName: String? = null,
    ): Provider<Directory> {
      val buildDirectory = project.layout.buildDirectory
      return if (variantName != null && abiName != null) {
        buildDirectory.dir("intermediates/go/$variantName/$abiName")
      } else {
        buildDirectory.dir("intermediates/go")
      }
    }
  }
}
