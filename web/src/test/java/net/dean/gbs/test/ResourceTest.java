package net.dean.gbs.test;

import io.dropwizard.testing.junit.ResourceTestRule;
import net.dean.gbs.web.db.DataAccessObject;
import net.dean.gbs.web.models.ProjectModel;
import net.dean.gbs.web.resources.Resource;
import org.junit.Before;
import org.junit.Rule;

public abstract class ResourceTest<U extends DataAccessObject> {
    protected static final ProjectModel project = TestUtils.newProjectModel();

    @Rule
    public final ResourceTestRule resources;
    public final DataAccessObject dao;

    protected ResourceTest(U dao, Resource... resources) {
        ResourceTestRule.Builder builder = ResourceTestRule.builder();
        for (Resource res : resources) {
            builder.addResource(res);
        }

        this.resources = builder.build();
        this.dao = dao;
    }

    @Before
    public abstract void setup();
}
