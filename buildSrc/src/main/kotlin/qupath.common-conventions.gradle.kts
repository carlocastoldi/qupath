import io.github.qupath.gradle.PlatformPlugin
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the

plugins {
    id("qupath.java-conventions")
    jacoco
    id("org.bytedeco.gradle-javacpp-platform")
}

// See https://discuss.gradle.org/t/how-to-apply-binary-plugin-from-convention-plugin/48778/2
apply(plugin = "io.github.qupath.platform")

val libs = the<LibrariesForLibs>()

repositories {

    val useLocal = providers.gradleProperty("use-maven-local")
    if (useLocal.orNull == "true") {
        logger.warn("Using Maven local")
        mavenLocal()
    }

    mavenCentral()

    // Required for scijava (including some QuPath jars)
    maven {
    	name = "SciJava"
	    url = uri("https://maven.scijava.org/content/repositories/releases")
	}

    // May be required during development
    maven {
        name = "SciJava snapshots"
        url = uri("https://maven.scijava.org/content/repositories/snapshots")
    }

    // Required for Bio-Formats
    maven {
        name = "Unidata"
        url = uri("https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases")
    }
    maven {
        name = "Open Microscopy"
        url = uri("https://artifacts.openmicroscopy.org/artifactory/maven/")
    }

    // May be required for snapshot JavaCPP jars
    maven {
        name = "Sonatype snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

}

/*
 * Some metadata for the manifest
 */
project.version = gradle.extra["qupathVersion"] as String

/*
 * Handle OS-specific decisions
 */
if (io.github.qupath.gradle.Utils.currentPlatform() == PlatformPlugin.Platform.UNKNOWN) {
    throw GradleException("Unknown operating system - can't build QuPath, sorry!")
}
if ("32" == System.getProperty("sun.arch.data.model")) {
    throw GradleException("Can't build QuPath using a 32-bit JDK - please use a 64-bit JDK instead")
}

/*
 * Optionally use OpenCV with CUDA.
 * See https://github.com/bytedeco/javacpp-presets/tree/master/cuda for info (and licenses).
 */
val useCudaRedist = project.hasProperty("cuda-redist")
val useCuda = useCudaRedist || project.hasProperty("cuda")

val opencv by configurations.creating
val guava by configurations.creating

dependencies {

    if (useCudaRedist) {
        opencv(libs.bundles.opencv.cuda)
    } else if (useCuda) {
        opencv(libs.bundles.opencv.gpu)
    } else
        opencv(libs.bundles.opencv)

    guava(libs.guava)

    implementation(libs.bundles.logging)

    testImplementation(libs.junit)
    testRuntimeOnly(libs.junit.platform)
}

tasks.test {
    useJUnitPlatform()
}
