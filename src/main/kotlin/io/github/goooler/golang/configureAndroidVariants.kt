package io.github.goooler.golang

import com.android.build.api.variant.AndroidComponentsExtension
import io.github.goooler.golang.tasks.GoCompile
import java.io.File
import java.util.Locale
import org.gradle.api.Project
import org.gradle.api.tasks.Sync

internal fun String.capitalize(): String = replaceFirstChar { it.titlecase(Locale.ROOT) }

internal fun Project.configureAndroidVariants() {
  val androidComponents = extensions.getByType(AndroidComponentsExtension::class.java)
  val ndkDirectory = androidComponents.sdkComponents.ndkDirectory

  extensions.getByType(AndroidComponentsExtension::class.java).onVariants { variant ->
    val compileTasks =
      AndroidArch.entries.map { abi ->
        val taskName = "compileGo${variant.name.capitalize()}${abi.normalized.capitalize()}"
        val task =
          tasks.register(taskName, GoCompile::class.java) { task ->
            task.buildMode.convention(GoBuildMode.C_SHARED)
            task.environment.convention(
              ndkDirectory.map { ndkDir ->
                mapOf(
                  "CGO_ENABLED" to "1",
                  "GOOS" to "android",
                  "GOARCH" to abi.toGoArch(),
                  "CC" to abi.toClangPath(ndkDir.asFile, variant.minSdk.apiLevel),
                )
              }
            )

            (variant.sources.java ?: variant.sources.kotlin)?.let { sources ->
              task.source(
                sources.all.map { dirs -> dirs.map { it.asFile.parentFile.resolve("go") } }
              )
            }

            task.outputFile.convention(
              layout.buildDirectory.file(
                "intermediates/go/${variant.name}/${abi.abi}/lib${name}.so"
              )
            )
          }
        abi to task
      }

    val mergeTask =
      tasks.register("mergeGoJniLibs${variant.name.capitalize()}", Sync::class.java) { sync ->
        compileTasks.forEach { (abi, task) -> sync.from(task) { it.into(abi.abi) } }
        sync.into(layout.buildDirectory.dir("generated/go/jniLibs/${variant.name}"))
      }

    variant.sources.jniLibs?.addGeneratedSourceDirectory(mergeTask) {
      objects.directoryProperty().fileProvider(provider { it.destinationDir })
    }
    val mergeJniLibFolders = "merge${variant.name.capitalize()}JniLibFolders"
    tasks.configureEach {
      if (it.name == mergeJniLibFolders) {
        it.dependsOn(mergeTask)
      }
    }
  }
}

internal enum class AndroidArch(val abi: String, val normalized: String) {
  ARM64_V8A("arm64-v8a", "arm64"),
  ARMEABI_V7A("armeabi-v7a", "arm32"),
  X86("x86", "x86"),
  X86_64("x86_64", "x64");

  companion object {
    val values = entries.map { it.abi }
  }
}

internal enum class OS {
  WINDOWS,
  MACOS,
  LINUX;

  companion object {
    val current: OS = run {
      val os = System.getProperty("os.name").lowercase(Locale.US)
      when {
        os.contains("win") -> WINDOWS
        os.contains("mac") -> MACOS
        else -> LINUX
      }
    }
  }
}

private fun AndroidArch.toClangPath(ndkDir: File, apiLevel: Int): String {
  val host =
    when (OS.current) {
      OS.MACOS -> "darwin-x86_64"
      OS.WINDOWS -> "windows-x86_64"
      OS.LINUX -> "linux-x86_64"
    }
  val prefix =
    when (this) {
      AndroidArch.ARM64_V8A -> "aarch64-linux-android"
      AndroidArch.ARMEABI_V7A -> "armv7a-linux-androideabi"
      AndroidArch.X86 -> "i686-linux-android"
      AndroidArch.X86_64 -> "x86_64-linux-android"
    }
  return ndkDir.resolve("toolchains/llvm/prebuilt/$host/bin/$prefix$apiLevel-clang").absolutePath +
    if (OS.current == OS.WINDOWS) ".cmd" else ""
}

private fun AndroidArch.toGoArch(): String =
  when (this) {
    AndroidArch.ARM64_V8A -> "arm64"
    AndroidArch.ARMEABI_V7A -> "arm"
    AndroidArch.X86 -> "386"
    AndroidArch.X86_64 -> "amd64"
  }
