package io.github.goooler.golang

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet

public interface GoSourceSet {
  public val go: SourceDirectorySet

  public fun go(action: Action<in SourceDirectorySet>)
}

internal class DefaultGoSourceSet
@Inject
constructor(sourceSet: SourceSet, objectFactory: ObjectFactory) : GoSourceSet {
  override val go: SourceDirectorySet =
    objectFactory.sourceDirectorySet("go", "${sourceSet.name} Go source").apply {
      filter.include("**/*.go")
    }

  override fun go(action: Action<in SourceDirectorySet>) {
    action.execute(go)
  }
}
