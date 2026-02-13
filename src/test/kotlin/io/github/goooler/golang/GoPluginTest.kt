package io.github.goooler.golang

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import io.github.goooler.golang.tasks.GoBuildMode
import io.github.goooler.golang.tasks.GoCompile
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class GoPluginTest {
  @Test
  fun `plugin registers task and sourceSet extension`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("io.github.goooler.golang")

    assertThat(project.tasks.findByName("compileGo")).isNotNull()

    val mainSourceSet =
      project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
    val goSourceSet = mainSourceSet.extensions.findByName("go")
    assertThat(goSourceSet).isNotNull().isInstanceOf(GoSourceSet::class)
  }

  @Test
  fun `plugin registers goAndroid extension`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("io.github.goooler.golang")

    val goAndroidExtension = project.extensions.findByName("goAndroid")
    assertThat(goAndroidExtension).isNotNull().isInstanceOf(GoAndroidExtension::class)
  }

  @Test
  fun `GoCompile task supports build mode configuration`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("io.github.goooler.golang")

    val compileTask = project.tasks.getByName("compileGo") as GoCompile
    compileTask.buildMode.set(GoBuildMode.C_SHARED)

    assertThat(compileTask.buildMode.get()).isEqualTo(GoBuildMode.C_SHARED)
  }

  @Test
  fun `GoCompile task supports environment variables`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("io.github.goooler.golang")

    val compileTask = project.tasks.getByName("compileGo") as GoCompile
    compileTask.environment.put("GOOS", "android")
    compileTask.environment.put("GOARCH", "arm64")

    assertThat(compileTask.environment.get()["GOOS"]).isEqualTo("android")
    assertThat(compileTask.environment.get()["GOARCH"]).isEqualTo("arm64")
  }

  @Test
  fun `GoAndroidExtension has default architectures`() {
    val project = ProjectBuilder.builder().build()
    project.plugins.apply("io.github.goooler.golang")

    val goAndroid = project.extensions.getByType(GoAndroidExtension::class.java)
    val defaultArchs = goAndroid.architectures.get()

    assertThat(defaultArchs.size).isEqualTo(2)
    assertThat(defaultArchs[0]).isEqualTo(AndroidArchitecture.ARM64)
    assertThat(defaultArchs[1]).isEqualTo(AndroidArchitecture.ARM)
  }
}
