package com.hafthor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class CommandTest {
    @Test
    void commandForHelp() {
        Command cmd = Command.CommandFor(new String[]{});
        assertInstanceOf(HelpCommand.class, cmd);
    }

    @Test
    void commandForError() {
        Command cmd = Command.CommandFor(new String[]{"error"});
        assertInstanceOf(ErrorCommand.class, cmd);
    }

    @Test
    void commandForExport() {
        Command cmd = Command.CommandFor(new String[]{"export", "-g", "-f", "source,sku,description,timestamp", "delta1-0.zsv", "delta1-0.json.gz"});
        assertInstanceOf(ExportCommand.class, cmd);
        assertEquals("delta1-0.zsv", cmd.inputFile);
        assertEquals("delta1-0.json.gz", cmd.outputFile);
        assertTrue(cmd.isGzip);
        assertEquals("[source, sku, description, timestamp]", cmd.fields.toString());
        assertNull(cmd.errorMessage);
    }

    @Test
    void commandForImport() {
        Command cmd = Command.CommandFor(new String[]{"import", "-f", "source,sku,description,timestamp", "-g", "delta1-0.json.gz", "delta1-0.zsv"});
        assertInstanceOf(ImportCommand.class, cmd);
        assertEquals("delta1-0.json.gz", cmd.inputFile);
        assertEquals("delta1-0.zsv", cmd.outputFile);
        assertTrue(cmd.isGzip);
        assertEquals("[source, sku, description, timestamp]", cmd.fields.toString());
        assertNull(cmd.errorMessage);
    }

    @Test
    void executeHelp() {
        final var help = new HelpCommand();
        try (final var nullOut = new PrintStream(OutputStream.nullOutputStream())) {
            help.out = nullOut;
            help.err = null;
            assertEquals(0, help.execute());
        }
    }

    @Test
    void executeError() throws IOException {
        final var err = new ErrorCommand("error");
        try (final var nullOut = new PrintStream(OutputStream.nullOutputStream())) {
            err.out = null;
            err.err = nullOut;
            assertEquals(1, err.execute());
        }
    }
}