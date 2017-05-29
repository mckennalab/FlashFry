[![Build Status](https://travis-ci.org/aaronmck/FlashFry.svg?branch=master)](https://travis-ci.org/aaronmck/FlashFry)
[![codecov](https://codecov.io/gh/aaronmck/FlashFry/branch/master/graph/badge.svg)](https://codecov.io/gh/aaronmck/FlashFry)
[![Join the chat at https://gitter.im/FlashFry/Lobby](https://badges.gitter.im/FlashFry/Lobby.svg)](https://gitter.im/FlashFry/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


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

There are three main steps to running FlashFry.

1) First, you build a database using the specified CRISPR motif against the target database. This is only done once, as the database is reuseable. You have to choose the enzyme time to use while indexing. As of writing this includes the Cas9s with 23 bp targets: SpCas9 (NAG or NGG), SpCas9NGG (NGG), SpCas9NAG (NAG), and Cpf1 (TTTN) with 24 basepair targets. This are adjustable in the code, or you can create your own. In writing the database temporary files are put in the --tempLocation location. This will take up a bit more space than the final database (maybe 10-20% depending on how duplicated genome targets are). Runtimes on a pretty slow drive look like:

| Genome / version | Cas9 (NGG) | Cas9 (NGG/NAG) | CPF1 (TTTN) |
| ------------- |-------------:| -----:| -----:|
| Caenorhabditis elegans - 235 | 0:3:21 | 0:6:03 | 0:5:35 | 
| Human - hg38 | 3:19:29 | 5:24:55 | 2:50:59 | 
| Mouse - mm10 | 2:36:53 | 4:36:03 | 2:11:35 | 
| Drophellia melanegaster - BDGP6 | 0:6:33 | 0:10:48 | 0:5:44 |

2) Secondly, you discover any potential targets within the sequence of interest. These are then run against the off-targets database, and an annotated output file is produced.

3) Lastly, you can score this annotated output file.


# FAQ

Why seperate the off-target discovery and scoring parts of FlashFry?

Off-target discovery can have high computational costs for large putitive target sets. To avoid having to do this step every time you'd like to switch scoring metrics, we thought it was best to split the two stages up.

Why the does the output file look the way it does?

We wanted the output to work with common analysis tools such as BEDTools. This meant a format like encoded specific details into specific columns, as well as leaving off the traditional header in favor of listing column details in the header section

