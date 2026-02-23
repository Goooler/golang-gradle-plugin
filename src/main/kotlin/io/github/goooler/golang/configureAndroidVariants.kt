package io.github.goooler.golang

import com.android.build.api.variant.AndroidComponentsExtension
import io.github.goooler.golang.tasks.GoCompile
import io.github.goooler.golang.tasks.GoCompile.Companion.baseOutputDir
import io.github.goooler.golang.tasks.MergeGoJniLibsTask
import java.io.File
import java.util.Locale
import org.gradle.api.Project

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
        os.startsWith("win") -> WINDOWS
        os.contains("mac") -> MACOS
        else -> LINUX
      }
    }
  }
}

internal fun String.capitalize(): String = replaceFirstChar { it.titlecase(Locale.ROOT) }

internal fun Project.configureAndroidVariants(goExtension: GoExtension) {
  val androidComponents =
    extensions.getByType(AndroidComponentsExtension::class.java).apply {
      registerSourceType("go")
      registerSourceType("golang")
    }
  val ndkDirectory =
    androidComponents.sdkComponents.ndkDirectory.orElse(
      objects
        .directoryProperty()
        .fileProvider(
          providers
            .environmentVariable("ANDROID_NDK")
            .orElse(providers.environmentVariable("ANDROID_NDK_HOME"))
            .orElse(providers.environmentVariable("ANDROID_NDK_LATEST_HOME"))
            .map { File(it) }
        )
    )

  androidComponents.onVariants { variant ->
    val isRelease = variant.buildType.orEmpty().lowercase(Locale.ROOT) == "release"
    val compileTasks =
      AndroidArch.entries.associate { abi ->
        val taskName = "compileGo${variant.name.capitalize()}${abi.normalized.capitalize()}"
        val task =
          tasks.register(taskName, GoCompile::class.java) { task ->
            task.buildMode.convention(goExtension.buildMode.orElse(GoBuildMode.C_SHARED))
            task.packageName.convention(goExtension.packageName)
            task.buildTags.convention(goExtension.buildTags)
            task.compilerArgs.convention(
              goExtension.compilerArgs.map { args ->
                if (isRelease) args + listOf("-trimpath", "-ldflags", "-s -w") else args
              }
            )
            task.executable.convention(goExtension.executable)
            task.workingDir.convention(goExtension.workingDir)
            task.environment.convention(
              ndkDirectory.map { ndkDir ->
                mapOf(
                  "CGO_ENABLED" to "1",
                  "GOOS" to "android",
                  "GOARCH" to abi.toGoArch(),
                  "GOARM" to abi.toGoArm(),
                  "CC" to abi.toClangPath(ndkDir.asFile, variant.minSdk.apiLevel),
                )
              }
            )

            (variant.sources.java ?: variant.sources.kotlin)?.let { sources ->
              val goSourceDirs =
                sources.static.map { dirs ->
                  dirs.map { dir ->
                    val goDir = dir.asFile.resolveSibling("go")
                    val golangDir = dir.asFile.resolveSibling("golang")
                    when {
                      goDir.exists() -> goDir
                      golangDir.exists() -> golangDir
                      else -> goDir
                    }
                  }
                }
              val goSourceSet = variant.sources.getByName("go")
              val golangSourceSet = variant.sources.getByName("golang")
              var workingDirAdded = false
              goSourceDirs.get().forEach { selectedDir ->
                if (selectedDir.name == "golang") {
                  golangSourceSet.addStaticSourceDirectory(selectedDir.absolutePath)
                } else {
                  goSourceSet.addStaticSourceDirectory(selectedDir.absolutePath)
                }
                if (!workingDirAdded && selectedDir.exists()) {
                  task.workingDir.convention(
                    goExtension.workingDir.orElse(
                      layout.projectDirectory.dir(selectedDir.absolutePath)
                    )
                  )
                  workingDirAdded = true
                }
              }
              task.source(goSourceDirs)
            }

            task.outputFileName.convention(
              goExtension.outputFileName.orElse("lib${project.name}.so")
            )
            task.outputFile.convention(
              baseOutputDir.zip(task.outputFileName) { base, fileName ->
                base.file("${variant.name}/${abi.abi}/$fileName")
              }
            )
            task.outputHeaderFile.convention(
              baseOutputDir.zip(task.outputFileName) { base, fileName ->
                base.file("${variant.name}/${abi.abi}/${fileName.substringBeforeLast('.')}.h")
              }
            )
          }
        abi.abi to task
      }

    val mergeTask =
      tasks.register(
        "mergeGoJniLibs${variant.name.capitalize()}",
        MergeGoJniLibsTask::class.java,
      ) { merge ->
        val libraryFiles =
          compileTasks.map { (abi, task) ->
            MergeGoJniLibsTask.LibraryFile(abi, task.map { it.outputFile })
          }
        merge.libraryFiles.convention(libraryFiles)
        merge.destinationDir.convention(
          layout.buildDirectory.dir("generated/go/jniLibs/${variant.name}")
        )
      }

    variant.sources.jniLibs?.addGeneratedSourceDirectory(mergeTask) { it.destinationDir }

    tasks
      .named { it.startsWith("configureCMake") }
      .configureEach { ccm ->
        compileTasks.forEach { (abi, task) ->
          when {
            // `buildCMakeDebug[arm64-v8a]` for debug
            ccm.name.contains("Debug") && ccm.name.contains(abi) && task.name.contains("Debug") ->
              ccm.dependsOn(task)
            // `buildCMakeRelWithDebInfo[arm64-v8a]` for release
            ccm.name.contains("RelWithDebInfo") &&
              ccm.name.contains(abi) &&
              task.name.contains("Release") -> ccm.dependsOn(task)
          }
        }
      }
  }
}

private fun AndroidArch.toClangPath(ndkDir: File, apiLevel: Int): String {
  val host =
    when (OS.current) {
      OS.MACOS -> "darwin-x86_64" // TODO: support Apple Silicon if NDK supports it.
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

private fun AndroidArch.toGoArm(): String =
  when (this) {
    AndroidArch.ARMEABI_V7A -> "7"
    else -> ""
  }
