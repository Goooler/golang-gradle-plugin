package io.github.goooler.golang

import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet

public interface GoSourceSet {
  public val go: SourceDirectorySet

  public fun go(action: Action<in SourceDirectorySet>)
}
