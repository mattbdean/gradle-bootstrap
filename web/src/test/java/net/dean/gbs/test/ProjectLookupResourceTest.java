package net.dean.gbs.test;

import io.dropwizard.testing.junit.ResourceTestRule;
import net.dean.gbs.web.db.ProjectDao;
import net.dean.gbs.web.models.ProjectModel;
import net.dean.gbs.web.resources.ProjectLookupResource;
import org.junit.Rule;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ProjectLookupResourceTest extends ResourceTest<ProjectDao> {
    private static final ProjectDao dao = mock(ProjectDao.class);

    @Rule
    public final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new ProjectLookupResource(dao))
            .build();

    public ProjectLookupResourceTest() {
        super(dao, new ProjectLookupResource(dao));
    }

    @Override
    public void setup() {
        when(dao.get(eq(project.getId()))).thenReturn(project);
    }

    @Test
    public void testGetProjectModel() {
        ProjectModel actual = resources.client().resource("/project/" + project.getId()).get(ProjectModel.class);
        assertThat(actual).isEqualTo(project);
        verify(dao).get(project.getId());
    }
}

