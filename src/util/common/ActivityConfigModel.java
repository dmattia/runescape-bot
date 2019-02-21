package util.common;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActivityConfigModel {
    Map<String, String> keyValStore;
    List<TextOption> textOptions;

    private ActivityConfigModel(List<TextOption> textOptions) {
        keyValStore = new HashMap<>();
        this.textOptions = textOptions;

        textOptions.forEach(option -> keyValStore.put(option.key, option.value));
    }

    public static class TextOption {
        String key;
        String value;

        private TextOption(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private List<TextOption> options;

        Builder() {
            this.options = new ArrayList<>();
        }

        public Builder withTextField(String key, String defaultValue) {
            options.add(new TextOption(key, defaultValue));
            return this;
        }

        public ActivityConfigModel build() {
            return new ActivityConfigModel(options);
        }
    }
}
