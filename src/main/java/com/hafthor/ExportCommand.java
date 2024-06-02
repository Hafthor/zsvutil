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
import java.util.HashMap;
import java.util.HashSet;
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
        final var fieldsMap = new HashMap<String, JsonNode>();
        final var fieldsList = new ArrayList<String>();
        final var fields = this.fields == null ? null : new HashSet<String>();
        if (this.fields != null) fields.addAll(this.fields);
        final var mapper = new ObjectMapper();
        try (final var z = new ZipFile(inputFile)) {
            final var e = z.entries();
            while (e.hasMoreElements()) {
                final var entry = e.nextElement();
                if (fields == null || fields.contains(entry.getName())) {
                    fieldsList.add(entry.getName());
                    try (final var in = z.getInputStream(entry)) {
                        System.err.print("Reading " + entry.getName() + "... ");
                        final var node = mapper.readTree(in);
                        if (!node.isArray()) {
                            errorMessage = "Error: root of JSON tree must be an array.";
                            return 1;
                        }
                        fieldsMap.put(entry.getName(), node);
                        System.err.println("Done.");
                    }
                }
            }
        }
        final int rows = fieldsMap.get(fieldsList.get(0)).size();

        // write output header
        System.err.print("Writing " + outputFile + "... ");

        // write output
        try (final var g = mapper.createGenerator(out, JsonEncoding.UTF8)) {
            g.writeStartArray();
            for (int row = 0; row < rows; row++) {
                g.writeStartObject();
                for (final var fieldName : fieldsList) {
                    g.writeFieldName(fieldName);
                    g.writeTree(fieldsMap.get(fieldName).get(row));
                }
                g.writeEndObject();
            }
            g.writeEndArray();
        }
        return 0;
    }
}