# Change Log


## [Unreleased](https://github.com/Goooler/golang-gradle-plugin/compare/0.3.0...HEAD) - 2026-xx-xx


## [0.3.0](https://github.com/Goooler/golang-gradle-plugin/releases/tag/0.3.0) - 2026-06-09

### Added

- Fail fast if Go is not installed.
- Log Go version before `GoCompile` starts.

### Changed

- Prioritize `PATH` over `GOROOT` for Go executable resolution.

## [0.2.2](https://github.com/Goooler/golang-gradle-plugin/releases/tag/0.2.2) - 2026-05-07

### Changed

- No need to convention destinationDir.
- Revert "Don't cache GoCompile for C_SHARED/C_ARCHIVE by default".

## [0.2.1](https://github.com/Goooler/golang-gradle-plugin/releases/tag/0.2.1) - 2026-04-29

### Fixed

- Fallback empty `abiFilters`.

## [0.2.0](https://github.com/Goooler/golang-gradle-plugin/releases/tag/0.2.0) - 2026-04-29

### Added

- Honor `abiFilters`.

## [0.1.4](https://github.com/Goooler/golang-gradle-plugin/releases/tag/0.1.4) - 2026-04-22

### Changed

- Revert changes in version `0.1.3`.

## [0.1.3](https://github.com/Goooler/golang-gradle-plugin/releases/tag/0.1.3) - 2026-04-22

### Fixed

- Fix header file not found when running `assembleRelease`.

## [0.1.2](https://github.com/Goooler/golang-gradle-plugin/releases/tag/0.1.2) - 2026-04-17

### Changed

- Skip redundant variant/flavor compile tasks.

## [0.1.1](https://github.com/Goooler/golang-gradle-plugin/releases/tag/0.1.1) - 2026-04-15


## [0.1.0](https://github.com/Goooler/golang-gradle-plugin/releases/tag/0.1.0) - 2026-03-05

### Added

- Initial support for Go compilation in Gradle.
