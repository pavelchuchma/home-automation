package org.chuma.homecontroller.app.servlet.rest;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.chuma.homecontroller.base.utils.Options;

public class OptionsHandlerTest {
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private Options createOptions() {
        Options options = new Options(new File(tempDir.getRoot(), "app.properties").getPath(), "options-handler-test.properties");
        options.addListener(new Options.OptionChangeListener() {
            @Override
            public void optionChanged(String key, String value) {
                if ("test.b".equals(key) && "bad".equals(value)) {
                    throw new IllegalArgumentException("Invalid value");
                }
            }

            @Override
            public void optionsSaved(Set<String> keys) {
            }
        });
        return options;
    }

    @Test
    public void updatesAllValues() {
        Options options = createOptions();
        OptionsHandler.update(options, Map.of("test.a", "2", "test.b", "y"));
        Assert.assertEquals("2", options.get("test.a"));
        Assert.assertEquals("y", options.get("test.b"));
    }

    @Test
    public void restoresChangedValuesWhenOneIsRejected() {
        Options options = createOptions();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("test.a", "2");
        values.put("test.b", "bad");
        Assert.assertThrows(IllegalArgumentException.class, () -> OptionsHandler.update(options, values));
        Assert.assertEquals("1", options.get("test.a"));
        Assert.assertEquals("x", options.get("test.b"));
    }

    @Test
    public void rejectsUnknownKey() {
        Options options = createOptions();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("test.a", "2");
        values.put("test.unknown", "z");
        Assert.assertThrows(IllegalArgumentException.class, () -> OptionsHandler.update(options, values));
        Assert.assertEquals("1", options.get("test.a"));
    }

    @Test
    public void readsEmptyValueAsEmptyString() {
        Map<String, String> values = OptionsHandler.read(createOptions(), new String[]{"test.a", "test.empty"});
        Assert.assertEquals("1", values.get("test.a"));
        Assert.assertEquals("", values.get("test.empty"));
    }

    @Test
    public void readRejectsUnknownKey() {
        Assert.assertThrows(IllegalArgumentException.class, () -> OptionsHandler.read(createOptions(), new String[]{"test.unknown"}));
    }
}
