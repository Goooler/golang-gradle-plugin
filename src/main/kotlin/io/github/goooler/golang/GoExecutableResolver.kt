package io.github.goooler.golang

import java.io.File
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

internal fun resolveGoExecutable(providerFactory: ProviderFactory): Provider<String> {
  return providerFactory.provider {
    val goRoot = System.getenv("GOROOT")
    val os = OS.current
    val goName = if (os == OS.WINDOWS) "go.exe" else "go"

    val candidates = mutableListOf<File>()
    if (!goRoot.isNullOrEmpty()) {
      candidates.add(File(goRoot, "bin/$goName"))
    }

    when (os) {
      OS.MACOS -> {
        candidates.add(File("/usr/local/go/bin/$goName"))
        candidates.add(File("/opt/homebrew/bin/$goName"))
      }
      OS.LINUX -> candidates.add(File("/usr/local/go/bin/$goName"))
      OS.WINDOWS -> candidates.add(File("C:\\Program Files\\Go\\bin\\$goName"))
    }

    candidates.firstOrNull { it.exists() && it.isFile && it.canExecute() }?.absolutePath ?: "go"
  }
}
