package net.dean.gbs.web.models;

import io.dropwizard.jackson.JsonSnakeCase;
import net.dean.gbs.api.models.Language;
import net.dean.gbs.api.models.License;
import net.dean.gbs.api.models.LoggingFramework;
import net.dean.gbs.api.models.Project;
import net.dean.gbs.api.models.TestingFramework;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

// This is done in Java for two reasons:
// 1. We want to automatically generate a toString(), hashCode(), and equals() method by applying the kotlin.data
//    annotation, but Kotlin will only generate those methods if all properties are initialized in the constructor.
// 2. When this happens, Kotlin will generate component1(), component2(), etc. methods that mess with Jackson
//    serialization
@JsonSnakeCase
public final class ProjectModel implements Model<Project> {
    protected UUID id;
    protected Date createdAt;
    protected Date updatedAt;
    protected String name;
    protected String group;
    protected String version;
    protected String testingFramework;
    protected String loggingFramework;
    protected String license;
    protected Set<Language> languages;
    protected String status;

    public ProjectModel() {
        // JSON serialization
    }

    public ProjectModel(UUID id, Date createdAt, Date updatedAt, String name, String group, String version,
                        TestingFramework testingFramework, LoggingFramework loggingFramework, License license,
                        Set<Language> languages, BuildStatus status) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.name = name;
        this.group = group;
        this.version = version;
        setTestingFramework(testingFramework);
        setLoggingFramework(loggingFramework);
        setLicense(license);
        setStatus(status);
        this.languages = languages;
    }

    public static ProjectModel fromProject(Project project, UUID id, Date createdAt, Date updatedAt, BuildStatus status) {
        return new ProjectModel(id,
                createdAt,
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

    public UUID getId() {
        return id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    public String getTestingFramework() {
        return testingFramework;
    }

    public String getLoggingFramework() {
        return loggingFramework;
    }

    public String getLicense() {
        return license;
    }

    public Set<Language> getLanguages() {
        return languages;
    }

    public String getStatus() {
        return status;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
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
        this.languages = languages;
    }

    public void setStatus(BuildStatus status) {
        this.status = status.name().toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProjectModel that = (ProjectModel) o;

        if (createdAt != null ? !createdAt.equals(that.createdAt) : that.createdAt != null) return false;
        if (group != null ? !group.equals(that.group) : that.group != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (languages != null ? !languages.equals(that.languages) : that.languages != null) return false;
        if (license != null ? !license.equals(that.license) : that.license != null) return false;
        if (loggingFramework != null ? !loggingFramework.equals(that.loggingFramework) : that.loggingFramework != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (testingFramework != null ? !testingFramework.equals(that.testingFramework) : that.testingFramework != null)
            return false;
        if (updatedAt != null ? !updatedAt.equals(that.updatedAt) : that.updatedAt != null) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (updatedAt != null ? updatedAt.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (group != null ? group.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (testingFramework != null ? testingFramework.hashCode() : 0);
        result = 31 * result + (loggingFramework != null ? loggingFramework.hashCode() : 0);
        result = 31 * result + (license != null ? license.hashCode() : 0);
        result = 31 * result + (languages != null ? languages.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ProjectModel {" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", name='" + name + '\'' +
                ", group='" + group + '\'' +
                ", version='" + version + '\'' +
                ", testingFramework='" + testingFramework + '\'' +
                ", loggingFramework='" + loggingFramework + '\'' +
                ", license='" + license + '\'' +
                ", languages=" + languages +
                '}';
    }
}


