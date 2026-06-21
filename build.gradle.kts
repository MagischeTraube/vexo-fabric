plugins {
	id("net.fabricmc.fabric-loom")
	id("maven-publish")
	kotlin("jvm")
}


version = property("mod_version") as String
group = property("maven_group") as String

base {
	archivesName.set(property("archives_base_name") as String)
}

repositories {
	maven("https://repo.essential.gg/repository/maven-public")
	maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
	maven("https://maven.terraformersmc.com/")
	maven("https://jitpack.io")
	maven("https://api.modrinth.com/maven")
	mavenCentral()
}

dependencies {
	minecraft("com.mojang:minecraft:${property("minecraft_version")}")
	implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
	implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
	implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
	runtimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth_version")}")
	compileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")

	property("elementa_version").let {
		implementation("gg.essential:elementa:$it")
		include("gg.essential:elementa:$it")
	}

	property("uc_version").let {
		implementation("gg.essential:universalcraft-26.1-fabric:$it")
		include("gg.essential:universalcraft-26.1-fabric:$it")
	}

	property("commodore_version").let {
		implementation("com.github.stivais:Commodore:$it")
		include("com.github.stivais:Commodore:$it")
	}

}

tasks.processResources {
	inputs.property("version", project.version)

	filesMatching("fabric.mod.json") {
		expand("version" to project.version)
	}
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
	compilerOptions {
		freeCompilerArgs.add("-Xlambdas=class")
		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(25)
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	inputs.property("archivesName", base.archivesName)

	from("LICENSE") {
		rename { "${it}_${base.archivesName.get()}" }
	}
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifactId = property("archives_base_name") as String
			from(components["java"])
		}
	}
}