package com.hafthor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
        final var inlines = in.readAllBytes();
        System.err.println("Done.");
        final var line = new ArrayList<String>();
        final var fd = fieldDelimiter;
        int start = 0;

        // header scan
        for (int si = 0, sl = 0; si < inlines.length; si++) {
            final byte b = inlines[si];
            if (b == '\n') {
                if (hasHeader)
                    start = si + 1;
                line.add(new String(inlines, sl, si - sl));
                break;
            } else if (b == fd) {
                line.add(new String(inlines, sl, si - sl));
                sl = si + 1;
            }
        }
        int lineCount = hasHeader ? -1 : 0;
        for (int i = 0; i < inlines.length; i++)
            if (inlines[i] == '\n')
                lineCount++;

        // get field names
        if (fields == null) {
            fields = line;
            if (!hasHeader)
                for (int i = 0; i < fields.size(); i++)
                    fields.set(i, "field" + i);
        }
        if (fields.size() < line.size())
            for (int i = fields.size(); i < line.size(); i++)
                fields.add("field" + i);

        // write zip
        try (final var fo = new FileOutputStream(outputFile)) {
            try (final var zip = new ZipOutputStream(fo)) {
                final int colCount = fields.size();
                for (int col = 0; col < colCount; col++) {
                    final var name = fields.get(col);
                    System.err.print("Writing field " + name + "... ");
                    final var entry = new ZipEntry(name);
                    entry.setComment("{rows:" + lineCount + "}");
                    zip.putNextEntry(entry);
                    for (int si = start, c = 0, cx = inlines.length; si < cx; si++) {
                        for (; c < col && si < cx; si++) {
                            final var b = inlines[si];
                            if (b == '\n') {
                                c = col + 1;
                                System.err.print('!');
                            } else if (b == fd)
                                c++;
                        }
                        if (c == col) {
                            final int sf = si;
                            for (; si < cx; si++) {
                                var b = inlines[si];
                                if (b == fd || b == '\n')
                                    break;
                            }
                            zip.write(inlines, sf, si - sf);
                        }
                        zip.write('\n');
                        if (c < colCount - 1) {
                            while (inlines[si] != '\n') si++;
                        } else {
                            for (; ; si++) {
                                final var b = inlines[si];
                                if (b == fd)
                                    System.err.print('?');
                                if (b == '\n')
                                    break;
                            }
                        }
                        c = 0;
                    }
                    zip.closeEntry();
                    System.err.println("Done.");
                }
            }
        }
        return 0;
    }
}