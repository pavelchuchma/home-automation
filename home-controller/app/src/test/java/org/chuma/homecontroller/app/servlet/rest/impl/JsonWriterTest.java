package org.chuma.homecontroller.app.servlet.rest.impl;

import junit.framework.TestCase;
import org.junit.Test;

public class JsonWriterTest extends TestCase {
    @Test
    public void testWrite() {
        JsonWriter writer = new JsonWriter(true);

        try (JsonWriter root = writer.startObject()) {
            try (JsonWriter status = root.startArrayAttribute("status")) {
                try (JsonWriter pumps = status.startObject()) {
                    try (JsonWriter pumpArr = pumps.startArrayAttribute("wpmp")) {
                        try (JsonWriter pump1 = pumpArr.startObject()) {
                            pump1.addAttribute("id", "wpmp")
                                    .addAttribute("on", false)
                                    .addAttribute("recCount", 69);
                            try (JsonWriter recordArr = pump1.startArrayAttribute("lastRecords")) {
                                try (JsonWriter r1 = recordArr.startObject()) {
                                    r1.addAttribute("time", "Wed Jan 19 21:54:36 CET 2022")
                                            .addAttribute("diration", 1253);
                                }
                                try (JsonWriter r2 = recordArr.startObject()) {
                                    r2.addAttribute("time", "Wed Jan 20 21:54:36 CET 2022")
                                            .addAttribute("diration", 58225);
                                }
                            }
                        }
                    }
                    try (JsonWriter i4 = writer.startArrayAttribute("wpmp")) {
                        try (JsonWriter i5 = writer.startObject()) {
                            writer.addAttribute("id", "wpmp");
                            writer.addAttribute("on", false);
                            writer.addAttribute("recCount", 69);
                            writer.addAttribute("recCount", 69);
                            try (JsonWriter i6 = writer.startArrayAttribute("lastRecords")) {
                                try (JsonWriter i7 = writer.startObject()) {
                                    writer.addAttribute("time", "Wed Jan 19 21:54:36 CET 2022");
                                    writer.addAttribute("diration", 1253);
                                }
                                try (JsonWriter i7 = writer.startObject()) {
                                    writer.addAttribute("time", "Wed Jan 20 21:54:36 CET 2022");
                                    writer.addAttribute("diration", 58225);
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println(writer);
    }

    @Test
    public void testWrite4() {
        JsonWriter writer = new JsonWriter(true);

        try (JsonWriter ignored = writer.startObject()) {
            writer.addAttribute("aa", "val");
            writer.addAttribute("bb", 99);
            try (JsonWriter ignored2 = writer.startArrayAttribute("arrayEntry")) {
                try (JsonWriter ignored3 = writer.startObject()) {
                    writer.addAttribute("cc", 3333);
                }
                try (JsonWriter ignored3 = writer.startObject()) {
                    writer.addAttribute("dd", 4444);
                }
            }
        }
        System.out.println(writer);
    }

    public void testWrite3() {
        JsonWriter writer = new JsonWriter(true);

        try (JsonWriter ignored = writer.startObject()) {
            writer.addAttribute("aa", "val");
            writer.addAttribute("bb", 99);
            try (JsonWriter ignored2 = writer.startArrayAttribute("arrayEntry")) {
                try (JsonWriter ignored3 = writer.startObject()) {
                    writer.addAttribute("cc", 3333);
                }
                try (JsonWriter ignored3 = writer.startObject()) {
                    writer.addAttribute("dd", 4444);
                }
            }
        }
        System.out.println(writer);
    }

    @Test
    public void testWrite2() {
        JsonWriter writer = new JsonWriter(true);

        writer.startObject();
        {
            writer.addAttribute("aa", "val");
            writer.addAttribute("bb", 99);
            writer.startArrayAttribute("arrayEntry");
            writer.startObject();
            writer.addAttribute("cc", 3333);
            writer.close();
            writer.startObject();
            writer.addAttribute("dd", 444);
            writer.close();
            writer.close();
        }
        writer.close();
        System.out.println(writer.toString());
    }

}