[![Build Status](https://travis-ci.com/mckennalab/FlashFry.svg?branch=master)](https://travis-ci.com/aaronmck/FlashFry)
[![codecov](https://codecov.io/gh/mckennalab/FlashFry/branch/master/graph/badge.svg)](https://codecov.io/gh/aaronmck/FlashFry)
[![Join the chat at https://gitter.im/FlashFry/Lobby](https://badges.gitter.im/FlashFry/Lobby.svg)](https://gitter.im/FlashFry/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.1291646.svg)](https://doi.org/10.5281/zenodo.1291646)

*Note: If you used FlashFry before version 1.9, the command-line interface has changed slightly.*
<p align="center">
<img src="https://raw.githubusercontent.com/aaronmck/FlashFry/master/images/fries.png" width="500">
</p>

FlashFry is a fast and flexible command-line tool for characterizing large numbers of potential CRISPR target sequences. FlashFry can be used with any genome, and can run against non-traditional model organisms or transcriptomes. If you're looking to characterize a smaller region or would like a nice web interface we recommend the [GT-scan](http://gt-scan.csiro.au) or [crispor.org](http://crispor.org) websites.

The easiest way to get started is to try the quick-start procedure and confirm that everything works on your system. The wiki contains more detailed tutorials for specific workflows. Thanks to @drivenbyentropy for the Java implementation of the ViennaRNA energy calculations.

## Documentation

- [Quick start](https://github.com/aaronmck/FlashFry#quickstart)
- [General options documentation](https://github.com/aaronmck/FlashFry/wiki/Command-line-options)
- [Scoring methods](https://github.com/aaronmck/FlashFry/wiki/Site-scoring)
- [FAQ](https://github.com/aaronmck/FlashFry/wiki/Frequently-asked-questions)
- [Database format documentation](https://github.com/aaronmck/FlashFry/wiki/binary-format)
- [Developer guide: nuclease profiles and database compatibility](docs/nuclease-profiles.md)
- [Tutorial: profile the MYC coding locus with `spcas9-ngg-20`](docs/tutorial-spcas9-myc.md)
- Tutorials on the [wiki](https://github.com/aaronmck/FlashFry/wiki):
  - [Call target sites in a FASTA and annotate with BED intervals](https://github.com/aaronmck/FlashFry/wiki/End-to-end-scoring-and-annotation-with-FlashFry)
  - [Create random target sequences and score them against the genome](https://github.com/aaronmck/FlashFry/wiki/Scoring-random-sequences-against-the-genome)
- [Citing FlashFry](https://github.com/aaronmck/FlashFry#cite)

## Quick start

First, make sure you are running Java 8 by typing `java -version`. From a Unix or macOS command line, download the latest FlashFry JAR:

```shell
wget https://github.com/mckennalab/FlashFry/releases/download/1.15/FlashFry-assembly-1.15.jar
```
Download and extract the sample data for human chromosome 22:

```shell
wget https://raw.githubusercontent.com/aaronmck/FlashFry/master/test_data/quickstart_data.tar.gz
tar xf quickstart_data.tar.gz
```

Build the database. This should take a few minutes (approximately 75 seconds on the original benchmark laptop):

```shell
mkdir tmp
java -Xmx4g -jar FlashFry-assembly-1.15.jar \
 index \
 --tmpLocation ./tmp \
 --database chr22_cas9ngg_database \
 --reference chr22.fa.gz \
 --enzyme spcas9ngg
```

Next, discover candidate targets and their potential off-targets. This example uses the EMX1 target with flanking sequence, which is required by on-target scoring models that evaluate sequence context:

```shell
java -Xmx4g -jar FlashFry-assembly-1.15.jar \
 discover \
 --database chr22_cas9ngg_database \
 --fasta EMX1_GAGTCCGAGCAGAAGAAGAAGGG.fasta \
 --output EMX1.output
```

Finally, score the discovered sites:

```shell
java -Xmx4g -jar FlashFry-assembly-1.15.jar \
 score \
 --input EMX1.output \
 --output EMX1.output.scored \
 --scoringMetrics doench2014ontarget,doench2016cfd,dangerous,hsu2013,minot \
 --database chr22_cas9ngg_database
```

The scored sites are written to `EMX1.output.scored`.

## Nuclease profiles

Target geometry is defined by a nuclease profile: spacer length, PAM side, and one or more fixed-width IUPAC PAM patterns. FlashFry derives discovery regular expressions, spacer and PAM ranges, mismatch masks, and bin positions from that definition. The built-in profile IDs are:

- `cpf1-tttn-20`, `spcas9-ngg-nag-20`, `spcas9-ngg-20`, `spcas9-nag-20`, `spcas9-ngg-nag-19`, and `spcas9-ngg-19` (the original aliases and database IDs remain supported)
- `spg-ngn-20`
- `sacas9-nngrrt-21` and `sacas9-nngrrt-22`
- `ascas12a-tttv-23` and `lbcas12a-tttv-23`
- `encas12a-tttn-23` and `cas12a-2xnls-tttn-23`

New databases use a version-2 header that stores the complete profile definition and target codec. Version-1 databases remain readable. Targets through 24 bases, including SpG, retain the original packed representation; longer targets, including the SaCas9 and 23-base Cas12a profiles, use the wide codec with an independent occurrence count. SpG is kept distinct from SpCas9, so SpCas9-calibrated scores are not applied automatically. The `encas12a` and `cas12a2xnls` scoring metrics use bundled 23-position CFD matrices and do not apply to AsCas12a or LbCas12a. See the [nuclease profile developer guide](docs/nuclease-profiles.md) for extension and compatibility details.

### Custom fixed-width profiles

Custom fixed-width profiles can now be used for enzymes not covered by existing, pre-programmed sets. To use these you'll need to make a profile for your enzyme; the details of which can be found in the Nuclease profiles
document in the docs folder. Custom profiels can be checked with `profile validate --profileFile PROFILE.properties` and used to build a
dedicated database with `index --profileFile PROFILE.properties`. The validated definition is embedded in the database
header, so discovery and scoring do not require the profile file again.

## Cite

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
