import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.common.util.RunConfig
import net.minecraftforge.gradle.userdev.UserDevExtension

plugins {
    alias(libs.plugins.licenser)
    alias(libs.plugins.minecraftForge)
    alias(libs.plugins.librarian)
    alias(libs.plugins.mixin)
    alias(libs.plugins.shadow)
    java
}

tasks
    .withType<JavaCompile>()
    .matching { it.name == "compileJava" || it.name == "compileTestJava" }
    .configureEach {
        val disabledLint = listOf(
            "processing", "path", "fallthrough", "serial"
        )
        options.release.set(17)
        options.compilerArgs.addAll(listOf("-Xlint:all") + disabledLint.map { "-Xlint:-$it" })
        options.isDeprecation = true
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

license {
    ext {
        set("name", project.name)
        set("organization", project.property("organization"))
        set("url", project.property("url"))
    }
    header(rootProject.file("HEADER.txt"))
}

val minecraftVersion = libs.versions.minecraft.get()
val nextMajorMinecraftVersion: String = minecraftVersion.split('.').let { (useless, major) ->
    "$useless.${major.toInt() + 1}"
}
val forgeVersion = libs.versions.forge.get()

configurations.create("explicitDependencies") {
    isCanBeResolved = true
}
configurations["implementation"].extendsFrom(configurations["explicitDependencies"])

dependencies {
    minecraft(libs.minecraftForge) {
        version {
            require("$minecraftVersion-$forgeVersion")
        }
    }

    "annotationProcessor"(variantOf(libs.mixin) { classifier("processor") })
}

configure<UserDevExtension> {
    mappings("parchment", "${libs.versions.parchment.get()}-$minecraftVersion")

    runs {
        val runConfig = Action<RunConfig> {
            properties(
                mapOf(
                    "forge.logging.markers" to "NETWORKING",
                    "forge.logging.console.level" to "debug",
                    "forge.logging.mojang.level" to "debug",
                )
            )
            workingDirectory = project.file("run/$name").canonicalPath
            lazyToken("minecraft_classpath") {
                configurations["explicitDependencies"].resolve().joinToString(File.pathSeparator) { it.absolutePath }
            }
            // Forge, why would you hurt Gradle this way?
            forceExit(false)
        }
        val modRunConfig = Action<RunConfig> {
            runConfig(this)

            mods {
                create("totally_safe_backup") {
                    source(sourceSets["main"])
                }
            }
        }
        create("client", modRunConfig)
        create("server") {
            modRunConfig(this)
        }
    }
}

configure<org.spongepowered.asm.gradle.plugins.MixinExtension> {
    add(sourceSets["main"], "totally_safe_backup.mixins.refmap.json")
    config("totally_safe_backup.mixins.json")
}

configure<BasePluginExtension> {
    archivesName.set("${project.name}-mc$minecraftVersion")
}

tasks.named<Copy>("processResources") {
    // this will ensure that this task is redone when the versions change.
    val properties = mapOf(
        "file" to mapOf("jarVersion" to project.version),
        "forgeVersion" to forgeVersion,
        "minecraftVersion" to minecraftVersion,
        "nextMajorMinecraftVersion" to nextMajorMinecraftVersion
    )
    properties.forEach { (key, value) ->
        inputs.property(key, value)
    }

    filesMatching("META-INF/mods.toml") {
        expand(properties)
    }
}

tasks.register<ShadowJar>("shadowModJar") {
    dependsOn("reobfJar")
    from(zipTree(tasks.named<Jar>("jar").flatMap { it.archiveFile }))
    archiveBaseName.set(project.the<BasePluginExtension>().archivesName)
    archiveClassifier.set("dist")
    configurations = listOf(project.configurations["explicitDependencies"])

    minimize()
}
reobf.create("shadowModJar")
