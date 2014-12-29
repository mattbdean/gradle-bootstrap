package net.dean.gbs.web;

import net.dean.gbs.api.Language;
import net.dean.gbs.api.License;
import net.dean.gbs.api.LoggingFramework;
import net.dean.gbs.api.Project;
import net.dean.gbs.api.TestingFramework;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class ProjectModel {
    private UUID id;
    private Date createdAt;
    private Date updatedAt;
    private String name;
    private String group;
    private String version;
    private String testingFramework;
    private String loggingFramework;
    private String license;
    private List<String> languages;

    public ProjectModel() {
        // JSON serialization
    }

    public ProjectModel(UUID id, Date createdAt, Date updatedAt, String name, String group, String version,
                        TestingFramework testingFramework, LoggingFramework loggingFramework, License license, List<Language> languages) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.name = name;
        this.group = group;
        this.version = version;
        setTestingFramework(testingFramework);
        setLoggingFramework(loggingFramework);
        setLicense(license);
        setLanguages(languages);
    }

    public static ProjectModel fromProject(Project project, UUID id, Date createdAt, Date updatedAt) {
        return new ProjectModel(id,
                createdAt,
                updatedAt,
                project.getName(),
                project.getGroup(),
                project.getVersion(),
                project.getBuild().getTesting(),
                project.getBuild().getLogging(),
                project.getLicense(),
                new ArrayList<Language>(project.getLanguages()));
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

    public List<String> getLanguages() {
        return languages;
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

    public void setLanguages(List<Language> languages) {
        List<String> langStrings = new ArrayList<String>();
        for (Language lang : languages) {
            langStrings.add(lang.name().toLowerCase());
        }
        this.languages = langStrings;
    }
}
