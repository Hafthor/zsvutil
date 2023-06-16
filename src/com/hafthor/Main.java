package com.hafthor;

import java.io.IOException;

public class Main {
    // import -g -f "source,sku,description,timestamp" delta1-0.utsv.gz delta1-0.zsv
    // export -g delta1-0.zsv delta1-0.utsv.gz
    public static void main(final String[] args) throws IOException {
        System.exit(Command.CommandFor(args).execute());
    }
}