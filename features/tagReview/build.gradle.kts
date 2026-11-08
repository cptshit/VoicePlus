plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.navigation)
  implementation(projects.core.common)
  implementation(projects.core.strings)
  implementation(projects.core.data.api)
  implementation(libs.okhttp)
  implementation(libs.serialization.json)
}
