package io.github.goooler.golang

import javax.inject.Inject
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

public abstract class GoExtension @Inject constructor(objects: ObjectFactory) {
  public val sourceSets: NamedDomainObjectContainer<GoSourceSet> =
    objects.domainObjectContainer(GoSourceSet::class.java) { name ->
      objects.newInstance(DefaultGoSourceSet::class.java, name)
    }

  public abstract val packageName: Property<String>
  public abstract val buildTags: ListProperty<String>
}
