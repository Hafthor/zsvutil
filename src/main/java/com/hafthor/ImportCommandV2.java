package com.hafthor;

public class ImportCommandV2 extends Command {
    public ImportCommandV2(final String[] args) {
        super(args);
    }

    /* How this will work
    Import works by reading a JSON file as a byte stream in multiple passes to
    create the JSON streams for each field.

    The first pass reads the JSON file to get the field names and the row count.

    The second pass reads the JSON file to filter out the fields that are not
    the first field. The third pass filters out the fields that are not the
    second field, and so on.

    Here's an example:
    example.json:
    [ //           012         0                 0123456               0123456789012345678901 0
        {"source": "a", "sku": 1, "description": "desc1", "timestamp": "2020-01-01T00:00:00Z"},
        {"source": "b", "sku": 2, "description": "desc2", "timestamp": "2020-01-02T00:00:00Z"},
        {"source": "c", "sku": 3, "description": "desc3", "timestamp": "2020-01-03T00:00:00Z"}
    ]

    The first pass reads the JSON file to get the field names and the row count:
    fieldsList = ["source", "sku", "description", "timestamp"]
    rowCount = 3

    The second pass reads the JSON file to filter out the fields that are not
    'source' to create a zip stream for a file named 'source':
    [
        "a",
        "b",
        "c"
    ]

    The reader reads the JSON file as a byte stream and doesn't really parse it
    so much as it finds the start and end of the JSON objects and copies them to
    the output.
     */
}
