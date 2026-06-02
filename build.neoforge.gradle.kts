import io.github.klahap.dotenv.DotEnvBuilder

plugins {
    kotlin("jvm")

    id("dev.kikugie.fletching-table.neoforge") version "0.1.0-alpha.22"

    id("me.modmuss50.mod-publish-plugin") version "2.0.0-beta.1"
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
    minecraftVersion = property("deps.minecraft") as String

    parchment {
        findProperty("deps.parchment")?.let {
            val (mc, mappings) = (it as String).split(':')
            minecraftVersion = mc
            mappingsVersion = mappings
        }
    }

    metadata {
        modId = property("id") as String
        modName = property("name") as String
        modVersion = property("version") as String
        modGroup = property("group") as String

        findProperty("authors")?.let { modAuthor = it as String }
        findProperty("description")?.let { modDescription = it as String }
        findProperty("license")?.let { modLicense = it as String }
        findProperty("credits")?.let { modCredits = it as String }
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
        findProperty("deps.forge")?.let { forgeVersion = it as String }
        findProperty("deps.neoform")?.let { neoFormVersion = it as String }
        findProperty("deps.neoforge")?.let { neoForgeVersion = it as String }
        findProperty("deps.mcp")?.let { mcpVersion = it as String }

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

tasks.withType<Jar> {
    destinationDirectory.set(rootProject.file("build/libs/${project.version}"))
}

val processResources = tasks.named<ProcessResources>("processResources") {
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

        this["minecraft_versions"] = (project.property("deps.minecraft_range") ?: "[${project.property("deps.minecraft")}]") as String
        this["yacl_version"] = project.property("deps.yacl") as String
        this["sodium_version"] = project.property("deps.sodium") as String
    }

    inputs.properties(props)

    filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml", "**/*.mixins.json")) {
        expand(props)
    }
}

version = "${property("version")}+${property("deps.minecraft")}-neoforge"
base.archivesName = property("id") as String

jsonlang {
    languageDirectories = listOf("assets/${property("id")}/lang")
    prettyPrint = true
}

repositories {
    mavenLocal()
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.ryanhcode.dev/releases")
    maven("https://maven.fabricmc.net")
    maven("https://maven.parchmentmc.org")

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

dependencies {
    modstitchModImplementation(libs.kff)

    modstitchModImplementation("maven.modrinth:sodium:${property("deps.sodium")}")
    modstitchModCompileOnly(libs.bigShot)
    modstitchModCompileOnly("maven.modrinth:yacl:${property("deps.yacl")}")

    if (sc.current.version == "1.21") {
        modstitchJiJ(libs.sableCompanion)
        modstitchModApi(libs.sableCompanion)
    }
}

tasks {
    jar {
        exclude("**/*.accesswidener")
    }
}

val additionalVersionsStr = findProperty("publish.additionalVersions") as String?
val additionalVersions: List<String> = additionalVersionsStr
    ?.split(",")
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?: emptyList()

publishMods {
    file = tasks.jar.map { it.archiveFile.get() }
    //additionalFiles.from(tasks.named<org.gradle.jvm.tasks.Jar>("sourcesJar").map { it.archiveFile.get() })

    type = STABLE
    displayName = "${property("name")} ${property("version")} for ${property("deps.minecraft") as String} NeoForge"
    version = "${property("version")}+${property("deps.minecraft") as String}-neoforge"
    changelog = ""
    //changelog = provider { rootProject.file("CHANGELOG.md").readText() }
    modLoaders.add("neoforge")

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = env["MODRINTH_TOKEN"]
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        requires("kotlin-for-forge", "big-shot-lib", "yacl")
    }

    /*
    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = env["CURSEFORGE_TOKEN"]
        minecraftVersions.add(property("deps.minecraft") as String)
        minecraftVersions.addAll(additionalVersions)
        requires("kotlin-for-forge", "big-shot-lib", "yacl")
    }
     */
}