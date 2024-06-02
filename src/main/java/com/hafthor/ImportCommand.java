package com.hafthor;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ImportCommand extends Command {
    public ImportCommand(final String[] args) {
        super(args);
    }

    @Override
    public int execute() throws IOException {
        try (final var fi = new FileInputStream(inputFile)) {
            if (!isGzip)
                return importFile(fi);
            try (final var gz = new GZIPInputStream(fi, 65536)) {
                return importFile(gz);
            }
        }
    }

    private int importFile(final InputStream in) throws IOException {
        // read source
        System.err.print("Reading " + inputFile + "... ");
        final var mapper = new ObjectMapper();
        final var tree = mapper.readTree(in);
        // root of tree must be an array
        if (!tree.isArray()) {
            errorMessage = "Error: root of JSON tree must be an array.";
            return 1;
        }
        // count elements of the array
        final int rowCount = tree.size();
        // go through json tree once to get the field names and the row count
        final var fields = this.fields == null ? null : new HashSet<>(this.fields);
        final var fieldsSet = new HashSet<String>();
        final var fieldsList = new ArrayList<String>();
        // iterate over the array
        for (final var row : tree) {
            // each row must be an object
            if (!row.isObject()) {
                errorMessage = "Error: each row value must be an object.";
                return 1;
            }
            // iterate over the fields of the row
            row.fieldNames().forEachRemaining(f -> {
                if ((fields == null || fields.contains(f)) && fieldsSet.add(f)) fieldsList.add(f);
            });
        }
        // check that all fields are present
        if (fields != null && !fieldsSet.containsAll(fields)) {
            errorMessage = "Error: not all fields specified were present.";
            return 1;
        }
        final var fieldsMap = new HashMap<String, StringBuilder>();
        for (final var fieldName : fieldsList)
            fieldsMap.put(fieldName, new StringBuilder());
        for (final var row : tree)
            row.fields().forEachRemaining(f -> {
                final var key = f.getKey();
                if (fieldsSet.contains(key))
                    fieldsMap.get(key).append(',').append(f.getValue().toString());
            });
        System.err.println("Done.");

        // write zip
        try (final var fo = new FileOutputStream(outputFile)) {
            try (final var zip = new ZipOutputStream(fo)) {
                for (final var field : fieldsList) {
                    System.err.print("Writing field " + field + "... ");
                    final var entry = new ZipEntry(field);
                    entry.setComment("{\"rows\":" + rowCount + "}");
                    zip.putNextEntry(entry);
                    zip.write('[');
                    zip.write(fieldsMap.get(field).substring(1).getBytes());
                    zip.write(']');
                    zip.closeEntry();
                    System.err.println("Done.");
                }
            }
        }
        return 0;
    }
}