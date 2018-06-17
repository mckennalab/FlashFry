<p align="center">
<img src="https://raw.githubusercontent.com/aaronmck/FlashFry/master/images/fries.png" width="500">
</p>

FlashFry is a fast and flexible command-line tool for characterizing large numbers of potential CRISPR target sequences. If you're looking to characterize a smaller region or would like a nice web interface we recommend the [crispor.org](http://crispor.org) website.

The easiest way to get started it to try out the quick-start procedure to make sure everything works on your system. If everything looks good, there are few more in-depth tutorials to try out various capacities of FlashFry.

#### links:
- [Quick start](#Quickstart)
- [General options documentation](https://github.com/aaronmck/FlashFry/wiki/Command-line-options)
- [Scoring methods](https://github.com/aaronmck/FlashFry/wiki/Site-scoring)
- [FAQ](https://github.com/aaronmck/FlashFry/wiki/Frequently-asked-questions)
- [Database format documentation](https://github.com/aaronmck/FlashFry/wiki/binary-format)
- Tutorials
  * Tutorial - [call sites and annotate with BED file intervals](https://github.com/aaronmck/FlashFry/wiki/End-to-end-scoring-and-annotation-with-FlashFry)
 


# Quickstart

From the UNIX or Mac command line, download the latest release version of the FlashFry jar file:

```shell
wget https://github.com/aaronmck/FlashFry/releases/download/1.8.1/FlashFry-assembly-1.8.1.jar
```
download and then un-gzip the sample data for human chromosome 22:

```shell
wget https://raw.githubusercontent.com/aaronmck/FlashFry/master/test_data/quickstart_data.tar.gz
tar xf quickstart_data.tar.gz
```

then run the database creation step (this should take a few minutes, it takes ~75 seconds on my laptop):

```shell
mkdir tmp
java -Xmx4g -jar FlashFry-assembly-1.8.1.jar \
 --analysis index \
 --tmpLocation ./tmp \
 --database chr22_cas9ngg_database \
 --reference chr22.fa.gz \
 --enzyme spcas9ngg
```

Now we discover candidate targets and their potential off-target in the test data (takes a few seconds). Here we're using the EMX1 target with some random sequence flanking the target site:

```shell
java -Xmx4g -jar FlashFry-assembly-1.8.1.jar \
 --analysis discover \
 --database chr22_cas9ngg_database \
 --fasta EMX1_GAGTCCGAGCAGAAGAAGAAGGG.fasta \
 --output EMX1.output
```

finally we score the discovered sites (a few seconds):

```shell
java -Xmx4g -jar FlashFry-assembly-1.8.1.jar \
 --analysis score \
 --input EMX1.output \
 --output EMX1.output.scored \
 --scoringMetrics doench2014ontarget,doench2016cfd,dangerous,hsu2013,minot \
 --database chr22_cas9ngg_database
```

There should now be a set of scored sites in the `EMX1.output.scored`. Success! Now check out the documenation and and tutorials for more quesiton specific details.
