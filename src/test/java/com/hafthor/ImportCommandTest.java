package com.hafthor;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

public class ImportCommandTest {
    @Test
    void importTest() throws IOException {
        final File f = File.createTempFile("test", "utsv");
        f.deleteOnExit();
        try (final var fw = new FileWriter(f)) {
            fw.write("source\tsku\tdescription\ttimestamp\n");
            fw.write("a\t1\tdesc1\t2020-01-01T00:00:00Z\n");
            fw.write("b\t2\tdesc2\t2020-01-02T00:00:00Z\n");
            fw.write("c\t3\tdesc3\t2020-01-03T00:00:00Z\n");
        }
        final File f2 = File.createTempFile("test", "zsv");
        f2.deleteOnExit();
        final ImportCommand cmd = (ImportCommand) Command.CommandFor(new String[]{"import", "-h", f.getAbsolutePath(), f2.getAbsolutePath()});
        assertTrue(cmd.hasHeader);
        assertEquals(0, cmd.execute());
        f.delete();

        final var names = new ArrayList<String>();
        final var cols = new ArrayList<byte[]>();
        try (final var z = new ZipFile(f2)) {
            final var e = z.entries();
            while (e.hasMoreElements()) {
                final var entry = e.nextElement();
                names.add(entry.getName());
                try (final var in = z.getInputStream(entry)) {
                    cols.add(in.readAllBytes());
                }
            }
        }
        f2.delete();
        assertEquals("[source, sku, description, timestamp]", names.toString());
        assertEquals("a\nb\nc\n", new String(cols.get(0)));
        assertEquals("1\n2\n3\n", new String(cols.get(1)));
        assertEquals("desc1\ndesc2\ndesc3\n", new String(cols.get(2)));
        assertEquals("2020-01-01T00:00:00Z\n2020-01-02T00:00:00Z\n2020-01-03T00:00:00Z\n", new String(cols.get(3)));
    }
}
