package net.dean.gbs.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import net.dean.gbs.api.models.Language;
import net.dean.gbs.api.models.License;
import net.dean.gbs.api.models.LoggingFramework;
import net.dean.gbs.api.models.Project;
import net.dean.gbs.api.models.TestingFramework;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

// This is done in Java for two reasons:
// 1. We want to automatically generate a toString(), hashCode(), and equals() method by applying the kotlin.data
//    annotation, but Kotlin will only generate those methods if all properties are initialized in the constructor.
// 2. When this happens, Kotlin will generate component1(), component2(), etc. methods that mess with Jackson
//    serialization
@Entity(name = "project")
@Table(name = "projects")
public final class ProjectModel extends Model {
    @Column(name = "name")
    private String name;

    @Column(name = "group_name") // "group" is a keyword
    private String group;

    @Column(name = "version")
    private String version;

    @Column(name = "testing_fw")
    private String testingFramework;

    @Column(name = "logging_fw")
    private String loggingFramework;

    @Column(name = "license")
    private String license;

    @Column(name = "languages")
    private String languages; // Stored as comma separated values

    @Column(name = "status")
    private String status;

    public ProjectModel() {
        super();
        // JSON serialization
    }

    public ProjectModel(DateTime createdAt, DateTime updatedAt, String name, String group, String version,
                        TestingFramework testingFramework, LoggingFramework loggingFramework, License license,
                        Set<Language> languages, BuildStatus status) {
        super(createdAt, updatedAt);
        this.name = name;
        this.group = group;
        this.version = version;
        setTestingFramework(testingFramework);
        setLoggingFramework(loggingFramework);
        setLicense(license);
        setStatus(status);
        setLanguages(languages);
    }

    public static ProjectModel fromProject(Project project, DateTime createdAt, DateTime updatedAt, BuildStatus status) {
        return new ProjectModel(createdAt,
                updatedAt,
                project.getName(),
                project.getGroup(),
                project.getVersion(),
                project.getBuild().getTesting(),
                project.getBuild().getLogging(),
                project.getLicense(),
                project.getLanguages(),
                status);
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("group")
    public String getGroup() {
        return group;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("testing_framework")
    public String getTestingFramework() {
        return testingFramework;
    }

    @JsonProperty("logging_framework")
    public String getLoggingFramework() {
        return loggingFramework;
    }

    @JsonProperty("license")
    public String getLicense() {
        return license;
    }

    @JsonProperty("languages")
    public Set<String> getLanguages() {
        String[] strArray = languages.split(",");
        Set<String> set = new HashSet<String>(strArray.length);
        for (String str : strArray) {
            set.add(str.toLowerCase());
        }
        return set;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setTestingFramework(TestingFramework testingFramework) {
        this.testingFramework = testingFramework.name().toLowerCase();
    }

    public void setLoggingFramework(LoggingFramework loggingFramework) {
        this.loggingFramework = loggingFramework.name().toLowerCase();
    }

    public void setLicense(License license) {
        this.license = license.name().toLowerCase();
    }

    public void setLanguages(Set<Language> languages) {
        Set<String> lowercaseLangs = new HashSet<String>(languages.size());
        for (Language lang : languages) {
            lowercaseLangs.add(lang.name().toLowerCase());
        }
        this.languages = Joiner.on(',').join(lowercaseLangs);
    }

    public void setStatus(BuildStatus status) {
        this.status = status.name().toLowerCase();
    }
}


