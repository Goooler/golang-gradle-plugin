package io.github.goooler.golang

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/** Extension for configuring Android-specific Go compilation settings. */
public interface GoAndroidExtension {
  /** The Android NDK path. If not specified, will be auto-detected from Android SDK. */
  public val ndkPath: DirectoryProperty

  /** Target Android API level for compilation. */
  public val apiLevel: Property<Int>

  /** Target Android architectures (e.g., arm64-v8a, armeabi-v7a, x86, x86_64). */
  public val architectures: ListProperty<AndroidArchitecture>
}

/** Supported Android architectures for Go compilation. */
public enum class AndroidArchitecture(
  public val abiName: String,
  public val goArch: String,
  public val goArm: String? = null,
) {
  ARM64("arm64-v8a", "arm64"),
  ARM("armeabi-v7a", "arm", "7"),
  X86("x86", "386"),
  X86_64("x86_64", "amd64"),
}
