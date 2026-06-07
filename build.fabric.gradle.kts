import io.github.klahap.dotenv.DotEnvBuilder
import org.gradle.model.internal.report.unbound.UnboundRuleInput.type

plugins {
    kotlin("jvm")

    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"

    id("me.modmuss50.mod-publish-plugin") version "2.0.0-beta.1"
    id("io.github.klahap.dotenv") version "1.1.3"

    id("com.google.devtools.ksp") version "2.3.9"

    id("dev.isxander.modstitch.base") version "0.8.5"

    id("net.typho.big_shot_lib.plugin") version "1.0.0"
}

bigShotLib {
    version(sc.current.version)
    loader("fabric")

    transformInfo {
        shortIdentifierMethods()
        defaultDeprecatedMethods()
        defaultInterfaceInjections()

        clientOnlyPackages.add("net/typho/big_shot_lib/api/client")
        clientOnlyPackages.add("net/typho/big_shot_lib/impl/client")
        clientOnlyPackages.add("net/typho/big_shot_lib/mixin/api/client")
        clientOnlyPackages.add("net/typho/big_shot_lib/mixin/impl/client")
    }
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

        replacementProperties.put("id", property("id") as String)
        replacementProperties.put("group", project.group as String)
        replacementProperties.put("minecraft_version_range", project.property("deps.minecraft_range") as String)
        replacementProperties.put("fabric_api_version", project.property("deps.fabric_api") as String)
        replacementProperties.put("big_shot_version", project.property("deps.big_shot") as String)
        replacementProperties.put("yacl_version", project.property("deps.yacl") as String)
        replacementProperties.put("sodium_version", project.property("deps.sodium") as String)
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

tasks.named<ProcessResources>("processResources") {
    val props = HashMap<String, String>().apply {
        this["java_version"] = when {
            sc.current.parsed >= "1.20.5" -> "21"
            sc.current.parsed >= "1.18" -> "17"
            sc.current.parsed >= "1.17" -> "16"
            else -> "8"
        }
    }

    inputs.properties(props)

    filesMatching(listOf("**/*.mixins.json")) {
        expand(props)
    }
}

version = "${property("version")}+${property("deps.minecraft")}-fabric"
base.archivesName = property("id") as String

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
    modstitchModImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    modstitchModImplementation("maven.modrinth:fabric-language-kotlin:1.13.12+kotlin.2.4.0")

    //modstitchModCompileOnly("maven.modrinth:sodium:${property("deps.sodium")}")
    modstitchModCompileOnly("net.typho:big_shot_lib:${property("deps.big_shot")}")
    modstitchModCompileOnly("maven.modrinth:yacl:${property("deps.yacl")}")
    modstitchModCompileOnly("maven.modrinth:modmenu:${property("deps.modmenu")}")

    findProperty("deps.sable_companion")?.let {
        modstitchJiJ(modstitchModApi("dev.ryanhcode.sable-companion:sable-companion-fabric-${property("deps.minecraft")}:[${it},)")!!)
    }
}

val additionalVersions: List<String> = (findProperty("publish.additionalVersions") as? String)
    ?.split(",")
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() }
    ?: emptyList()

publishMods {
    file = modstitch.finalJarTask.map { it.archiveFile.get() }
    additionalFiles.from(tasks.kotlinSourcesJar)

    type = STABLE
    displayName = "${property("name")} ${property("version")} for ${property("deps.minecraft") as String} Fabric"
    version = "${property("version")}+${property("deps.minecraft") as String}-fabric"
    changelog = ""
    //changelog = provider { rootProject.file("CHANGELOG.md").readText() }
    modLoaders.add("fabric")

    findProperty("publish.modrinth")?.let {
        modrinth {
            projectId = it as String
            accessToken = env["MODRINTH_TOKEN"]
            minecraftVersions.add(property("deps.minecraft") as String)
            minecraftVersions.addAll(additionalVersions)
            requires("fabric-api", "fabric-language-kotlin", "big-shot-lib", "yacl")
        }
    }

    findProperty("publish.curseforge")?.let {
        curseforge {
            projectId = it as String
            accessToken = env["CURSEFORGE_TOKEN"]
            minecraftVersions.add(property("deps.minecraft") as String)
            minecraftVersions.addAll(additionalVersions)
            requires("fabric-api", "fabric-language-kotlin", "big-shot-lib", "yacl")
        }
    }
}