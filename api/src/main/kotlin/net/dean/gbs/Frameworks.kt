package net.dean.gbs

/**
 * Supported testing frameworks
 */
public enum class TestingFramework : Framework {
    NONE {
        override val deps = array<Dependency>()
    }
    // Not a typo, name is "TestNG"
    TESTNG {
        // TODO: configureOnto() adds useTestNG() to test closure
        override val deps = array(Dependency("org.testing", "testng", scope = Scope.TEST_COMPILE))
    }
    JUNIT {
        override val deps = array(Dependency("junit", "junit", scope = Scope.TEST_COMPILE))
    }
}

public enum class LoggingFramework : Framework {
    /** No logging (could be java.util.logging) */
    NONE {
        override val deps = array<Dependency>()
    }

    SLF4J {
        override val deps = array(Dependency("org.slf4j", "slf4j-api"), Dependency("org.slf4j", "slf4j-simple"))
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

