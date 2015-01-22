package net.dean.gbs.web.models;

import com.fasterxml.jackson.annotation.JsonValue;
import net.dean.gbs.api.models.HumanReadable;
import net.dean.gbs.web.resources.ProjectOption;

import java.util.HashMap;
import java.util.Map;

public class ProjectOptionModel {
    private static final ProjectOption[] OPTIONS = ProjectOption.values();
    public static final ProjectOptionModel INSTANCE = new ProjectOptionModel();

    private Map<String, Object> data;

    private ProjectOptionModel() {
        this.data = new HashMap<String, Object>(OPTIONS.length);
        Map<String, Object> enums = new HashMap<String, Object>();
        Map<String, String> defaults = new HashMap<String, String>();

        for (ProjectOption option : OPTIONS) {
            HumanReadable[] humanReadables = option.getValues();
            Map<String, String> values = new HashMap<String, String>(humanReadables.length);
            for (HumanReadable readable : humanReadables) {
                values.put(readable.toString().toLowerCase(), readable.getHumanReadable());
            }
            enums.put(option.name().toLowerCase(), values);
            defaults.put(option.toString().toLowerCase(), option.getDefault().toString().toLowerCase());
        }

        defaults.put("name", ProjectModel.DEFAULT_NAME);
        defaults.put("group", ProjectModel.DEFAULT_GROUP);
        defaults.put("version", ProjectModel.DEFAULT_VERSION);

        data.put("enums", enums);
        data.put("defaults", defaults);
    }

    @JsonValue
    public Object toJson() {
        return data;
    }
}
