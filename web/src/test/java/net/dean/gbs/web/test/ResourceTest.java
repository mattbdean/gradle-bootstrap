package net.dean.gbs.web.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.junit.ResourceTestRule;
import net.dean.gbs.api.models.Language;
import net.dean.gbs.api.models.License;
import net.dean.gbs.api.models.LoggingFramework;
import net.dean.gbs.api.models.TestingFramework;
import net.dean.gbs.web.GradleBootstrapConf;
import net.dean.gbs.web.ProjectBuilder;
import net.dean.gbs.web.db.ProjectDao;
import net.dean.gbs.web.models.BuildStatus;
import net.dean.gbs.web.models.ProjectModel;
import net.dean.gbs.web.resources.ProjectResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ResourceTest {
    protected static final ProjectModel project = TestUtils.newProjectModel();

    public static final ProjectDao dao = mock(ProjectDao.class);
    public static final ProjectBuilder builder = new ProjectBuilder(dao, Paths.get("build/test-downloads"));

    @Rule
    public final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new ProjectResource(dao, builder))
            .build();

    @Before
    public void setup() {
        reset(dao);
    }

    @Test
    public void testLookup() {
        when(dao.get(eq(project.getId()))).thenReturn(project);

        ProjectModel actual = resources.client().resource("/project/" + project.getId()).get(ProjectModel.class);

        assertThat(actual).isEqualTo(project);
        verify(dao).get(project.getId());
    }

    @Test
    public void testGetAll() {
        List<ProjectModel> expectedModels = generate(5);
        when(dao.getAll()).thenReturn(expectedModels.iterator());

        ProjectModel[] actual = resources.client().resource("/project/list").get(ProjectModel[].class);
        assertThat(actual).isEqualTo(toArray(expectedModels.iterator()));
        verify(dao).getAll();
    }

    @Test
    public void testCreate() throws IOException {
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

    private List<ProjectModel> generate(int count) {
        List<ProjectModel> models = new ArrayList<ProjectModel>();
        Set<Language> languages = new HashSet<Language>();
        languages.add(Language.JAVA);

        for (int i = 0; i < count; i++) {
            models.add(new ProjectModel(
                    UUID.randomUUID(),
                    new Date(),
                    new Date(),
                    String.format("app%d", i),
                    String.format("com.example.app%d", i),
                    "1.0",
                    TestingFramework.TESTNG,
                    LoggingFramework.SLF4J,
                    License.MIT,
                    languages,
                    BuildStatus.READY
            ));
        }

        return models;
    }

    private ProjectModel[] toArray(Iterator<ProjectModel> it) {
        List<ProjectModel> list = new ArrayList<ProjectModel>();
        // Read out the iterator into a Set
        while (it.hasNext())
            list.add(it.next());

        // Transfer the contents of the set into an array
        ProjectModel[] arr = new ProjectModel[list.size()];
        for (int i = 0; i < arr.length; i++)
            arr[i] = list.get(i);

        return arr;
    }
}
