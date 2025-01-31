package ru.mipt.npm.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

public open class KScienceCommonPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.configureKScience(
        KotlinVersion(1, 7, 0)
    )
}
