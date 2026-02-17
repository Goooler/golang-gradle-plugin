package io.github.goooler.golang

import java.io.File
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.os.OperatingSystem

internal object GoExecutableResolver {
  fun resolve(providerFactory: ProviderFactory): Provider<String> {
    return providerFactory.provider {
      val goRoot = System.getenv("GOROOT")
      val os = OperatingSystem.current()
      val goName = if (os.isWindows) "go.exe" else "go"

      val candidates = mutableListOf<File>()
      if (!goRoot.isNullOrEmpty()) {
        candidates.add(File(goRoot, "bin/$goName"))
      }

      if (os.isMacOsX) {
        candidates.add(File("/usr/local/go/bin/$goName"))
        candidates.add(File("/opt/homebrew/bin/$goName"))
      } else if (os.isLinux) {
        candidates.add(File("/usr/local/go/bin/$goName"))
      } else if (os.isWindows) {
        candidates.add(File("C:\\Program Files\\Go\\bin\\$goName"))
      }

      candidates.firstOrNull { it.exists() && it.canExecute() }?.absolutePath ?: "go"
    }
  }
}
