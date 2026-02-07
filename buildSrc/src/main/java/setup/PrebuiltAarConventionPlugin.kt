package setup

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

open class PrebuiltAarExtension {
    var aarFileName: String? = null
    var configurationName: String = DEFAULT_CONFIGURATION_NAME

    companion object {
        const val DEFAULT_CONFIGURATION_NAME = "default"
    }
}

class PrebuiltAarConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("prebuiltAar", PrebuiltAarExtension::class.java)

        target.afterEvaluate {
            val configurationName = extension.configurationName.ifBlank {
                PrebuiltAarExtension.DEFAULT_CONFIGURATION_NAME
            }
            val aarFileName = extension.aarFileName?.takeIf { it.isNotBlank() }
                ?: throw GradleException(
                    "prebuiltAar.aarFileName must be configured for project '${target.path}'"
                )
            val aarFile = target.file(aarFileName)
            if (!aarFile.exists()) {
                throw GradleException("AAR file does not exist: ${aarFile.path}")
            }

            target.configurations.maybeCreate(configurationName)
            target.artifacts.add(configurationName, aarFile)
        }
    }
}
