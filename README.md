[![Build Status](https://travis-ci.org/aaronmck/FlashFry.svg?branch=master)](https://travis-ci.org/aaronmck/FlashFry)
[![codecov](https://codecov.io/gh/aaronmck/FlashFry/branch/master/graph/badge.svg)](https://codecov.io/gh/aaronmck/FlashFry)
[![Join the chat at https://gitter.im/FlashFry/Lobby](https://badges.gitter.im/FlashFry/Lobby.svg)](https://gitter.im/FlashFry/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

<p align="center">
<img src="https://raw.githubusercontent.com/aaronmck/FlashFry/master/images/fries.png" width="500">
</p>

FlashFry is a fast and flexible command-line tool for characterizing large numbers of potential CRISPR target sequences. 

Sections:
- [Quickstart](#quickstart)
- [General documentation](#documentation)
- [FAQ](#faq)


# Quickstart

From the UNIX or Mac command line (will take some adjustment on Windows), download the latest release version of the FlashFry jar file:

```shell
wget https://github.com/aaronmck/FlashFry/releases/download/1.2.0/FlashFry-assembly-1.2.jar
```
download and then un-gzip the sample data for human chromosome 22:

```shell
wget https://raw.githubusercontent.com/aaronmck/FlashFry/master/test_data/quickstart_data.tar.gz
tar xf quickstart_data.tar.gz
```

then run the database creation step (this should take a few minutes, takes ~137 seconds on my laptop):

```shell
mkdir tmp
java -Xmx4g -jar FlashFry-assembly-1.2.jar \
 --analysis index \
 --tmpLocation ./tmp \
 --database chr22_cas9ngg_database \
 --reference chr22.fa.gz \
 --enzyme spcas9ngg
```

discover candidate targets and their potential off-target in the test data (takes a few seconds):

```shell
java -Xmx4g -jar FlashFry-assembly-1.2.jar \
 --analysis discover \
 --database chr22_cas9ngg_database \
 --fasta EMX1_GAGTCCGAGCAGAAGAAGAAGGG.fasta \
 --output EMX1.output
```

finally score the discovered sites:

```shell
java -Xmx4g -jar FlashFry-assembly-1.2.jar \
 --analysis score \
 --input EMX1.output \
 --output EMX1.output.scored \
 --scoringMetrics doench2014ontarget,doench2016cdf,dangerous,crisprmit \
 --database chr22_cas9ngg_database
```

# Documentation

FlashFry requires the Java virtual machine (JVM) to run. This is on almost every system imaginable these days, so it's probably already on your machine. We've tested it with both Oracle's Java as well as using the open JVM. Other requirements include:

- Your reference genome as a fasta file
- The region you'd like to score, as a fasta file
- A computer, with the command line terminal open
- Java 1.8

Once you have the requirements setup, there are three main steps to running FlashFry.

1) First, you build a database using the specified CRISPR motif against the target database using the `--analysis index` option. This is only done once, as the database is reuseable. You have to choose the enzyme time to use while indexing. As of writing this includes the Cas9s with 23 bp targets: SpCas9 (NAG or NGG), SpCas9NGG (NGG), SpCas9NAG (NAG), and Cpf1 (TTTN) with 24 basepair targets. This are adjustable in the code, or you can create your own. In writing the database temporary files are put in the --tempLocation location. This will take up a bit more space than the final database (maybe 10-20% depending on how duplicated genome targets are). Runtimes on a pretty slow drive look like (formated hours:minutes:seconds):

| Genome / version | Cas9 (NGG) | Cas9 (NGG/NAG) | CPF1 (TTTN) |
| :------------- |-------------:| -----:| -----:|
| Caenorhabditis elegans - 235 | 0:3:21 | 0:6:03 | 0:5:35 | 
| Human - hg38 | 3:19:29 | 5:24:55 | 2:50:59 | 
| Mouse - mm10 | 2:36:53 | 4:36:03 | 2:11:35 | 
| Drophellia melanegaster - BDGP6 | 0:6:33 | 0:10:48 | 0:5:44 |


2) The next step is to find candidate targets within the fasta sequence of interest. The `--analysis discover` options handles this. The candidates found in the fasta are then run against the off-targets database, and an annotated output file is produced. This output file is BED-file compatible, and contains an annotated header section with the chromosomes of the database you ran against (for reference later). 


3) Lastly, you can score this annotated output file. This is handled by the `--analysis score` module. We've implemented a fair number of scoring metrics. For guidance on which are appropriate in which situation, please see the wonderful paper by [Maximilian Haeussler](https://www.ncbi.nlm.nih.gov/pubmed/27380939) which analyzed all of these methods in aggregate:

- `hsu2013` - The Hsu et. al. method, also known as crispr.mit.edu score: [Pubmed](https://www.ncbi.nlm.nih.gov/pubmed/23873081.0)
- `doench2014ontarget` - Doench 2014 on-target efficiency score [Pubmed](https://www.ncbi.nlm.nih.gov/pubmed/25184501)
- `doench2016cfd` - The Doench 2016 cutting frequency determination score [Pubmed](https://www.ncbi.nlm.nih.gov/pubmed/26780180)
- `moreno2015` - Moreno-Mateos and Vejnar's CRISPRscan on-target method [Pubmed](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC4589495/)

We've also implemented a some additional metrics that are useful in CRISPR library creation:

- `bedannotator` - annotation your output targets with information from an associated BED file. It can find the 0 mismatch targets in the genome database and use those to infer the genomic location
- `dangerous` - annotate targets that have dangerous sequence features, such as high or extremely low GC, polIII transcriptional terminators, or low entropy. 
- `minot` - add a column that indicates the minimum mismatches to any off-target hit. 


## Binary file format

The tool uses a custom binary format that compresses the genome hits for a target sequences into binary values. Values are stored as using Java's default big-endian format. The database itself contains all the packed long values in a compressed file, using htslib's block-compressed file readers and writers.  The header file sits alongside the database and provides a lookup table for the target database.

The block format in the database is:
- block type: currently 0 for linear, 1 for sub-indexed (long)
- if indexed, for the set index size, a series of long values that describe the size and location of sub-indexes
- After any header, a series of targets, with X position encodings, set by the count number in the target encoding

The header format:
- 64 bit magic value (long)
- 64 bit version number (long)
- 64 bit internal enzyme number (long, see StandardScanParameters.scala for the enumerated enzymes)
- 64 bit number of bins (long)

- for the number of bins in the header:
  - 64 bit offset of this block in the file (long)
  - 64 bit size of this block in the file (in uncompressed bytes, long)
  - 64 bit number of targets contained within the bin (int)

- for each contig in the input reference file:
  - the contig name, followed by a '=' and it's index position in the reference

For lookup we perform something like the following:
![FlashFrySearch](https://raw.githubusercontent.com/aaronmck/DeepFry/master/images/document_format.png)


# FAQ

Why seperate the off-target discovery and scoring parts of FlashFry?

Off-target discovery can have high computational costs for large putitive target sets (say 10,000 to 100,000s of candidate guides). To avoid having to do this step every time you'd like to switch scoring metrics, we thought it was best to split the two stages up.

Why the does the output file look the way it does?

We wanted the output to work with common analysis tools such as BEDTools. This meant a format that encoded specific details into BED-file columns, as well as leaving off the traditional header line in favor of listing column details in the header section. It should be 

