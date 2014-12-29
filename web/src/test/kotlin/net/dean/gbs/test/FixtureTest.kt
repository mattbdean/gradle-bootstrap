package net.dean.gbs.test

import org.junit.Test as test
import org.junit.Assert.assertEquals
import com.fasterxml.jackson.databind.ObjectMapper
import net.dean.gbs.web.GradleBootstrapConf
import io.dropwizard.testing.FixtureHelpers
import net.dean.gbs.web.models.ProjectModel

public class FixtureTest {
    private val mapper = ObjectMapper();

    {
        GradleBootstrapConf.configureObjectMapper(mapper)
    }

    public test fun testPersonFixture() {
        val expected = TestUtils.newProjectModel()
        val actual = mapper.readValue(FixtureHelpers.fixture("fixtures/project.json"), javaClass<ProjectModel>())

        assertEquals(mapper.writeValueAsString(expected), mapper.writeValueAsString(actual))
    }
}
