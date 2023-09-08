package org.chuma.homecontroller.controller.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistent map with delayed file write. It is able to accumulate multiple writes into one operation to save disk.
 */
public class PersistentStateMap implements StateMap {
    static Logger log = LoggerFactory.getLogger(PersistentStateMap.class.getName());
    private final Map<String, Integer> map = new ConcurrentHashMap<>();
    private final File file;
    private final int saveDelayMs;
    private Thread saveThread;

    public PersistentStateMap(String fileName, int saveDelayMs) {
        file = new File(fileName);
        this.saveDelayMs = saveDelayMs;
        load();
    }

    void saveImpl() {
        try {
            if (!file.getParentFile().exists()) {
                Files.createDirectories(file.getParentFile().toPath());
            }
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
                String[] keys = map.keySet().toArray(new String[0]);
                Arrays.sort(keys);
                for (String key : keys) {
                    w.write(key);
                    w.write('=');
                    w.write(Integer.toString(map.get(key)));
                    w.newLine();
                }
                log.debug("Saved {} entries", map.size());
            }
        } catch (IOException e) {
            log.error("Failed to save state to " + file.getAbsolutePath(), e);
        }
    }

    void load() {
        if (!file.exists()) {
            log.debug("File {} doesn't exist, nothing to load", file.getAbsolutePath());
            return;
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) {
                String[] pair = line.trim().split("=");
                map.put(pair[0], Integer.valueOf(pair[1]));
            }
            log.debug("Loaded {} entries", map.size());
        } catch (IOException e) {
            map.clear();
            log.error("Failed to deserialize values from " + file.getAbsolutePath(), e);
        }
    }

    @Override
    public synchronized void setValue(String key, int value) {
        log.debug("set {}={}", key, value);
        if (!Integer.valueOf(value).equals(map.put(key, value))) {
            save();
        }
    }

    @Override
    public synchronized void removeValue(String key) {
        log.debug("remove {}", key);
        if (map.remove(key) != null) {
            save();
        }
    }

    private void save() {
        if (saveDelayMs == 0) {
            saveImpl();
        } else {
            if (saveThread == null) {
                saveThread = new Thread(() -> {
                    try {
                        Thread.sleep(saveDelayMs);
                    } catch (InterruptedException ignored) {
                    }
                    delayedSave();
                });
                saveThread.start();
            }
        }
    }

    private synchronized void delayedSave() {
        saveImpl();
        saveThread = null;
    }

    @Override
    public Integer getValue(String key) {
        return map.get(key);
    }
}
