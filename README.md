# zsvutil
A utility for converting JSON files from/to ZSV files.

# Introducing ZSV - ZIP Separated Values

## TL;DR
ZSV (ZIP Separated Values) is a columnar data storage format with features
similar to [Parquet](https://parquet.apache.org) or 
[ORC](https://orc.apache.org), however, it is built upon the simple
technologies of [JSON](https://www.json.org/json-en.html) and
[ZIP](https://en.wikipedia.org/wiki/ZIP_(file_format)), making it easy to
understand, create and consume, but still provide the query performance
characteristics of a modern columnar store format.

## Tenets
* Be simple - remember: complexity is the enemy of security
* Prefer mature, widely available technologies - every major programming
  language has libraries for JSON and ZIP
* Favor human readability

## Description
Given an original source, **products.json**, zsvutil import creates a
**products.zsv** file that is just a .zip file with a file inside for each
column, for example, SKU, Description and Price. Inside those files is just the
JSON for that column, compressed.

## FAQ
### Why is ZSV built on ZIP file format? Why not use .targz?
ZIP is a widely available, mature technology that is easy to use and has
built-in support in many languages and platforms. .targz is a single gzip
stream of a tar file, which is a collection of files. This makes it
effectively impossible to seek to a specific file without reading and
decompressing the whole stream up to that file. ZIP files are a collection of
individually compressed files, with a directory as a footer to the file, which
makes it easy to seek to a specific file without reading the whole file.

### Why is ZSV built on JSON format? Why not CSV or TSV?
JSON is a simple, human-readable format that is easy to understand and
manipulate. It is also trivial to parse and generate. CSV is also a good
choice but with quoting and escaping rules that can be confusing, ambiguous and
inconsistent. TSV has the limitation of not having any escapes so would have
forbidden characters. That said, CSV and TSV are available as alternate inner
formats.

### Why not use a binary format like Parquet or ORC does?
Binary formats like Parquet or ORC are more read-time efficient than ZSV, but
they are also more complex and are not human-readable. They are also not as
easy to parse or generate as JSON. JSON and other plain text formats are more 
future-proof and expressive than binary formats. For example, it is easier to
specify a numeric column as having a certain precision in a text format than in
a binary format. Likewise, a date time, where you may wish to capture the time
and the precision in the field. Binary formats would require specification of
the schema of the data, which we are trying to avoid. Binary formats are also
more resistant to standard compression algorithms.

### How well is ZSV supported by tools and platforms?
Today ZSV is not widely supported by tools and platforms, but it is easy to
convert between JSON and ZSV using zsvutil. It should be relatively easy to add
support for ZSV to any tool that supports other columnar data formats.

### What is an ideal use case for ZSV?
If you are currently using JSON files and want to improve query performance
without changing your data format, ZSV is a good choice.

## Simple Columnar Storage Example
Given **products.json**

```json
[
  {"SKU": "AA", "Description": "Item AA", "Price": 111.11, "Region": "US"},
  {"SKU": "BB", "Description": "Item BB", "Price": 222.22, "Region": "US"},
  {"SKU": "CC", "Description": "Item CC", "Price": 333.33, "Region": "US"},
  {"SKU": "DD",                           "Price": 444.44, "Region": "US"}
]
```

we would have a ZIP file products.zsv with the files SKU, Description and Price
inside. Each file would have just that column's data.

Note that column names MUST be allowed by .zip format as entry names.

### products.zsv
* SKU `["AA","BB","CC","DD"]`
* Description `["Item AA","Item BB","Item CC",null]`
* Price `[111.11,222.22,333.33,444.44]`
* Region `["US","US","US","US"]`

Note the number of rows in each column MUST be the same, except for Constant 
Columns (see below). The nature of .zip files makes it possible to seek and read
just the columns required without having to read/decode the other columns.

# Additional features
These are features that are not required, but may be useful in some cases. They
are somewhat counter to our tenet of being simple, but they may be useful
enough to warrant the additional complexity. These features are mostly
independent of each other, so you can use one or more of them without using the
others.

## Constant Columns
Constant Columns allow us to add an invariant column, which is useful for
partition keys. Note the constant column is a single un-array-wrapped JSON
string or number.

### products.zsv
* SKU `["AA","BB","CC","DD"]`
* Description `["Item AA","Item BB","Item CC",null]`
* Price `[111.11,222.22,333.33,444.44]`
* Region `"US"`

## Binary Data
It is recommended, but not part of the specification of ZSV, to store binary
data, such as images, as a base64 JSON string.

### products.zsv
* SKU `["AA","BB","CC","DD"]`
* Description `["Item AA","Item BB","Item CC",null]`
* Price `[111.11,222.22,333.33,444.44]`
* Region `"US"`
* Images `["base64image1==","base64image2==",null,"base64image4=="]`

An alternative is to store the binary data in a ZIP file that is embedded in
the zsv file.

### products.zsv
* SKU `["AA","BB","CC","DD"]`
* Description `["Item AA","Item BB","Item CC",null]`
* Price `[111.11,222.22,333.33,444.44]`
* Region `"US"`
* Images (inner stored ZIP - not compressed)
  * 0 `<<Compressed image data for AA>>`
  * 1 `<<Compressed image data for BB>>`
  * 3 `<<Compressed image data for DD>>`

Note that the inner Images file is not a .zsv file, but a regular .zip file with
one file for each record that has a non-null value. The inner file name MUST
be the record number of the data relative to the other columns. In the example
above, row 2 has a null value for the image, so there is no file for it in the
inner ZIP file. The inner ZIP file SHOULD NOT be stored with compression in the
ZSV file, however, the individual inner files SHOULD be compressed. This is to
allow for seek operations to access the individual files in the inner ZIP file.

## Row Groups
While it is recommended to use ZSV file-level partitioning, row groups may be
used to split up longer data sets inside a bigger .zsv. This is done by 
repeating the column file names followed by a double underscore `__`
and a unique number for each rowgroup.

### products.zsv
* SKU__0 `["AA","BB"]`
* Description__0 `["Item AA","Item BB"]`
* Price__0 `[111.11,222.22]`
* Region__0 `"US"`
* SKU__1 `["CC","DD"]`
* Description__1 `["Item CC",null]`
* Price__1 `[333.33,444.44]`
* Region__1 `"US"`

Note the number of rows in each column of the row group MUST be equal. The
columns referenced in each row group MUST be equal. Columns referenced in each
row group SHOULD be in the same order and grouped together, however, this is not
a strict requirement and readers MUST NOT assume an order of files. Constant
columns may be different in each row group when named with the double underscore
or there can be a single constant column as though there were no rowgroups.

### products.zsv
* SKU__0 `["AA","BB"]`
* Description__0 `["Item AA","Item BB"]`
* Price__0 `[111.11,222.22]`
* SKU__1 `["CC","DD"]`
* Description__1 `["Item CC",null]`
* Price__1 `[333.33,444.44]`
* Region `"US"`

## Metadata
ZIP files support having comments on file entries inside. This may be used to
hold metadata about the contents that are otherwise unavailable, such as row
counts, partition information, sorting, distinct values, min/max text or values,
all in a JSON format.

### products.zsv
* SKU _{"format":"JSON", "rows":4, "distinct":4, "maxlength":2, "minimum":"AA", "maximum":"DD"}_ `["AA","BB","CC","DD"]`
* Description _{"format":"JSON", "rows":4, "distinct":3, "maxlength":7}_ `["Item AA","Item BB","Item CC",null]`
* Price _{"format":"JSON", "rows":4, "distinct":4, "minimum":111.11, "maximum":444.44}_ `[111.11,222.22,333.33,444.44]`
* Region _{"format":"JSON", "constant":true}_ `"US"`

Note that the metadata specification of "format" being "JSON" is an optional
indicator of the inner format of the column, not the metadata itself. The
metadata is always JSON.

## Encryption
ZIP files support encryption of individual files. This may be used to store
sensitive data in a ZSV file. How the keys are stored and managed is outside
the scope of this specification, but it is recommended to use file metadata to
store the encryption key ID for each encrypted column file.

## Alternative CSV/TSV Inner Formats
While JSON is the preferred inner format for ZSV, a form using 
[CSV](https://en.wikipedia.org/wiki/Comma-separated_values) or
[TSV](https://en.wikipedia.org/wiki/Tab-separated_values) is also possible. 
For CSV, each line has comma separated values and each value is either a quoted
string with JSON escapes possible, a number, or a bare string, but with no
escapes or forbidden characters. For TSV, each line has tab separated values
and each value is just an unescaped unquoted string with no escapes or forbidden
characters (i.e. tab `⇥` or newline `⮐`).

### products.zsv 
* SKU _{"format":"CSV"}_ `"AA"⮐"BB"⮐"CC"⮐"DD"⮐`
* Description _{"format":"CSV"}_ `"Item AA"⮐"Item BB"⮐"Item CC"⮐⮐`
* Price _{"format":"CSV"}_ `111.11⮐ 222.22⮐ 333.33⮐444.44⮐`
* Region _{"format":"CSV", "constant":true}_ `"US"`

### products.zsv
* SKU _{"format":"TSV"}_ `AA⮐BB⮐CC⮐DD⮐`
* Description _{"format":"TSV"}_ `Item AA⮐Item BB⮐Item CC⮐⮐`
* Price _{"format":"TSV"}_ `111.11⮐ 222.22⮐ 333.33⮐444.44⮐`
* Region _{"format":"TSV", "constant":true}_ `US`

Note that the numbers are not ints or floats, but are just unquoted strings
that represent a number of arbitrary scale and precision. Also note that
neither alternative inner format supports null values as being distinct from
empty strings.