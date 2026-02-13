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

      // Create Android extension for Android-specific configurations
      val androidExtension =
        extensions.create(
          GoAndroidExtension::class.java,
          "goAndroid",
          DefaultGoAndroidExtension::class.java,
          objects,
        )

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

        val compileTask =
          tasks.register(sourceSet.getTaskName("compile", "Go"), GoCompile::class.java) {
            it.source(goSourceSet.go)
            it.outputFile.convention(layout.buildDirectory.file("go/bin/${sourceSet.name}"))
          }

        // Configure Android-specific environment if Android plugin is present
        plugins.withId("com.android.application") {
          configureAndroidCompile(compileTask.get(), androidExtension)
        }
        plugins.withId("com.android.library") {
          configureAndroidCompile(compileTask.get(), androidExtension)
        }
      }
    }

  private fun configureAndroidCompile(task: GoCompile, androidExtension: GoAndroidExtension) {
    // Set up Android NDK environment variables for cross-compilation
    task.doFirst {
      val ndkPath = androidExtension.ndkPath.orNull
      val apiLevel = androidExtension.apiLevel.orNull
      val architectures = androidExtension.architectures.get()

      if (ndkPath != null && apiLevel != null) {
        // Environment variables for Android NDK cross-compilation will be set
        // This is configured per-architecture when building for Android
        task.logger.info("Configuring Go compilation for Android NDK")
        task.logger.info("NDK Path: ${ndkPath.asFile.absolutePath}")
        task.logger.info("API Level: $apiLevel")
        task.logger.info("Architectures: ${architectures.joinToString { it.abiName }}")
      }
    }
  }
}

internal abstract class DefaultGoAndroidExtension
@javax.inject.Inject
constructor(objectFactory: org.gradle.api.model.ObjectFactory) : GoAndroidExtension {
  override val ndkPath = objectFactory.directoryProperty()
  override val apiLevel = objectFactory.property(Int::class.java)
  override val architectures =
    objectFactory
      .listProperty(AndroidArchitecture::class.java)
      .convention(listOf(AndroidArchitecture.ARM64, AndroidArchitecture.ARM))
}
