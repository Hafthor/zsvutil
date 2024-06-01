package com.hafthor;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ExportCommandTest {
    @Test
    void exportTest() throws IOException {
        final File f = File.createTempFile("test", "zsv");
        f.deleteOnExit();
        try (final var fo = new FileOutputStream(f)) {
            try (final var zip = new ZipOutputStream(fo)) {
                zip.putNextEntry(new ZipEntry("source"));
                zip.write("a\nb\nc\n".getBytes());
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("sku"));
                zip.write("1\n2\n3\n".getBytes());
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("description"));
                zip.write("desc1\ndesc2\ndesc3\n".getBytes());
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("timestamp"));
                zip.write("2020-01-01T00:00:00Z\n2020-01-02T00:00:00Z\n2020-01-03T00:00:00Z\n".getBytes());
                zip.closeEntry();
            }
        }
        final File f2 = File.createTempFile("test", "utsv");
        f2.deleteOnExit();
        final ExportCommand cmd = (ExportCommand) Command.CommandFor(new String[]{"export", "-h", f.getAbsolutePath(), f2.getAbsolutePath()});
        assertEquals(0, cmd.execute());
        f.delete();
        assertEquals("source\tsku\tdescription\ttimestamp\na\t1\tdesc1\t2020-01-01T00:00:00Z\nb\t2\tdesc2\t2020-01-02T00:00:00Z\nc\t3\tdesc3\t2020-01-03T00:00:00Z\n", Files.readString(f2.toPath(), StandardCharsets.UTF_8));
        f2.delete();
    }
}
