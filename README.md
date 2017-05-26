[![Build Status](https://travis-ci.org/aaronmck/FlashFry.svg?branch=master)](https://travis-ci.org/aaronmck/FlashFry)
[![codecov](https://codecov.io/gh/aaronmck/DeepFry/branch/master/graph/badge.svg)](https://codecov.io/gh/aaronmck/FlashFry)
[![Join the chat at https://gitter.im/FlashFry/Lobby](https://badges.gitter.im/FlashFry/Lobby.svg)](https://gitter.im/FlashFry/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


![FlashFry_logo](https://raw.githubusercontent.com/aaronmck/FlashFry/master/images/fries.png)

FlashFry is a fast and flexible command-line tool for characterizing large numbers of CRISPR guide sequences. 

Quickstart
=======

- get the jar file: link here
- get the sample data:
- untar the sample data: 
- run database creation (should take a few minutes):

```bash
mkdir tmp; java -jar FlashFry-assembly-1.2.jar --analysis index --tmpLocation ./tmp --database chr22_cas9ngg_database --reference chr22.fa.gz --enzyme spcas9ngg
```

- discover targets and their potential off-target in the test data (takes a few seconds):

```bash
java -jar FlashFry-assembly-1.2.jar --analysis discover --database chr22_cas9ngg_database --fasta EMX1_GAGTCCGAGCAGAAGAAGAAGGG.fasta --output EMX1.output
```

- score the discovered sites:

```bash
java -jar FlashFry-assembly-1.2.jar --analysis score --database chr22_cas9ngg_database --fasta EMX1_GAGTCCGAGCAGAAGAAGAAGGG.fasta --output EMX1.output
```





Getting started - a more detailed description
=======

There are three main steps to running FlashFry.

1) First, you build a database using the specified CRISPR motif against the target database. This is only done once, as the database is reuseable.

2) Secondly, you discover any potential targets within the sequence of interest. These are then run against the off-targets database, and an annotated output file is produced.

3) Lastly, you can score this annotated output file.


FAQ
=======

Why seperate the off-target discovery and scoring parts of FlashFry?

Off-target discovery can have high computational costs for large putitive target sets. To avoid having to do this step every time you'd like to switch scoring metrics, we thought it was best to split the two stages up.

Why the does the output file look the way it does? (aka I don't like it)

We wanted the output to work with common analysis tools such as BEDTools. This meant a format like encoded specific details into specific columns, as well as leaving off the traditional header in favor of listing column details in the header section

