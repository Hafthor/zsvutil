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
        System.out.println("  -d [char] - field delimiter - default \"\t\"");
        System.out.println("  -h - export: add header line to output csv / import: input file has header");
        System.out.println("  -g - export: gzip the output csv file / import: gunzip the input csv");
        System.out.println("  -f [string] - fields, default is from header if available or \"field1,field2,...\"");
        System.out.println("example:  # converts customers.tsv to customers.zsv with default options");
        System.out.println("  zsvutil import customers.tsv customers.zsv");
        System.out.println("example:  # converts customers.zsv to customers.csv.gz with headers and gzip");
        System.out.println("  zsvutil export -d , -h -g customers.zip customers.tsv.gz");
        System.out.println("Note that, at present, zsvutil requires enough memory to hold the entire uncompressed input file in memory.");
        System.out.println("Also note that this utility only supports the base specification of zsv and does not support the extended features.");
        return 0;
    }
}