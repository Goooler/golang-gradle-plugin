package io.github.goooler.golang.internal

import com.android.build.api.variant.AndroidComponentsExtension
import io.github.goooler.golang.GoBuildMode
import io.github.goooler.golang.tasks.GoCompile
import java.io.File
import java.util.Locale
import org.gradle.api.Project

internal object AndroidConfigurer {
  fun configure(project: Project) {
    val androidComponents =
      project.extensions.findByType(AndroidComponentsExtension::class.java) ?: return

    androidComponents.onVariants { variant ->
      val compileTasks =
        AndroidArch.values.map { abi ->
          val taskName =
            "compileGo${variant.name.replaceFirstChar { it.uppercase() }}${abi.replace("-", "").replaceFirstChar { it.uppercase() }}"
          val task =
            project.tasks.register(taskName, GoCompile::class.java) { task ->
              task.buildMode.convention(GoBuildMode.C_SHARED)
              task.environment.set(
                androidComponents.sdkComponents.ndkDirectory.map { ndkDir ->
                  mapOf(
                    "CGO_ENABLED" to "1",
                    "GOOS" to "android",
                    "GOARCH" to
                      when (abi) {
                        "arm64-v8a" -> "arm64"
                        "armeabi-v7a" -> "arm"
                        "x86" -> "386"
                        "x86_64" -> "amd64"
                        else -> error("Unsupported ABI: $abi")
                      },
                    "CC" to
                      getClangPath(ndkDir.asFile, variant.minSdk.apiLevel, abi) +
                        if (System.getProperty("os.name").lowercase(Locale.US).contains("win"))
                          ".cmd"
                        else "",
                  )
                }
              )

              variant.sources.java?.let { javaSources ->
                task.source(
                  javaSources.all.map { directories ->
                    directories.map { it.asFile.parentFile.resolve("go") }
                  }
                )
              }

              task.outputFile.convention(
                project.layout.buildDirectory.file(
                  "intermediates/go/${variant.name}/$abi/lib${project.name}.so"
                )
              )
            }
          abi to task
        }

      val mergeTask =
        project.tasks.register(
          "mergeGoJniLibs${variant.name.replaceFirstChar { it.uppercase() }}",
          org.gradle.api.tasks.Sync::class.java,
        ) { sync ->
          compileTasks.forEach { (abi, task) -> sync.from(task) { it.into(abi) } }
          sync.into(project.layout.buildDirectory.dir("generated/go/jniLibs/${variant.name}"))
        }

      variant.sources.jniLibs?.addGeneratedSourceDirectory(mergeTask) {
        project.objects
          .directoryProperty()
          .value(project.layout.dir(project.provider { it.destinationDir }))
      }
      val mergeJniLibFolders =
        "merge${variant.name.replaceFirstChar { it.uppercase() }}JniLibFolders"
      project.tasks.configureEach {
        if (it.name == mergeJniLibFolders) {
          it.dependsOn(mergeTask)
        }
      }
    }
  }

  private fun getNdkHostOs(): String {
    val os = System.getProperty("os.name").lowercase(Locale.US)
    return when {
      os.contains("mac") -> "darwin-x86_64"
      os.contains("win") -> "windows-x86_64"
      else -> "linux-x86_64"
    }
  }

  private fun getClangPath(ndkDir: File, apiLevel: Int, abi: String): String {
    val host = getNdkHostOs()
    val prefix =
      when (abi) {
        "arm64-v8a" -> "aarch64-linux-android"
        "armeabi-v7a" -> "armv7a-linux-androideabi"
        "x86" -> "i686-linux-android"
        "x86_64" -> "x86_64-linux-android"
        else -> error("Unsupported ABI: $abi")
      }
    return ndkDir.resolve("toolchains/llvm/prebuilt/$host/bin/$prefix$apiLevel-clang").absolutePath
  }
}

internal enum class AndroidArch(val abi: String) {
  ARM64_V8A("arm64-v8a"),
  ARMEABI_V7A("armeabi-v7a"),
  X86("x86"),
  X86_64("x86_64");

  companion object {
    val values = entries.map { it.abi }
  }
}
