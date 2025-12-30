plugins {
	id("fabric-loom")
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
	mavenCentral()
}

dependencies {
	minecraft("com.mojang:minecraft:${property("minecraft_version")}")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
	modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth_version")}")
	modCompileOnly("com.terraformersmc:modmenu:${property("modmenu_version")}")

	property("elementa_version").let {
		implementation("gg.essential:elementa:$it")
		include("gg.essential:elementa:$it")
	}

	property("uc_version").let {
		modImplementation("gg.essential:universalcraft-1.21.9-fabric:$it")
		include("gg.essential:universalcraft-1.21.9-fabric:$it")
	}

	property("commodore_version").let {
		modImplementation("com.github.stivais:Commodore:$it")
		include("com.github.stivais:Commodore:$it")
	}

}

tasks.processResources {
	inputs.property("version", project.version)

	filesMatching("fabric.mod.json") {
		expand("version" to project.version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(21)
}

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
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