plugins { kotlin("multiplatform") version "2.3.21" }
kotlin {
    jvm()
    jvmToolchain(21)
    sourceSets {
        commonMain {
            kotlin.srcDir("../mermaid-ffi/dist/kotlin-multiplatform/src/commonMain/kotlin")
            dependencies { implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0") }
        }
        jvmMain {
            kotlin.srcDir("../mermaid-ffi/dist/kotlin-multiplatform/src/jvmMain/kotlin")
            resources.srcDir("../mermaid-ffi/dist/kotlin-multiplatform/src/jvmMain/resources")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
