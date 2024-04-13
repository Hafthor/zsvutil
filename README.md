# zsvutil
A utility for converting TSV files from/to ZSV files.

# Introducing ZSV - ZIP Separated Values

## TL;DR
ZSV (ZIP Separated Values) is a columnar data storage format with features
similar to [Parquet](https://parquet.apache.org), [Orc](https://orc.apache.org)
or [Avro](https://avro.apache.org), however, it is built upon the simple
technologies of 
[TSV (tab separated values)](https://en.wikipedia.org/wiki/Tab-separated_values)
and [ZIP](https://en.wikipedia.org/wiki/ZIP_(file_format)), making it easy to
understand, create and consume, but still provide the query performance
characteristics of a modern columnar store format. 

## Tenants
* Be simple
* Prefer mature, widely available technologies
* Favor human readability
* Be easy to parse and generate
* Be efficient for simple tabular data
* Prefer longevity over novelty

## Description
Given an original source, **products.tsv**, zsvutil import creates a
**products.zsv** file that is just a .zip file with a file inside for each
column, for example, SKU, Description and Price. Inside those files is just the
TSV for that column, compressed.

## FAQ
### Why is ZSV built on ZIP file format? Why not use .targz?
ZIP is a widely available, mature technology that is easy to use and has
built-in support in many languages and platforms. .targz is a single gzip
stream of a tar file, which is a collection of files. This makes it
effectively impossible to seek to a specific file without reading and
decompressing the whole stream up to that file. ZIP files are a collection of
individually compressed files, with a directory as a footer to the file, which
makes it easy to seek to a specific file without reading the whole file.

### Why is ZSV built on TSV format? Why not CSV? Why not JSON?
TSV is a simple, human-readable format that is easy to understand and
manipulate. It is also trivial to parse and generate. CSV is also a good
choice, but it is more complex than TSV, with quoting and escaping rules
that can be confusing, ambiguous and inconsistent. JSON is a good format
for nested data, but it is not as easy to read or write as TSV. JSON is 
also not as efficient as TSV for simple tabular data.

### What are some key shortcomings of ZSV?
ZSV is not a good choice for binary or unstructured textual data. The main
limitation is that the data in the columns must not include the tab character
`⇥` or newline character `⮐`. This is a limitation of the TSV format. Any
escaping or encoding of these characters would make the format less
human-readable, harder to parse and could introduce ambiguity and consistency
problems.

### How well is ZSV supported by tools and platforms?
Today ZSV is not widely supported by tools and platforms, but it is easy to
convert between TSV and ZSV using zsvutil. It should be relatively easy to add
support for ZSV to any tool that supports columnar data formats.

### What is an ideal use case for ZSV?
If you are currently using TSV files and want to improve query performance
without changing your data format, ZSV is a good choice.

## Simple Columnar Storage Example
Given **products.tsv** with a header line

| `SKU⇥` | `Description⇥` | `Price⇥`  | `Region⮐` |
|--------|----------------|-----------|-----------|
| `AA⇥`  | `Item AA⇥`     | `111.11⇥` | `US⮐`     |
| `BB⇥`  | `Item BB⇥`     | `222.22⇥` | `US⮐`     |
| `CC⇥`  | `Item CC⇥`     | `333.33⇥` | `US⮐`     |

we would have a ZIP file products.zsv with the files SKU, Description and Price
inside. Each file would have just that column's data.

Note that column names MUST be allowed by .zip format as entry names. Also, the 
tab character `⇥` MUST NOT be used in the name.

### products.zsv
* SKU `AA⮐BB⮐CC⮐`
* Description `Item AA⮐Item BB⮐Item CC⮐`
* Price `111.11⮐222.22⮐333.33⮐`
* Region `US⮐US⮐US⮐`

Note the number of rows in each column MUST be the same, except for Constant 
Columns (see below). The nature of .zip files makes it possible to seek and read
just the columns required without having to read/decode the other columns. Note
that newline `⮐` MUST NOT appear in the actual column data since it is used to
separate rows. Note that each column row MUST end with a `⮐` including the last
one.

# Additional features
These are features that are not required, but may be useful in some cases. They
are somewhat counter to our tenant of being simple, but they may be useful
enough to warrant the additional complexity. These features are mostly
independent of each other, so you can use one or more of them without using the
others.

## Constant Columns
Constant Columns allow us to add an invariant column, which is useful for
partition keys. Note that the field has no trailing newline `⮐`.

### products.zsv
* SKU `AA⮐BB⮐CC⮐`
* Description `Item AA⮐Item BB⮐Item CC⮐`
* Price `111.11⮐222.22⮐333.33⮐`
* Region `US`

## Compound Columns
If a collection of columns are always accessed together, it may make sense to
combine them, for example if SKU and Description were never accessed
independently, we could make **products.zsv** look like this:

### products.zsv
* SKU `AA⮐BB⮐CC⮐`
* Description⇥Price `Item AA⇥111.11⮐Item BB⇥222.22⮐Item CC⇥333.33⮐`
* Region `US`

Note that Constant Columns MUST NOT participate in Compound Columns. Note that
along with newline `⮐`, the tab `⇥` character MUST NOT appear in the column
data in a Compound Column. Each row in any column MUST include the same number
of columns as its entry name.

## Repeated Columns
Data may be repeated using Compound Columns, if desired, for example:

### products.zsv
* SKU `AA⮐BB⮐CC⮐`
* Description⇥Price `Item AA⇥111.11⮐Item BB⇥222.22⮐Item CC⇥333.33⮐`
* Price `111.11⮐222.22⮐333.33⮐`
* Region `US`

It is up to the reader to decide the optimal combination of ZIP entries to read
to meet the requirements and avoid reading unnecessary data. The same
combination of columns may appear in a different order, especially when the data
is sorted.

## Nested/Binary Data
Data may be nested by storing a ZIP of compressed row blob files inside the ZSV.

### products.zsv
* SKU `AA⮐BB⮐CC⮐`
* Description⇥Price `Item AA⇥111.11⮐Item BB⇥222.22⮐Item CC⇥333.33⮐`
* ⇥Images (inner stored ZIP)
  * 0 `<<Image data for AA>>`
  * 1 `<<Image data for BB>>`
  * 2 `<<Image data for CC>>`

Data stored inside, Image data for BB, for example, is directly seekable and
fetchable without reading through any of the other data. The image data itself
may be compressed, but Images ZIP itself would not be compressed inside
products.zsv.

Note the column name is prefixed with a tab `⇥` character to indicate to the
reader that this is a nested column.

## Row Groups
Row Groups may be used to split up longer data sets inside a bigger .zsv. This
is done by repeating the column file names followed by a double tab `⇥⇥` and a
unique number for each rowgroup.

### products.zsv
* SKU⇥⇥0 `AA⮐BB⮐`
* Description⇥Price⇥⇥0 `Item AA⇥111.11⮐Item BB⇥222.22⮐`
* Region⇥⇥0 `US`
* SKU⇥⇥1 `CC⮐`
* Description⇥Price⇥⇥1 `Item CC⇥333.33⮐`
* Region⇥⇥1 `US`

Note the number of rows in each column of the row group MUST be equal. The
columns referenced in each row group MUST be equal. Columns referenced in each
row group SHOULD be in the same order and grouped together, however, this is not
a strict requirement and readers MUST NOT assume an order of files. Constant
columns may be different in each row group when named with the double tab or
there can be a single constant column as though there were no rowgroups.

### products.zsv
* SKU⇥⇥0 `AA⮐BB⮐`
* Description⇥Price⇥⇥0 `Item AA⇥111.11⮐Item BB⇥222.22⮐`
* SKU⇥⇥1 `CC⮐`
* Description⇥Price⇥⇥1 `Item CC⇥333.33⮐`
* Region `US`

## Metadata
ZIP files support having comments on file entries inside. This may be used to
hold metadata about the contents that are otherwise unavailable, such as row
counts, partition information, sorting, distinct values, min/max text or values,
all in a bare keyname JSON format.

### products.zsv
* SKU⇥⇥0 _{rows:2, distinct:2, maxlength:2, min:"AA", max:"BB"}_ `AA⮐BB⮐`
* Description⇥⇥0 _{rows:2, distinct:2, maxlength:7}_ `Item AA⮐Item BB⮐`
* Price⇥⇥0 _{rows:2, distinct:2, minvalue:111.11, maxvalue:222.22}_ `111.11⮐222.22⮐`
* SKU⇥⇥1 _{rows:1, distinct:1, maxlength:2, min:"CC", max:"CC"}_ `CC⮐`
* Description⇥⇥1 _{rows:1, distinct:1, maxlength:7}_ `Item CC⮐`
* Price⇥⇥1 _{rows:1, distinct:1, minvalue:333.33, maxvalue:333.33}_ `333.33⮐`
* Region _{}_ `US`

## Alternative CSV Inner Format
While TSV is the preferred inner format for ZSV, a form using CSV is also
possible. Each line has comma separated values and each value is either a quoted
string with JSON escapes possible, a JSON number, or a bare string, but with no
escapes and with forbidden characters.

### products.zsv
* SKU `"AA"⮐"BB"⮐"CC"⮐`
* Description⇥Price `"Item AA",111.11⮐"Item BB",222.22⮐"Item CC",333.33⮐`
* Price `111.11⮐ 222.22⮐ 333.33⮐`
* Region `"US"`