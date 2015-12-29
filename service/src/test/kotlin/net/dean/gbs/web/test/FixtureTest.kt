package net.dean.gbs.web.test

import io.dropwizard.jackson.Jackson
import io.dropwizard.testing.FixtureHelpers
import net.dean.gbs.web.GradleBootstrapConf
import net.dean.gbs.web.models.ProjectModel
import org.junit.Assert.assertEquals
import org.junit.Test as test

public class FixtureTest {
    private val mapper = Jackson.newObjectMapper();

    init {
        GradleBootstrapConf.configureObjectMapper(mapper)
    }

    public @test fun testProjectFixture() {
        val expected = TestUtils.newProjectModel()
        val actual = mapper.readValue(FixtureHelpers.fixture("fixtures/project.json"), ProjectModel::class.java)

        assertEquals(mapper.writeValueAsString(expected), mapper.writeValueAsString(actual))
    }
}
