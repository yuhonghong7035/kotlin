apply plugin: 'kotlin'
apply plugin: 'maven'
apply plugin: 'jps-compatible'

configureJvmProject(project)
configurePublishing(project)

repositories {
    mavenLocal()
}

pill {
    variant = "FULL"
}

dependencies {
    compile project(':kotlin-stdlib')
    compile project('::kotlin-native:kotlin-native-utils')

    compileOnly gradleApi()
    compileOnly 'com.android.tools.build:gradle:0.4.2'
}

tasks.withType(project.compileKotlin.class) {
    kotlinOptions.languageVersion = "1.2"
    kotlinOptions.apiVersion = "1.2"
    kotlinOptions.freeCompilerArgs += ["-Xskip-metadata-version-check"]
}

artifacts {
    archives sourcesJar
    archives javadocJar
}

jar {
    manifestAttributes(manifest, project)
}