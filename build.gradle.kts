plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
  val userHome = System.getProperty("user.home")
  val isWindows = System.getProperty("os.name").lowercase().contains("win")
  if (isWindows) {
    layout.buildDirectory.set(file("$userHome/AppData/Local/AndroidBuild/ExamBox/${project.name}"))
  }
}

