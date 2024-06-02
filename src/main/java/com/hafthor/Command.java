package com.hafthor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class Command {
    String errorMessage, inputFile, outputFile;
    boolean isGzip;
    List<String> fields;

    public static Command CommandFor(final String[] args) {
        if (args.length == 0)
            return new HelpCommand();

        final String command = args[0];
        final var options = Arrays.copyOfRange(args, 1, args.length);
        if ("import".equals(command))
            return new ImportCommand(options);
        if ("export".equals(command))
            return new ExportCommand(options);
        return new ErrorCommand("invalid command " + command);
    }

    protected Command(final String[] args) {
        isGzip = false;
        fields = null;
        inputFile = args.length >= 2 ? args[args.length - 2] : null;
        outputFile = args.length >= 2 ? args[args.length - 1] : null;
        errorMessage = parseOptions(Arrays.copyOfRange(args, 0, Math.max(0, args.length - 2)));
    }

    private String parseOptions(final String[] args) {
        if (inputFile == null || outputFile == null)
            return "missing input/output files";

        for (int i = 0; i < args.length; i++) {
            final String arg = args[i], next = i < args.length - 1 ? args[i + 1] : null;
            if ("-f".equals(arg)) {
                if (next == null || next.isEmpty())
                    return "missing field definitions";
                fields = Arrays.stream(next.split(",")).toList();
                i++;
            } else if ("-g".equals(arg))
                isGzip = true;
            else
                return "invalid option '" + arg + "'";
        }
        return null;
    }

    public int execute() throws IOException {
        if (errorMessage == null)
            return 0;
        System.err.println(errorMessage);
        return 1;
    }
}