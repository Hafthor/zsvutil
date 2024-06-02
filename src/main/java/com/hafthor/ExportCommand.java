package com.hafthor;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipFile;

public class ExportCommand extends Command {
    public ExportCommand(final String[] args) {
        super(args);
    }

    @Override
    public int execute() throws IOException {
        try (final var fo = new FileOutputStream(outputFile)) {
            if (!isGzip)
                return exportFile(fo);
            try (final var gz = new GZIPOutputStream(fo, 65536)) {
                return exportFile(gz);
            }
        }
    }

    int exportFile(final OutputStream out) throws IOException {
        // read the zip file making names and cols
        final var names = new ArrayList<String>();
        final var cols = new ArrayList<JsonNode>();
        final var mapper = new ObjectMapper();
        try (final var z = new ZipFile(inputFile)) {
            final var e = z.entries();
            while (e.hasMoreElements()) {
                final var entry = e.nextElement();
                if (fields == null || fields.stream().anyMatch(f -> f.equals(entry.getName()))) {
                    names.add(entry.getName());
                    try (final var in = z.getInputStream(entry)) {
                        System.err.print("Reading " + entry.getName() + "... ");
                        cols.add(mapper.readTree(in));
                        System.err.println("Done.");
                    }
                }
            }
        }

        // write output header
        System.err.print("Writing " + outputFile + "... ");

        // write output
        JsonGenerator g = mapper.createGenerator(out, JsonEncoding.UTF8);
        g.writeStartArray();
        for (int i = 0; i < cols.size(); i++) {
            g.writeFieldName(names.get(i));
            final var col = cols.get(i);
            g.writeTree(col);
        }
        g.writeEndArray();
        return 0;
    }
}