package io.github.goooler.golang

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet

public interface GoSourceSet {
  public val go: SourceDirectorySet
  public val packageName: Property<String>
  public val buildTags: ListProperty<String>

  public fun go(action: Action<in SourceDirectorySet>)
}

internal abstract class DefaultGoSourceSet
@Inject
constructor(sourceSet: SourceSet, objectFactory: ObjectFactory) : GoSourceSet {
  override val go: SourceDirectorySet =
    objectFactory.sourceDirectorySet("go", "${sourceSet.name} Go source").apply {
      filter.include("**/*.go")
    }

  override val packageName: Property<String> = objectFactory.property(String::class.java)

  override val buildTags: ListProperty<String> = objectFactory.listProperty(String::class.java)

  override fun go(action: Action<in SourceDirectorySet>) {
    action.execute(go)
  }
}
