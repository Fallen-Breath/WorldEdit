import net.fabricmc.loom.task.RemapJarTask

plugins {
    base
}

applyCommonConfiguration()

tasks.register<Jar>("jar") {
    val remapFabric = project(":worldedit-fabric").tasks.named<RemapJarTask>("remapShadowJar")
    dependsOn(
        remapFabric
    )
    from(zipTree({remapFabric.get().archiveFile}))

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("dist")
}

tasks.named("assemble") {
    dependsOn("jar")
}
