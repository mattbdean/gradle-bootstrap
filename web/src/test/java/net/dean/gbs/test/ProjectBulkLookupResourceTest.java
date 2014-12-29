package net.dean.gbs.test;

import net.dean.gbs.api.Language;
import net.dean.gbs.api.License;
import net.dean.gbs.api.LoggingFramework;
import net.dean.gbs.api.TestingFramework;
import net.dean.gbs.web.db.ProjectDao;
import net.dean.gbs.web.models.ProjectModel;
import net.dean.gbs.web.resources.ProjectBulkLookupResource;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ProjectBulkLookupResourceTest extends ResourceTest<ProjectDao> {
    private List<ProjectModel> expectedModels;
    private static final ProjectDao dao = mock(ProjectDao.class);

    public ProjectBulkLookupResourceTest() {
        super(dao, new ProjectBulkLookupResource(dao));
    }

    @Override
    public void setup() {
        this.expectedModels = generate(5);
        when(dao.getAll()).thenReturn(expectedModels.iterator());
    }

    @Test
    public void testBulkRetrieval() {
        ProjectModel[] actual = resources.client().resource("/projects").get(ProjectModel[].class);
        assertThat(actual).isEqualTo(toArray(expectedModels.iterator()));
        verify(dao).getAll();
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
                    languages
            ));
        }

        return models;
    }
}
