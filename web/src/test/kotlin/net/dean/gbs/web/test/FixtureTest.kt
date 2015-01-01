package net.dean.gbs.web.test

import org.junit.Test as test
import org.junit.Assert.assertEquals
import io.dropwizard.testing.FixtureHelpers
import net.dean.gbs.web.models.ProjectModel
import io.dropwizard.jackson.Jackson
import net.dean.gbs.web.GradleBootstrapConf

public class FixtureTest {
    private val mapper = Jackson.newObjectMapper();

    {
        GradleBootstrapConf.configureObjectMapper(mapper)
    }

    public test fun testPersonFixture() {
        val expected = TestUtils.newProjectModel()
        val actual = mapper.readValue(FixtureHelpers.fixture("fixtures/project.json"), javaClass<ProjectModel>())

        assertEquals(mapper.writeValueAsString(expected), mapper.writeValueAsString(actual))
    }
}
