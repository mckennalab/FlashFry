DeepFry
=======

The CRISPR site finder.  Why reinvent the wheel on this one?  Well there are a couple of nice CRISPR design tools out there, but 
many (if not all) exist as webtools.  So if you're screening a lot of CRISPR targets, you're stuck either 1) feeding them manually
into the website or 2) feeding / screen-scraping the results in some ugly python script.  Boo.  So I've combined the off-target scoring approach 
from http://crispr.mit.edu with the on-target scoring scheme from http://www.broadinstitute.org/rnai/public/analysis-tools/sgrna-design.

Nothing fancy, but it does allow you to rapidly screen targets from any arbritrary genome.  To achive high speeds, the whole
list of sorted off-target hits is loaded and only sites that have <=5 mismatches are scored per bin.


