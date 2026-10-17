package org.chuma.homecontroller.extensions.metrics;

import org.junit.Assert;
import org.junit.Test;

public class LineProtocolWriterTest {
    private enum Mode {Normal, Fault}

    @Test
    public void writesTypedFieldsWithPrefixAndMsTimestamp() {
        LineProtocolWriter w = new LineProtocolWriter("home");
        w.line("inverter", 1_700_000_000_123L)
                .field("pv_w", 1234)
                .field("soc", 55L)
                .field("batt_v", 51.25)
                .field("on", true)
                .field("mode", Mode.Normal)
                .field("note", "a \"quoted\" \\ value")
                .end();
        Assert.assertEquals("home-inverter pv_w=1234i,soc=55i,batt_v=51.25,on=true,mode=\"Normal\",note=\"a \\\"quoted\\\" \\\\ value\" 1700000000123\n",
                w.toString());
    }

    @Test
    public void tagsComeBeforeFieldsAndAreEscaped() {
        LineProtocolWriter w = new LineProtocolWriter("home");
        w.line("my measurement", 1)
                .tag("name", "a b,c=d")
                .tag("empty", "")
                .tag("none", null)
                .field("v", 1)
                .end();
        Assert.assertEquals("home-my\\ measurement,name=a\\ b\\,c\\=d v=1i 1\n", w.toString());
    }

    @Test
    public void skipsMissingValuesAndDropsEmptyLines() {
        LineProtocolWriter w = new LineProtocolWriter("home");
        w.line("x", 1).field("nan", Double.NaN).field("inf", Double.POSITIVE_INFINITY).field("s", (String)null).field("e", (Enum<?>)null).end();
        Assert.assertTrue("line without fields must be dropped", w.isEmpty());
        w.line("x", 2).field("nan", Double.NaN).field("ok", 1.0).end();
        Assert.assertEquals("home-x ok=1.0 2\n", w.toString());
    }

    @Test
    public void formatsDoublesWithoutExponentAndTrailingZeros() {
        LineProtocolWriter w = new LineProtocolWriter("p");
        w.line("x", 3).field("a", 0.1 + 0.2).field("b", 12345.5).field("c", -3.0).field("d", 1e-5).end();
        Assert.assertEquals("p-x a=0.3,b=12345.5,c=-3.0,d=0.0 3\n", w.toString());
    }

    @Test
    public void appendsRawLinesAndClears() {
        LineProtocolWriter w = new LineProtocolWriter("p");
        w.line("x", 1).field("a", 1).end();
        w.append("raw line 2\n");
        Assert.assertEquals("p-x a=1i 1\nraw line 2\n", w.toString());
        w.clear();
        Assert.assertTrue(w.isEmpty());
    }
}
