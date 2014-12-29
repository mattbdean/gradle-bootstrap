package net.dean.gbs.test

import org.testng.annotations.Test as test
import org.testng.Assert.assertEquals
import com.fasterxml.jackson.databind.ObjectMapper
import net.dean.gbs.web.GradleBootstrapConf
import net.dean.gbs.web.ProjectModel
import java.util.UUID
import net.dean.gbs.api.TestingFramework
import net.dean.gbs.api.LoggingFramework
import net.dean.gbs.api.License
import net.dean.gbs.api.Language
import io.dropwizard.testing.FixtureHelpers
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

public class FixtureTest {
    private val mapper = ObjectMapper();

    {
        GradleBootstrapConf.configureObjectMapper(mapper)
    }

    public test fun testPersonFixture() {
        val created = DateTime(2000, 1, 1, 0, 0, DateTimeZone.UTC).toDate()
        val expected = ProjectModel(UUID.fromString("f3b4d46c-e691-4c6b-b7c7-feed491d0dbd"),
                created,
                created,
                "app",
                "com.example.app",
                "1.0",
                TestingFramework.TESTNG,
                LoggingFramework.SLF4J,
                License.APACHE,
                listOf(Language.JAVA, Language.KOTLIN))
        val actual = mapper.readValue(FixtureHelpers.fixture("fixtures/project.json"), javaClass<ProjectModel>())

        assertEquals(mapper.writeValueAsString(actual), mapper.writeValueAsString(expected))
    }
}
