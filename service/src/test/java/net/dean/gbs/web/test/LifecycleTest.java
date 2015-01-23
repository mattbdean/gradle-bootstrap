package net.dean.gbs.web.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.testing.junit.DropwizardAppRule;
import net.dean.gbs.api.models.Language;
import net.dean.gbs.api.models.License;
import net.dean.gbs.api.models.LoggingFramework;
import net.dean.gbs.api.models.TestingFramework;
import net.dean.gbs.web.GradleBootstrap;
import net.dean.gbs.web.GradleBootstrapConf;
import net.dean.gbs.web.models.ProjectModel;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LifecycleTest {
    @ClassRule
    public static final DropwizardAppRule<GradleBootstrapConf> RULE =
            new DropwizardAppRule<GradleBootstrapConf>(GradleBootstrap.class, TestUtils.getResource("dw-conf.yml"));
    private static final Path downloadDir = Paths.get("build/lifecycle_tests/");
    private static final JacksonJsonProvider jsonProvider;
    private static final ZipBodyReader zipProvider;

    static {
        jsonProvider = new JacksonJaxbJsonProvider();
        ObjectMapper mapper = Jackson.newObjectMapper();
        GradleBootstrapConf.configureObjectMapper(mapper);
        jsonProvider.setMapper(mapper);
        zipProvider = new ZipBodyReader(downloadDir);
    }

    @Test
    public void createAndDownload() {
        Client client = ClientBuilder.newClient()
                .register(jsonProvider)
                .register(zipProvider);

        GbsApi api = new GbsApiImpl(String.format("http://localhost:%d/api/v1", RULE.getLocalPort()), client);

        ProjectModel project = api.createProject("myapp", "com.test", "1.0", TestingFramework.TESTNG, LoggingFramework.SLF4J,
                License.MIT, setOf(Language.JAVA, Language.KOTLIN));

        int count = 0;
        int maxRetries = 5;

        while (!api.isDownloadReady(project.getId())) {
            if (count > maxRetries)
                fail("Project took too long to build");
            System.out.println("Download ready check #" + (++count));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail(e.getClass().getName());
            }
        }

        Path zipFile = api.download(project, downloadDir);
        assertTrue("Download did not exist", Files.isRegularFile(zipFile));
    }

    private <T> Set<T> setOf(T... values) {
        Set<T> set = new HashSet<T>(values.length);
        Collections.addAll(set, values);
        return set;
    }
}
