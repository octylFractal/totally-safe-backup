pluginManagement {
    resolutionStrategy {
        val pluginMap = mapOf(
            "net.minecraftforge.gradle" to "net.minecraftforge.gradle:ForgeGradle",
            "org.spongepowered.mixin" to "org.spongepowered:mixingradle",
        )
        eachPlugin {
            pluginMap[requested.id.id]?.let { module ->
                useModule("$module:${requested.version}")
            }
        }
    }
    repositories {
        maven {
            name = "Forge"
            url = uri("https://maven.minecraftforge.net")
            content {
                includeGroupByRegex("net\\.minecraftforge($|\\..*)")
            }
        }
        maven {
            name = "Parchment"
            url = uri("https://maven.parchmentmc.org")
            content {
                includeGroupByRegex("org\\.parchmentmc($|\\..*)")
            }
        }
        maven {
            name = "sponge"
            url = uri("https://repo.spongepowered.org/maven")
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "totally-safe-backup"
