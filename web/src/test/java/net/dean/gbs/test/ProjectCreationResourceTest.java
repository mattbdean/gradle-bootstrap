package net.dean.gbs.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import io.dropwizard.jackson.Jackson;
import net.dean.gbs.web.GradleBootstrapConf;
import net.dean.gbs.web.db.ProjectDao;
import net.dean.gbs.web.models.ProjectModel;
import net.dean.gbs.web.resources.ProjectCreationResource;
import net.dean.gbs.web.resources.ProjectLookupResource;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ProjectCreationResourceTest extends ResourceTest<ProjectDao> {
    private static final ProjectDao dao = mock(ProjectDao.class);

    public ProjectCreationResourceTest() {
        super(dao, new ProjectCreationResource(dao), new ProjectLookupResource(dao));
    }

    @Override
    public void setup() {
    }

    @Test
    public void testInsertProject() throws IOException {
        ObjectMapper mapper = Jackson.newObjectMapper();
        GradleBootstrapConf.OBJECT$.configureObjectMapper(mapper);

        // Insert the model
        ClientResponse response = resources.client().resource("/project").type(MediaType.APPLICATION_FORM_URLENCODED).post(ClientResponse.class, TestUtils.toMultivaluedMap(project));
        // Grab the model from the response
        ProjectModel expected = mapper.readValue(response.getEntityInputStream(), ProjectModel.class);
        // Now mock dao.get() so that it returns the expected object when queried
        when(dao.get(expected.getId())).thenReturn(expected);

        assertThat(dao.get(expected.getId())).isEqualTo(expected);
        verify(dao).get(expected.getId());
    }
}
