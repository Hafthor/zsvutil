package com.hafthor;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

    int exportFile(final OutputStream fo) throws IOException {
        // read the zip file making names and cols
        final var fieldsMap = new HashMap<String, JsonNode>();
        final var fieldsList = new ArrayList<String>();
        final var fields = this.fields == null ? null : new HashSet<>(this.fields);
        final var mapper = new ObjectMapper();
        try (final var zip = new ZipFile(inputFile)) {
            final var e = zip.entries();
            while (e.hasMoreElements()) {
                final var entry = e.nextElement();
                final var entryName = entry.getName();
                if (fields == null || fields.contains(entryName)) {
                    fieldsList.add(entryName);
                    try (final var fi = zip.getInputStream(entry)) {
                        out.print("Reading " + entryName + "... ");
                        final var node = mapper.readTree(fi);
                        if (!node.isArray()) {
                            errorMessage = "Error: root of JSON tree must be an array.";
                            return 1;
                        }
                        fieldsMap.put(entryName, node);
                        out.println("Done.");
                    }
                }
            }
        }
        // check that all fields are present
        if (fields != null && !fieldsMap.keySet().containsAll(fields)) {
            errorMessage = "Error: not all fields specified were present.";
            return 1;
        }

        final int rows = fieldsMap.get(fieldsList.get(0)).size();

        // write output
        out.print("Writing " + outputFile + "... ");
        try (final var gen = mapper.createGenerator(fo, JsonEncoding.UTF8)) {
            gen.writeStartArray();
            for (int row = 0; row < rows; row++) {
                gen.writeStartObject();
                for (final var fieldName : fieldsList) {
                    gen.writeFieldName(fieldName);
                    gen.writeTree(fieldsMap.get(fieldName).get(row));
                }
                gen.writeEndObject();
            }
            gen.writeEndArray();
        }
        out.println("Done.");
        return 0;
    }
}