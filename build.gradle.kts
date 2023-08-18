/*
 * Copyright 2023 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.time.Instant

///////////////////////////////
//////    PUBLISH TO SONATYPE / MAVEN CENTRAL
////// TESTING : (to local maven repo) <'publish and release' - 'publishToMavenLocal'>
////// RELEASE : (to sonatype/maven central), <'publish and release' - 'publishToSonatypeAndRelease'>
///////////////////////////////

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS   // always show the stacktrace!
gradle.startParameter.warningMode = WarningMode.All

plugins {
    java

    id("com.dorkbox.GradleUtils") version "3.17"
    id("com.dorkbox.Licensing") version "2.26"
    id("com.dorkbox.VersionUpdate") version "2.8"
    id("com.dorkbox.GradlePublish") version "1.18"

    kotlin("jvm") version "1.8.0"
}

object Extras {
    // set for the project
    const val description = "Unbuffered input and ANSI output support for Linux, MacOS, or Windows for Java 8+"
    const val group = "com.dorkbox"
    const val version = "3.9"

    // set as project.ext
    const val name = "Console"
    const val id = "Console"
    const val vendor = "Dorkbox LLC"
    const val vendorUrl = "https://dorkbox.com"
    const val url = "https://git.dorkbox.com/dorkbox/Console"
}

///////////////////////////////
/////  assign 'Extras'
///////////////////////////////
GradleUtils.load("$projectDir/../../gradle.properties", Extras)
GradleUtils.defaults()
GradleUtils.compileConfiguration(JavaVersion.VERSION_1_8)
GradleUtils.jpms(JavaVersion.VERSION_1_9)

licensing {
    license(License.APACHE_2) {
        description(Extras.description)
        url(Extras.url)
        author(Extras.vendor)

        extra("JAnsi", License.APACHE_2) {
            copyright(2009)
            author("Progress Software Corporation")
            author("Joris Kuipers")
            author("Jason Dillon")
            author("Hiram Chirino")
            url("https://github.com/fusesource/jansi")
        }

        extra("JLine2", License.BSD_2) {
            copyright(2012)
            author("Marc Prud\'hommeaux <mwp1@cornell.edu>")
            author("Daniel Doubrovkine")
            author("Torbjorn Granlund")
            author("David MacKenzie")
            url("https://github.com/jline/jline2")
        }
    }
}

tasks.jar.get().apply {
    manifest {
        // https://docs.oracle.com/javase/tutorial/deployment/jar/packageman.html
        attributes["Name"] = Extras.name

        attributes["Specification-Title"] = Extras.name
        attributes["Specification-Version"] = Extras.version
        attributes["Specification-Vendor"] = Extras.vendor

        attributes["Implementation-Title"] = "${Extras.group}.${Extras.id}"
        attributes["Implementation-Version"] = GradleUtils.now()
        attributes["Implementation-Vendor"] = Extras.vendor

        attributes["Automatic-Module-Name"] = Extras.id
    }
}


dependencies {
    api("com.dorkbox:ByteUtilities:1.14")
    api("com.dorkbox:PropertyLoader:1.3")
    api("com.dorkbox:Updates:1.1")
    api("com.dorkbox:JNA:1.1")
    api("com.dorkbox:Utilities:1.44")

    api("org.slf4j:slf4j-api:2.0.7")

    val jnaVersion = "5.12.1"
    api("net.java.dev.jna:jna:$jnaVersion")
    api("net.java.dev.jna:jna-platform:$jnaVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("ch.qos.logback:logback-classic:1.4.5")
}


publishToSonatype {
    groupId = Extras.group
    artifactId = Extras.id
    version = Extras.version

    name = Extras.name
    description = Extras.description
    url = Extras.url

    vendor = Extras.vendor
    vendorUrl = Extras.vendorUrl

    issueManagement {
        url = "${Extras.url}/issues"
        nickname = "Gitea Issues"
    }

    developer {
        id = "dorkbox"
        name = Extras.vendor
        email = "email@dorkbox.com"
    }
}
