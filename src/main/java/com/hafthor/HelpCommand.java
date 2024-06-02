package com.hafthor;

public class HelpCommand extends Command {
    protected HelpCommand() {
        super(new String[]{});
    }

    @Override
    public int execute() {
        System.out.println("zsvutil syntax:");
        System.out.println("  zsvutil import/export [options] inputfile outputfile");
        System.out.println("options:");
        System.out.println("  -g - export: gzip the output json file / import: gunzip the input json file");
        System.out.println("  -f [string] - fields, default is from contents of input file");
        System.out.println("example:  # converts customers.json to customers.zsv with default options");
        System.out.println("  zsvutil import customers.json customers.zsv");
        System.out.println("example:  # converts customers.zsv to customers.json.gz with gzip");
        System.out.println("  zsvutil export -g customers.zip customers.tsv.gz");
        System.out.println("Note that, at present, zsvutil requires enough memory to hold the entire uncompressed input file in memory.");
        System.out.println("Also note that this utility only supports the base specification of zsv and does not support the extended features.");
        return 0;
    }
}