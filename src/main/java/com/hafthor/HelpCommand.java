package com.hafthor;

public class HelpCommand extends Command {
    protected HelpCommand() {
        super(new String[]{});
    }

    @Override
    public int execute() {
        out.println("""
                zsvutil syntax:
                  zsvutil import/export [options] inputfile outputfile
                options:
                  -g - export: gzip the output json file / import: gunzip the input json file
                  -f [string] - fields, default is from contents of input file
                example:  # converts customers.json to customers.zsv with default options
                  zsvutil import customers.json customers.zsv
                example:  # converts customers.zsv to customers.json.gz with gzip
                  zsvutil export -g customers.zip customers.tsv.gz
                Note that, at present, zsvutil requires enough memory to hold the entire uncompressed input file in memory.
                Also note that this utility only supports the base specification of zsv and does not support the extended features.
                """);
        return 0;
    }
}