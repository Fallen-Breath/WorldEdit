import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.PluginDependency

plugins {
    id("org.spongepowered.gradle.plugin")
    id("org.spongepowered.gradle.vanilla")
}

applyPlatformAndCoreConfiguration()
applyShadowConfiguration()

repositories {
    mavenCentral()
}

minecraft {
    version("1.16.5")
}

sponge {
    apiVersion("8.0.0")
    plugin("worldedit") {
        loader(PluginLoaders.JAVA_PLAIN)
        displayName("WorldEdit")
        version(project.ext["internalVersion"].toString())
        mainClass("com.sk89q.worldedit.sponge.SpongeWorldEdit")
        description("WorldEdit is an easy-to-use in-game world editor for Minecraft, supporting both single- and multi-player.")
        links {
            homepage("https://enginehub.org/worldedit/")
            source("https://github.com/EngineHub/WorldEdit")
            issues("https://github.com/EngineHub/WorldEdit/issues")
        }
        contributor("EngineHub") {
            description("Various members of the EngineHub team")
        }
        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}

dependencies {
    api(project(":worldedit-core"))
    api(project(":worldedit-libs:sponge"))
    api("org.spongepowered:spongeapi:8.0.0-SNAPSHOT")
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.14.1") {
        because("Sponge 8 (will?) provides Log4J at 2.14.1")
    })
    api("org.apache.logging.log4j:log4j-api")
    // bStats isn't updated yet :(
    // api("org.bstats:bstats-sponge:2.2.1")
    testImplementation("org.mockito:mockito-core:${Versions.MOCKITO}")
}

addJarManifest(WorldEditKind.Mod, includeClasspath = true)

tasks.named<ShadowJar>("shadowJar") {
    dependencies {
        relocate("org.bstats", "com.sk89q.worldedit.sponge.bstats") {
            include(dependency("org.bstats:bstats-sponge"))
        }
        include(dependency(":worldedit-core"))

        relocate("org.antlr.v4", "com.sk89q.worldedit.sponge.antlr4")
        include(dependency("org.antlr:antlr4-runtime"))
        relocate("it.unimi.dsi.fastutil", "com.sk89q.worldedit.sponge.fastutil") {
            include(dependency("it.unimi.dsi:fastutil"))
        }
    }
}
tasks.named("assemble").configure {
    dependsOn("shadowJar")
}
