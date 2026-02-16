package io.github.goooler.golang

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

public interface GoExtension {
  public val packageName: Property<String>
  public val buildTags: ListProperty<String>
}
