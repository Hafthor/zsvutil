package com.hafthor;

public class HelpCommand extends Command {
    protected HelpCommand() {
        super(new String[]{});
    }

    @Override
    public int execute() {
        System.out.println("zsvutil syntax:\n" +
                "  zsvutil import/export [options] inputfile outputfile\n" +
                "options:\n" +
                "  -g - export: gzip the output json file / import: gunzip the input json file\n" +
                "  -f [string] - fields, default is from contents of input file\n" +
                "example:  # converts customers.json to customers.zsv with default options\n" +
                "  zsvutil import customers.json customers.zsv\n" +
                "example:  # converts customers.zsv to customers.json.gz with gzip\n" +
                "  zsvutil export -g customers.zip customers.tsv.gz\n" +
                "Note that, at present, zsvutil requires enough memory to hold the entire uncompressed input file in memory.\n" +
                "Also note that this utility only supports the base specification of zsv and does not support the extended features.\n");
        return 0;
    }
}