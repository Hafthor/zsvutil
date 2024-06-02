package com.hafthor;

public class ExportCommandV2 extends Command {
    public ExportCommandV2(final String[] args) {
        super(args);
    }

    /* How this will work
    Export works by reading a ZIP file's entries as byte streams, one byte
    stream for each field file entry in the ZIP file. The output JSON file is
    created by combining each field file's row into a JSON object that becomes
    a row in the output JSON array.

    Here's an example:
    example.zsv:
        source:
        [ //012
            "a",
            "b",
            "c"
        ]
        sku:
        [ //0
            1,
            2,
            3
        ]
        description:
        [ //0123456
            "desc1",
            "desc2",
            "desc3"
        ]
        timestamp:
        [ //0123456789012345678901
            "2020-01-01T00:00:00Z",
            "2020-01-02T00:00:00Z",
            "2020-01-03T00:00:00Z"
        ]

    example.json:
    [ //           012         0                 0123456               0123456789012345678901 0
        {"source": "a", "sku": 1, "description": "desc1", "timestamp": "2020-01-01T00:00:00Z"},
        {"source": "b", "sku": 2, "description": "desc2", "timestamp": "2020-01-02T00:00:00Z"},
        {"source": "c", "sku": 3, "description": "desc3", "timestamp": "2020-01-03T00:00:00Z"}
    ]

    The reader reads the ZIP's field files as byte streams and doesn't really
    parse it so much as it finds the start and end of the JSON values and copies
    them to the output.
     */
}
