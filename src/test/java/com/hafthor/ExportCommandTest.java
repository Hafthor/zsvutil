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
        final File f = File.createTempFile("test", ".zsv");
        f.deleteOnExit();
        try (final var fo = new FileOutputStream(f)) {
            try (final var zip = new ZipOutputStream(fo)) {
                zip.putNextEntry(new ZipEntry("source"));
                zip.write("[\"a\",\"b\",\"c\"]".getBytes());
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("sku"));
                zip.write("[1,2,3]".getBytes());
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("description"));
                zip.write("[\"desc1\",\"desc2\",\"desc3\"]".getBytes());
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("timestamp"));
                zip.write("[\"2020-01-01T00:00:00Z\",\"2020-01-02T00:00:00Z\",\"2020-01-03T00:00:00Z\"]".getBytes());
                zip.closeEntry();
            }
        }
        final File f2 = File.createTempFile("test", ".json");
        f2.deleteOnExit();
        final ExportCommand cmd = (ExportCommand) Command.CommandFor(new String[]{"export", f.getAbsolutePath(), f2.getAbsolutePath()});
        assertEquals(0, cmd.execute());
        f.delete();
        assertEquals("[{'source':'a','sku':1,'description':'desc1','timestamp':'2020-01-01T00:00:00Z'},{'source':'b','sku':2,'description':'desc2','timestamp':'2020-01-02T00:00:00Z'},{'source':'c','sku':3,'description':'desc3','timestamp':'2020-01-03T00:00:00Z'}]".replaceAll("'","\""), Files.readString(f2.toPath(), StandardCharsets.UTF_8));
        f2.delete();
    }
}
