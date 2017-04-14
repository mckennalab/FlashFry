[![Build Status](https://travis-ci.org/aaronmck/DeepFry.svg?branch=master)](https://travis-ci.org/aaronmck/DeepFry)
[![codecov](https://codecov.io/gh/aaronmck/DeepFry/branch/master/graph/badge.svg)](https://codecov.io/gh/aaronmck/DeepFry)
FlashFry
=======

The CRISPR site finder.  Why reinvent the wheel on this one?  Well there are a couple of nice CRISPR design tools out there, but 
many (though not all) exist as webtools.  So if you're screening a lot of CRISPR targets, you're stuck either 1) feeding them manually
into the website or 2) feeding / screen-scraping the results in some ugly python script.  Boo.  

Nothing fancy, but it does allow you to rapidly screen targets from any genome.  To achive higher speeds, the genome is preprocessed into a binary format where off-targets and their positions are encoded into long (64 bit) integers. This makes reading quick, and comparison between the putitive guide the off-target amazingly fast. The format is lightly documented below, though the tools can index your genome, so the documentation is more as a sanity check for me.

Binary file format
------------------

The tools uses a custom binary format that compresses the genome hits for a target sequences into binary values. Values are stored as default big endian in java, so we'll keep it that way here. Here's what the format looks like:


header:
- 64 bit magic value (long)
- 64 bit version number (long)
- 64 bit number of bins (long)

bin lookup table:
- for the number of bins in the header:
  - 64 bit size of the this block in the file (in bytes, long)
  - 64 bit offset of this block in the file (in bytes, long)

within each block the following pattern repeats:
- 1 64 bit long for the target sequence and count information (long)
- X 64 bit long for the positions within the target genome (long)

