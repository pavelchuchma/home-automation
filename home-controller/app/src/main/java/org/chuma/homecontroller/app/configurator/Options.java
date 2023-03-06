package org.chuma.homecontroller.app.configurator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;

import org.chuma.homecontroller.base.node.ListenerManager;

/**
 * Configuration properties with file persistence.
 * <p>
 * <ul>
 * <li>The properties are always checked for existence. It is not possible to set any property which is not already present.
 * <li>Set of all known properties along with their initial values is loaded from the resource.
 * <li>Properties can be accessed as non-string types. However, they are always stored as strings.
 * <li>Empty value is always converted to null - on get, null is returned, on put, empty string is stored instead. For non-string types,
 *     null throws exception.
 * <li>The file is loaded in constructor but to write it {@link #save()} must be called.
 * </ul>
 * <p>
 * The implementation uses custom load/save with format quite similar to properties file:
 * <ul>
 * <li>File must be in UTF-8
 * <li>Lines beginning with hash (#) are comments.
 * <li>Comment line directly preceding the property definition is remembered as property comment (description) and can be accessed
 *     by {@link #getComment(String)}. It is also saved to file along with the property.
 * <li>Property is in format 'name=value' where both name and value are trimmed. There are no specialties like double apostrophes as
 *     in standard properties file. It is also not possible to span value multiple lines with backslash.
 * </ul>
 */
public class Options {
    private final Properties properties = new Properties();
    private final Properties comments = new Properties();
    private final File file;
    private final ListenerManager<OptionChangeListener> listenerManager = new ListenerManager<>();

    public Options(String persistenceFile, String defaultResource) {
        file = new File(persistenceFile);
        try {
            // First load from properties resource
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(defaultResource)) {
                if (is == null) {
                    throw new IllegalArgumentException("Resource '" + defaultResource + "' not found");
                }
                loadFrom(is);
            }
            // If available, load additional properties from file
            if (file.exists()) {
                try (InputStream is = new FileInputStream(file)) {
                    loadFrom(is);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load options", e);
        }
    }

    public void addListener(OptionChangeListener listener) {
        listenerManager.add(listener);
    }

    public String get(String key) {
        String value = (String)properties.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Property '" + key + "' has no value");
        }
        return value.length() == 0 ? null : value;
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    /**
     * Returns comment for given key or null if none defined.
     */
    public String getComment(String key) {
        return (String)comments.get(key);
    }

    public void put(String key, String value) {
        if (value == null) {
            value = "";
        }
        if (!properties.containsKey(key)) {
            throw new IllegalArgumentException("Property '" + key + "' is unknown");
        }
        String old = properties.getProperty(key);
        if (!value.equals(old)) {
            // Changed - notify & set if notification didn't fail
            String v = value;
            listenerManager.callListeners(l -> l.optionChanged(key, v));
            properties.put(key, value);
        }
    }

    public void put(String key, int value) {
        put(key, Integer.toString(value));
    }

    public void put(String key, boolean value) {
        put(key, Boolean.toString(value));
    }

    /**
     * Get names of all properties.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getNames() {
        return (Set<String>)(Set<?>)properties.keySet();
    }

    /**
     * Save properties to file specified in constructor.
     */
    public void save() throws IOException {
        if (!file.getParentFile().exists()) {
            Files.createDirectories(file.getParentFile().toPath());
        }
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            String[] keys = getNames().toArray(new String[0]);
            Arrays.sort(keys);
            for (String key : keys) {
                String comment = (String)comments.get(key);
                if (comment != null) {
                    w.write("# ");
                    w.write(comment);
                    w.newLine();
                }
                w.write(key);
                w.write('=');
                w.write((String)properties.get(key));
                w.newLine();
            }
        }
    }

    private void loadFrom(InputStream inputStream) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String lastComment = null;
            for (String line; (line = r.readLine()) != null; ) {
                line = line.trim();
                if (line.length() == 0) {
                    lastComment = null;
                    continue;
                }
                if (line.startsWith("#")) {
                    lastComment = line;
                    continue;
                }
                int i = line.indexOf('=');
                if (i > 0) {
                    String key = line.substring(0, i).trim();
                    String value = line.substring(i + 1).trim();
                    if (key.length() != 0) {
                        properties.put(key, value);
                        if (lastComment != null) {
                            for (i = 1; i < lastComment.length() && (lastComment.charAt(i) == '#' || Character.isWhitespace(lastComment.charAt(i))); i++)
                                ;
                            lastComment = lastComment.substring(i);
                            if (lastComment.length() > 0) {
                                comments.put(key, lastComment);
                            }
                            lastComment = null;
                        }
                        continue;
                    }
                }
            }
        }
    }

    public interface OptionChangeListener {
        void optionChanged(String key, String value);
    }
}
