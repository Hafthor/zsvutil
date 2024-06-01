package com.hafthor;

import org.junit.jupiter.api.Test;

import java.io.IOException;

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
        Command cmd = Command.CommandFor(new String[]{"export", "-g", "-h", "delta1-0.zsv", "delta1-0.utsv.gz"});
        assertInstanceOf(ExportCommand.class, cmd);
        assertEquals("delta1-0.zsv", cmd.inputFile);
        assertEquals("delta1-0.utsv.gz", cmd.outputFile);
        assertTrue(cmd.isGzip);
        assertTrue(cmd.hasHeader);
        assertEquals('\t', cmd.fieldDelimiter);
        assertNull(cmd.fields);
        assertNull(cmd.errorMessage);
    }

    @Test
    void commandForImport() {
        Command cmd = Command.CommandFor(new String[]{"import", "-d", ",", "-f", "source,sku,description,timestamp", "delta1-0.utsv.gz", "delta1-0.zsv"});
        assertInstanceOf(ImportCommand.class, cmd);
        assertEquals("delta1-0.utsv.gz", cmd.inputFile);
        assertEquals("delta1-0.zsv", cmd.outputFile);
        assertFalse(cmd.isGzip);
        assertFalse(cmd.hasHeader);
        assertEquals(',', cmd.fieldDelimiter);
        assertEquals("[source, sku, description, timestamp]", cmd.fields.toString());
        assertNull(cmd.errorMessage);
    }

    @Test
    void executeHelp() {
        assertEquals(0, new HelpCommand().execute());
    }

    @Test
    void executeError() throws IOException {
        assertEquals(1, new ErrorCommand("error").execute());
    }
}