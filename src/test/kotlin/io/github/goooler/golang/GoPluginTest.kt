package io.github.goooler.golang

import assertk.assertThat
import assertk.assertions.isNotNull
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class GoPluginTest {
  @Test
  fun `plugin registers task`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply(JavaPlugin::class.java)
    project.plugins.apply("io.github.goooler.golang")

    assertThat(project.tasks.findByName("compileGo")).isNotNull()
  }
}
