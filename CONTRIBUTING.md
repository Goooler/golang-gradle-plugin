# Contributing to golang-gradle-plugin

Thank you for your interest in contributing to golang-gradle-plugin! This guide will help you get started with development and contributing to this project.

## Development Commands

### Code Style
- **Check code style**: `./gradlew spotlessCheck`
- **Format code**: `./gradlew spotlessFormat`

This project uses [ktfmt](https://github.com/facebook/ktfmt) with Google style for Kotlin code formatting.

### Testing
- **Run unit tests**: `./gradlew test`
- **Run functional/integration tests**: `./gradlew functionalTest`

### API Management
- **Check API compatibility**: `./gradlew checkKotlinAbi`
- **Update API dumps**: `./gradlew updateKotlinAbi`

All public APIs must be explicitly documented and dumped to ensure API compatibility is maintained.

### Linting
- **Run lint checks**: `./gradlew lint`
- **Update lint baseline**: `./gradlew updateLintBaseline`

The lint baseline file is located at `gradle/lint-baseline.xml`.

## Contribution Guidelines

### For Bug Fixes
When fixing issues:
1. Ensure all tests pass before submitting your changes
2. Add regression tests for the related issues to prevent future regressions
3. Run the full test suite to verify your changes don't break existing functionality

### For New Features
When adding new features or APIs:
1. Ensure proper visibility modifiers are used (this project has explicit API mode enabled)
2. All public APIs must have explicit visibility modifiers and documentation
3. Update API dumps by running `./gradlew updateKotlinAbi` after adding public APIs
4. Add appropriate tests to cover the new functionality

### Before Submitting
Before submitting a pull request:
1. Run `./gradlew spotlessFormat` to format your code
2. Run `./gradlew spotlessCheck` to verify code style
3. Run `./gradlew test` to ensure unit tests pass
4. Run `./gradlew functionalTest` to ensure integration tests pass
5. Run `./gradlew checkKotlinAbi` to verify API compatibility
6. Run `./gradlew lint` to check for lint issues
7. Optionally, run `./gradlew build` to run compilation, tests, and standard verification tasks configured for the project

## Code Style Guidelines
- Follow the Google Kotlin Style Guide
- Use ktfmt for automatic formatting
- All public declarations must have explicit visibility modifiers
- Add KDoc comments for public APIs

