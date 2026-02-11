package io.github.goooler.golang

import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.Input

public interface GoSourceSet {
  @get:Input public val go: SourceDirectorySet

  public fun go(action: Action<in SourceDirectorySet>)
}
