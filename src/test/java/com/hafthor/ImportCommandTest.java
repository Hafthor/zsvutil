package com.hafthor;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

public class ImportCommandTest {
    @Test
    void importTest() throws IOException {
        final File f = File.createTempFile("test", ".json");
        f.deleteOnExit();
        try (final var fw = new FileWriter(f)) {
            fw.write("[{'source':'a','sku':1,'description':'desc1','timestamp':'2020-01-01T00:00:00Z'},{'source':'b','sku':2,'description':'desc2','timestamp':'2020-01-02T00:00:00Z'},{'source':'c','sku':3,'description':'desc3','timestamp':'2020-01-03T00:00:00Z'}]".replaceAll("'","\""));
        }
        final File f2 = File.createTempFile("test", ".zsv");
        f2.deleteOnExit();
        final ImportCommand cmd = (ImportCommand) Command.CommandFor(new String[]{"import", f.getAbsolutePath(), f2.getAbsolutePath()});
        assertEquals(0, cmd.execute());
        f.delete();

        final var names = new ArrayList<String>();
        final var cols = new ArrayList<String>();
        try (final var z = new ZipFile(f2)) {
            final var e = z.entries();
            while (e.hasMoreElements()) {
                final var entry = e.nextElement();
                names.add(entry.getName());
                try (final var in = z.getInputStream(entry)) {
                    cols.add(new String(in.readAllBytes()));
                }
            }
        }
        f2.delete();
        assertEquals("[source, sku, description, timestamp]", names.toString());
        assertEquals("[\"a\",\"b\",\"c\"]", cols.get(0));
        assertEquals("[1,2,3]", cols.get(1));
        assertEquals("[\"desc1\",\"desc2\",\"desc3\"]", cols.get(2));
        assertEquals("[\"2020-01-01T00:00:00Z\",\"2020-01-02T00:00:00Z\",\"2020-01-03T00:00:00Z\"]", cols.get(3));
    }
}
