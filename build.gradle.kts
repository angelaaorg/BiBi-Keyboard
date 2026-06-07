// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
