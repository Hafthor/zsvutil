package com.hafthor;

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
        final var cols = new ArrayList<byte[]>();
        try (final var z = new ZipFile(inputFile)) {
            final var e = z.entries();
            while (e.hasMoreElements()) {
                final var entry = e.nextElement();
                names.add(entry.getName());
                try (final var in = z.getInputStream(entry)) {
                    System.err.print("Reading " + entry.getName() + "... ");
                    cols.add(in.readAllBytes());
                    System.err.println("Done.");
                }
            }
        }

        // write output header
        System.err.print("Writing " + outputFile + "... ");
        final var sis = new int[cols.size()];
        if (hasHeader) {
            for (int i = 0; i < names.size(); i++) {
                if (i > 0)
                    out.write(fieldDelimiter);
                out.write(names.get(i).getBytes(StandardCharsets.UTF_8));
            }
            out.write('\n');
        }

        // write output
        final int cl = sis.length, lastCol = cl - 1;
        for (var all = true; all; ) {
            for (int i, c = 0; c < cl; c++) {
                final var line = cols.get(c);
                final int cx = line.length, si = sis[c];
                for (i = si; i < cx; i++)
                    if (line[i] == '\n')
                        break;
                if (i >= cx) {
                    all = false;
                    if (c == 0)
                        break;
                }
                out.write(line, si, i - si);
                out.write(c == lastCol ? '\n' : fieldDelimiter);
                sis[c] = i + 1;
            }
        }
        return 0;
    }
}