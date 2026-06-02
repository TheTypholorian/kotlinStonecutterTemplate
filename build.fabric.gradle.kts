@file:Suppress("UnstableApiUsage")

import groovy.json.JsonOutput.prettyPrint
import io.github.klahap.dotenv.DotEnvBuilder

plugins {
    kotlin("jvm")

    //alias(libs.plugins.loom)

    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"

    //id("me.modmuss50.mod-publish-plugin") version "2.0.0-beta.1"
    id("io.github.klahap.dotenv") version "1.1.3"

    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
    id("dev.kikugie.postprocess.jsonlang")

    id("dev.isxander.modstitch.base") version "0.8.5"
}

fletchingTable {
    mixins.create("main") {
        mixin("default", "${project.property("id")}.mixins.json")
    }
}

modstitch {
    modLoaderVersion = property("deps.loader_version") as String
    minecraftVersion = sc.current.version

    parchment {
        property("deps.parchment")?.let {
            val (mc, mappings) = (it as String).split(':')
            minecraftVersion = mc
            mappingsVersion = mc
        }
    }

    metadata {
        modId = property("id") as String
        modName = property("name") as String
        modVersion = property("version") as String
        modGroup = property("group") as String

        property("authors")?.let { modAuthor = it as String }
        property("description")?.let { modDescription = it as String }
        property("license")?.let { modLicense = it as String }
        property("credits")?.let { modCredits = it as String }
    }

    mixin {
        configs.register(property("id") as String)
        addMixinsToModManifest = true
    }

    finalJarTask.configure {
        destinationDirectory.set(rootProject.file("build/libs/${project.version}"))
    }

    namedJarTask.configure {
        destinationDirectory.set(rootProject.file("build/devlibs/${project.version}"))
    }

    loom {
        fabricLoaderVersion = "0.19.2"
    }

    moddevgradle {
        property("deps.forge")?.let { forgeVersion = it as String }
        property("deps.neoform")?.let { neoFormVersion = it as String }
        property("deps.neoforge")?.let { neoForgeVersion = it as String }
        property("deps.mcp")?.let { mcpVersion = it as String }

        defaultRuns()
    }
}

val env = DotEnvBuilder.dotEnv {
    addFileIfExists("$rootDir/.env")
    addFileIfExists("$projectDir/.env")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks.named<ProcessResources>("processResources") {
    val props = HashMap<String, String>().apply {
        this["java_version"] = when {
            sc.current.parsed >= "1.20.5" -> "21"
            sc.current.parsed >= "1.18" -> "17"
            sc.current.parsed >= "1.17" -> "16"
            else -> "8"
        }
        this["id"] = project.property("id") as String
        this["name"] = project.property("name") as String
        this["version"] = project.property("version") as String
        this["authors"] = project.property("authors") as String
        this["description"] = project.property("description") as String
        this["credits"] = project.property("credits") as String
        this["license"] = project.property("license") as String

        this["minecraft_versions"] = (project.property("deps.minecraft_range") ?: project.property("deps.minecraft")) as String
        this["fabric_api_version"] = project.property("deps.fabric-api") as String
        this["yacl_version"] = project.property("deps.yacl") as String
        this["sodium_version"] = project.property("deps.sodium") as String
    }

    inputs.properties(props)

    filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml", "**/*.mixins.json")) {
        expand(props)
    }
}

version = "${property("version")}+${property("deps.minecraft")}-fabric"
base.archivesName = property("id") as String

jsonlang {
    languageDirectories = listOf("assets/${property("id")}/lang")
    prettyPrint = true
}

repositories {
    mavenLocal()
    maven("https://thedarkcolour.github.io/KotlinForForge/") { name = "KotlinForForge" }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }
    maven("https://maven.isxander.dev/releases") {
        name = "Xander Maven"
    }
    maven("https://maven.ryanhcode.dev/releases") {
        name = "RyanHCode Maven"
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net")
    }

    ivy {
        url = uri("https://github.com/TheTypholorian/big_shot_lib/releases/download")
        patternLayout {
            artifact("[revision]/[artifact]-[revision](-[classifier]).[ext]")
        }
        metadataSources {
            artifact()
        }
    }
}

tasks.withType<Javadoc>().configureEach {
    enabled = false
}

dependencies {
    modstitch.loom {
        modstitchModImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric-api")}")
        modstitchModImplementation(libs.flk)

        modstitchModCompileOnly("maven.modrinth:sodium:${property("deps.sodium")}")
        modstitchModCompileOnly(libs.bigShot)
        modstitchModCompileOnly("maven.modrinth:yacl:${property("deps.yacl")}")
        modstitchModCompileOnly(libs.modmenu)

        if (sc.current.version == "1.21") {
            include(libs.sableCompanionFabric)
            modstitchModApi(libs.sableCompanionFabric)
        }
    }
}

fabricApi {
    configureDataGeneration() {
        outputDirectory = file("$rootDir/src/main/generated")
        //client = true
    }
}

java {
    val javaCompat = when {
        sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
        sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
        sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
        else -> JavaVersion.VERSION_1_8
    }
    sourceCompatibility = javaCompat
    targetCompatibility = javaCompat
}

kotlin {
    jvmToolchain(
        when {
            sc.current.parsed >= "1.20.5" -> 21
            sc.current.parsed >= "1.18" -> 17
            sc.current.parsed >= "1.17" -> 16
            else -> 8
        }
    )
}

val additionalVersionsStr = findProperty("publish.additionalVersions") as String?
val additionalVersions: List<String> = additionalVersionsStr
    ?.split(",")
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?: emptyList()

/*
publishMods {
    file = tasks.remapJar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.remapSourcesJar.map { it.archiveFile.get() })

    type = STABLE
    displayName = "${property("name")} ${property("version")} for ${stonecutter.current.version} Fabric"
    version = "${property("version")}+${stonecutter.current.version}-fabric"
    changelog = ""
    //changelog = provider { rootProject.file("CHANGELOG.md").readText() }
    modLoaders.add("fabric")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = env["MODRINTH_TOKEN"]
        minecraftVersions.add(stonecutter.current.version)
        minecraftVersions.addAll(additionalVersions)
        requires("fabric-api", "fabric-language-kotlin", "big-shot-lib", "yacl")
    }

    /*
    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = env["CURSEFORGE_TOKEN"]
        minecraftVersions.add(stonecutter.current.version)
        minecraftVersions.addAll(additionalVersions)
        requires("fabric-api", "fabric-language-kotlin", "big-shot-lib", "yacl")
    }
     */
}
 */