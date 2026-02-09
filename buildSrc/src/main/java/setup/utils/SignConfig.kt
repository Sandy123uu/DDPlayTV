package setup.utils

import com.android.build.gradle.internal.dsl.SigningConfig
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.util.Properties

object SignConfig {

    private const val RELEASE_KEYSTORE_FILE = "RELEASE_KEYSTORE_FILE"
    private const val KEYSTORE_PASS = "KEYSTORE_PASS"
    private const val ALIAS_NAME = "ALIAS_NAME"
    private const val ALIAS_PASS = "ALIAS_PASS"

    fun debug(project: Project, config: SigningConfig) {
        val properties = loadProperties(project) ?: return
        config.apply {
            storeFile(project.getAssembleFile(properties["KEY_LOCATION"].toString()))
            storePassword(properties[KEYSTORE_PASS].toString())
            keyAlias(properties[ALIAS_NAME].toString())
            keyPassword(properties[ALIAS_PASS].toString())

            enableV1Signing = true
        }
    }

    fun release(project: Project, config: SigningConfig) {
        val localProperties = loadLocalProperties(project)
        val keystoreFile = resolveReleaseKeystoreFile(project, localProperties)

        val storePassword = readReleaseCredential(KEYSTORE_PASS, localProperties)
        val aliasName = readReleaseCredential(ALIAS_NAME, localProperties)
        val aliasPassword = readReleaseCredential(ALIAS_PASS, localProperties)

        if (keystoreFile.exists() && storePassword != null && aliasName != null && aliasPassword != null) {
            config.apply {
                storeFile = keystoreFile
                storePassword(storePassword)
                keyAlias(aliasName)
                keyPassword(aliasPassword)
                enableV1Signing = true
            }
            return
        }

        val isCi =
            System.getenv("CI")?.equals("true", ignoreCase = true) == true ||
                System.getenv("GITHUB_ACTIONS")?.equals("true", ignoreCase = true) == true

        if (isCi && isReleaseSigningRequiredInCi(project)) {
            throw GradleException(
                "Release signing config not ready in CI: keystoreFile=${keystoreFile.path}, exists=${keystoreFile.exists()} env(RELEASE_KEYSTORE_FILE/KEYSTORE_PASS/ALIAS_NAME/ALIAS_PASS)=${hasEnvCredential(RELEASE_KEYSTORE_FILE)}/${hasEnvCredential(KEYSTORE_PASS)}/${hasEnvCredential(ALIAS_NAME)}/${hasEnvCredential(ALIAS_PASS)} local.properties(RELEASE_KEYSTORE_FILE/KEYSTORE_PASS/ALIAS_NAME/ALIAS_PASS)=${hasLocalCredential(RELEASE_KEYSTORE_FILE, localProperties)}/${hasLocalCredential(KEYSTORE_PASS, localProperties)}/${hasLocalCredential(ALIAS_NAME, localProperties)}/${hasLocalCredential(ALIAS_PASS, localProperties)}",
            )
        }

        project.logger.warn(
            "Release signing config not ready (missing keystore or signing credentials from env/local.properties), fallback to debug signing config",
        )
        debug(project, config)
    }

    private fun isReleaseSigningRequiredInCi(project: Project): Boolean {
        val taskNames = project.gradle.startParameter.taskNames
        if (taskNames.isEmpty()) return false

        return taskNames.any { fullName ->
            val name = fullName.substringAfterLast(':')
            name.equals("assemble", ignoreCase = true) ||
                name.equals("bundle", ignoreCase = true) ||
                name.contains("Release", ignoreCase = true) ||
                name.contains("Beta", ignoreCase = true)
        }
    }

    private fun loadProperties(project: Project): Properties? {
        var propertiesFile = project.getAssembleFile("keystore.properties")
        if (propertiesFile.exists().not()) {
            propertiesFile = project.getAssembleFile("debug.properties")
        }

        if (propertiesFile.exists().not()) return null

        return Properties().apply {
            FileInputStream(propertiesFile).use { load(it) }
        }
    }

    private fun loadLocalProperties(project: Project): Properties? {
        val localPropertiesFile = File(project.rootDir, "local.properties")
        if (localPropertiesFile.exists().not()) return null

        return Properties().apply {
            FileInputStream(localPropertiesFile).use { load(it) }
        }
    }

    private fun readReleaseCredential(key: String, localProperties: Properties?): String? {
        return System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: localProperties?.getProperty(key)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun resolveReleaseKeystoreFile(project: Project, localProperties: Properties?): File {
        val configuredPath = readReleaseCredential(RELEASE_KEYSTORE_FILE, localProperties)
            ?: "gradle/assemble/dandanplay.jks"

        val file = File(configuredPath)
        return if (file.isAbsolute) file else File(project.rootDir, configuredPath)
    }

    private fun hasEnvCredential(key: String): Boolean {
        return System.getenv(key)?.isNotBlank() == true
    }

    private fun hasLocalCredential(key: String, localProperties: Properties?): Boolean {
        return localProperties?.getProperty(key)?.trim()?.isNotBlank() == true
    }

    private fun Project.getAssembleFile(fileName: String): File {
        return File(rootDir, "gradle/assemble/$fileName")
    }
}
