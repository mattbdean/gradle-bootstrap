package net.dean.gbs.api.models

/**
 * Supported testing frameworks
 */
public enum class TestingFramework : Framework, HumanReadable {
    NONE {
        override val humanReadable = "None"
        override val deps = arrayOf<Dependency>()
    },
    // Not a typo, name is "TestNG"
    TESTNG {
        override val humanReadable = "TestNG"
        override val deps = arrayOf(Dependency("org.testing", "testng", scope = Scope.TEST_COMPILE))
    },
    JUNIT {
        override val humanReadable = "JUnit"
        override val deps = arrayOf(Dependency("junit", "junit", scope = Scope.TEST_COMPILE))
    }
}

public enum class LoggingFramework : Framework, HumanReadable {
    /** No logging (possibly java.util.logging) */
    NONE {
        override val humanReadable = "None"
        override val deps = arrayOf<Dependency>()
    },

    SLF4J {
        override val humanReadable = "SLF4J"
        private val group = "org.slf4j"
        override val deps = arrayOf(Dependency(group, "slf4j-api"), Dependency(group, "slf4j-simple"))
    },

    LOG4J {
        override val humanReadable = "Log4J"
        private val group = "org.apache.logging.log4j"
        override val deps = arrayOf(Dependency(group, "log4j-core"), Dependency(group, "log4j-api"))
    },

    LOGBACK_CLASSIC {
        override val humanReadable = "Logback Classic"
        override val deps = arrayOf(Dependency("ch.qos.logback", "logback-classic"))
    }
}

