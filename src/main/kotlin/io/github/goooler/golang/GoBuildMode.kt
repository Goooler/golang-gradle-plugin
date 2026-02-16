package io.github.goooler.golang

public enum class GoBuildMode(public val mode: String) {
  EXE("exe"),
  SHARED("shared"),
  C_SHARED("c-shared"),
  C_ARCHIVE("c-archive"),
  ARCHIVE("archive"),
  PIE("pie"),
  PLUGIN("plugin"),
}
