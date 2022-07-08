[![Build Status](https://travis-ci.com/mckennalab/FlashFry.svg?branch=master)](https://travis-ci.com/aaronmck/FlashFry)
[![codecov](https://codecov.io/gh/mckennalab/FlashFry/branch/master/graph/badge.svg)](https://codecov.io/gh/aaronmck/FlashFry)
[![Join the chat at https://gitter.im/FlashFry/Lobby](https://badges.gitter.im/FlashFry/Lobby.svg)](https://gitter.im/FlashFry/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.1291646.svg)](https://doi.org/10.5281/zenodo.1291646)

*Of note: If you've been using FlashFry before version 1.9, the command-line system has changed slightly.*
<p align="center">
<img src="https://raw.githubusercontent.com/aaronmck/FlashFry/master/images/fries.png" width="500">
</p>

FlashFry is a fast and flexible command-line tool for characterizing large numbers of potential CRISPR target sequences. FlashFry can be used with any genome, and can run against non-traditional model organisms or transcriptomes. If you're looking to characterize a smaller region or would like a nice web interface we recommend the [GT-scan](http://gt-scan.csiro.au) or [crispor.org](http://crispor.org) websites.

The easiest way to get started it to try out the quick-start procedure to make sure everything works on your system. If everything looks good, there are few more in-depth tutorials to try out various capacities of FlashFry. Thanks to @drivenbyentropy for the Java implementation of the ViennaRNA energy calculations.

#### links:
- [Quick start](https://github.com/aaronmck/FlashFry#quickstart)
- [General options documentation](https://github.com/aaronmck/FlashFry/wiki/Command-line-options)
- [Scoring methods](https://github.com/aaronmck/FlashFry/wiki/Site-scoring)
- [FAQ](https://github.com/aaronmck/FlashFry/wiki/Frequently-asked-questions)
- [Database format documentation](https://github.com/aaronmck/FlashFry/wiki/binary-format)
- Tutorials - on the [wiki](https://github.com/aaronmck/FlashFry/wiki)
  * [Call target sites in a FASTA and annotate with BED intervals](https://github.com/aaronmck/FlashFry/wiki/End-to-end-scoring-and-annotation-with-FlashFry)
  * [Create random target sequences and score them against the genome](https://github.com/aaronmck/FlashFry/wiki/Scoring-random-sequences-against-the-genome)
- [Citing FlashFry](https://github.com/aaronmck/FlashFry#cite)
 


# Quickstart

First, make sure you're running Java version 8 (type ```java -version``` on the command line to see the version). From the UNIX or Mac command line, download the latest release version of the FlashFry jar file:

```shell
wget https://github.com/mckennalab/FlashFry/releases/download/1.14/FlashFry-assembly-1.14.jar
```
Download and then un-gzip the sample data for human chromosome 22:

```shell
wget https://raw.githubusercontent.com/aaronmck/FlashFry/master/test_data/quickstart_data.tar.gz
tar xf quickstart_data.tar.gz
```

Then run the database creation step (this should take a few minutes, it takes ~75 seconds on my laptop):

```shell
mkdir tmp
java -Xmx4g -jar FlashFry-assembly-1.14.jar \
 index \
 --tmpLocation ./tmp \
 --database chr22_cas9ngg_database \
 --reference chr22.fa.gz \
 --enzyme spcas9ngg
```

Now we discover candidate targets and their potential off-target in the test data (takes a few seconds). Here we're using the EMX1 target with some  sequence flanking the target site. This flanking sequnce is needed by on-target scoring metrics to fully evaluate the target's efficiency:

```shell
java -Xmx4g -jar FlashFry-assembly-1.14.jar \
 discover \
 --database chr22_cas9ngg_database \
 --fasta EMX1_GAGTCCGAGCAGAAGAAGAAGGG.fasta \
 --output EMX1.output
```

Finally we score the discovered sites (a few seconds):

```shell
java -Xmx4g -jar FlashFry-assembly-1.14.jar \
 score \
 --input EMX1.output \
 --output EMX1.output.scored \
 --scoringMetrics doench2014ontarget,doench2016cfd,dangerous,hsu2013,minot \
 --database chr22_cas9ngg_database
```

There should now be a set of scored sites in the `EMX1.output.scored`. Success! Now check out the documentation and tutorials for more specific details.

# Cite

FlashFry is published in [BMC Biology](https://bmcbiol.biomedcentral.com/articles/10.1186/s12915-018-0545-0); if you find it useful please cite: 

```
TY  - JOUR
AU  - McKenna, Aaron
AU  - Shendure, Jay
PY  - 2018
DA  - 2018/07/05
TI  - FlashFry: a fast and flexible tool for large-scale CRISPR target design
JO  - BMC Biology
SP  - 74
VL  - 16
IS  - 1
AB  - Genome-wide knockout studies, noncoding deletion scans, and other large-scale studies require a simple and lightweight framework that can quickly discover and score thousands of candidate CRISPR guides targeting an arbitrary DNA sequence. While several CRISPR web applications exist, there is a need for a high-throughput tool to rapidly discover and process hundreds of thousands of CRISPR targets.
SN  - 1741-7007
UR  - https://doi.org/10.1186/s12915-018-0545-0
DO  - 10.1186/s12915-018-0545-0
ID  - McKenna2018
ER  -
```
