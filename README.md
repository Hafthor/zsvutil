# ZSV - Zip Separated Values
### Columnar data storage format

## Abstract
ZSV (Zip Separated Values) is a columnar data storage format with features
similar to Parquet, Orc or Avro, however, it is built upon the simple
technologies of tsv (tab separated values) and zip, making it easy to
understand, create and consume, but still provides the query performance
characteristics of a modern columnar store format.

## Tenants
* Be simple
* Prefer mature, widely available technologies
* Favor human readability

## Description
Given an original source, **products.tsv**, zsvutil import creates a
**products.zsv** file that is just a .zip file with a file inside for each
column, for example, SKU, Description and Price. Inside those files is just the
tsv for that column, compressed.

---
## Columnar Storage
Given **products.tsv** with a header line

| `SKU⇥` | `Description⇥` | `Price⇥`  | `Region⮐` |
|--------|----------------|-----------|-----------|
| `AA⇥`  | `Item AA⇥`     | `111.11⇥` | `US⮐`     |
| `BB⇥`  | `Item BB⇥`     | `222.22⇥` | `US⮐`     |
| `CC⇥`  | `Item CC⇥`     | `333.33⇥` | `US⮐`     |

we would have a zip file products.zsv with the files SKU, Description and Price
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

---
# Additional features
These are features that are not required, but may be useful in some cases. They
are somewhat counter to our tenant of being simple, but they may be useful
enough to warrant the additional complexity. These features are mostly
independent of each other, so you can use one or more of them without using the
others.

---
## Constant Columns
Constant Columns allow us to add an invariant column, which is useful for
partition keys. Note that the field has no trailing newline `⮐`.

### products.zsv
* SKU `AA⮐BB⮐CC⮐`
* Description `Item AA⮐Item BB⮐Item CC⮐`
* Price `111.11⮐222.22⮐333.33⮐`
* Region `US`

---
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

---
## Repeated Columns
Data may be repeated using Compound Columns, if desired, for example:

### products.zsv
* SKU `AA⮐BB⮐CC⮐`
* Description⇥Price `Item AA⇥111.11⮐Item BB⇥222.22⮐Item CC⇥333.33⮐`
* Price `111.11⮐222.22⮐333.33⮐`
* Region `US`

It is up to the reader to decide the optimal combination of zip entries to read
to meet the requirements and avoid reading unnecessary data. The same
combination of columns may appear in a different order, especially when the data
is sorted.

---
## Nested/Binary Data
Data may be nested by storing a zip of compressed row blob files inside the ZSV.

### products.zsv
* SKU `AA⮐BB⮐CC⮐`
* Description⇥Price `Item AA⇥111.11⮐Item BB⇥222.22⮐Item CC⇥333.33⮐`
* ⇥Images (inner stored zip)
  * 0 `<<Image data for AA>>`
  * 1 `<<Image data for BB>>`
  * 2 `<<Image data for CC>>`

Data stored inside, Image data for BB, for example, is directly seekable and
fetchable without reading through any of the other data. The image data itself
may be compressed, but Images zip itself would not be compressed inside
products.zsv.

Note the column name is prefixed with a tab `⇥` character to indicate to the
reader that this is a nested column.

---
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

---
## Metadata
Zip files support having comments on file entries inside. This may be used to
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

---
## CSV Format
While TSV is the preferred inner format for ZSV, a form using CSV is also
possible. Each line has comma separated values and each value is either a quoted
string with JSON escapes possible, a JSON number, or a bare string, but with no
escapes and with forbidden characters.

### products.zsv
* SKU `"AA"⮐"BB"⮐"CC"⮐`
* Description⇥Price `"Item AA",111.11⮐"Item BB",222.22⮐"Item CC",333.33⮐`
* Price `111.11⮐ 222.22⮐ 333.33⮐`
* Region `"US"`
