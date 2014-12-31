package net.dean.gbs.api.models

/**
 * Supported testing frameworks
 */
public enum class TestingFramework : Framework {
    NONE {
        override val deps = array<Dependency>()
    }
    // Not a typo, name is "TestNG"
    TESTNG {
        override val deps = array(Dependency("org.testing", "testng", scope = Scope.TEST_COMPILE))
    }
    JUNIT {
        override val deps = array(Dependency("junit", "junit", scope = Scope.TEST_COMPILE))
    }
}

public enum class LoggingFramework : Framework {
    /** No logging (possibly java.util.logging) */
    NONE {
        override val deps = array<Dependency>()
    }

    SLF4J {
        private val group = "org.slf4j"
        override val deps = array(Dependency(group, "slf4j-api"), Dependency(group, "slf4j-simple"))
    }

    LOG4J {
        private val group = "org.apache.logging.log4j"
        override val deps = array(Dependency(group, "log4j-core"), Dependency(group, "log4j-api"))
    }

    APACHE_COMMONS {
        override val deps = array(Dependency("commons-logging", "commons-logging"))
    }

    LOGBACK_CLASSIC {
        override val deps = array(Dependency("ch.qos.logback", "logback-classic"))
    }
}

