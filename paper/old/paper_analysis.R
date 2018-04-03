
library(data.table)
library(ggplot2)
library(tidyr)
library(devtools)
library(DSR)

setwd("~/google_drive/UW/shendure/flash_fry/2017_11_21_CGC_list")

cgc_tbl <- fread("data_with_exons.txt")
colnames(cgc_tbl) = make.names(colnames(cgc_tbl))

dim(cgc_tbl)

filtered_tbl = cgc_tbl[cgc_tbl$dangerous_GC == "NONE" & cgc_tbl$dangerous_polyT == "NONE",]
filtered_tbl = filtered_tbl[order(filtered_tbl$AggregateRankedScore_medianRank),]

dim(filtered_tbl)
nrow(cgc_tbl) - nrow(filtered_tbl)
nrow(filtered_tbl)/nrow(cgc_tbl)


two_best_guides = function(x,columnToReturn) {
  if (length(x) > 1) {
    # print(x)
    tttt = x[order(as.numeric(x$AggregateRankedScore_medianRank, .SD),decreasing=F,na.last =T),]
    return(tttt[1:2,][[columnToReturn]])
  } else {
    return(c(0,0))
  }
}

# at the gene level ########################################################################################################################

# Hsu et al. by gene
top_hits_gene_Hsu = by(filtered_tbl, list(filtered_tbl$gene), function(x) {two_best_guides(x,"Hsu2013")[1]},simplify=T )
second_hits_gene_Hsu = by(filtered_tbl, list(filtered_tbl$gene), function(x) {two_best_guides(x,"Hsu2013")[2]},simplify=T )

# Moreno.Mateos2015OnTarget by gene
top_hits_gene_MM = by(filtered_tbl, list(filtered_tbl$gene), function(x) {two_best_guides(x,"Moreno.Mateos2015OnTarget")[1]},simplify=T )
second_hits_gene_MM = by(filtered_tbl, list(filtered_tbl$gene), function(x) {two_best_guides(x,"Moreno.Mateos2015OnTarget")[2]},simplify=T )

# summary at the gene level
length(second_hits_gene_Hsu[second_hits_gene_Hsu > 75])/length(second_hits_gene_Hsu)
length(second_hits_gene_Hsu[second_hits_gene_MM > 50])/length(second_hits_gene_MM)

# at the exon level ########################################################################################################################
# get the number before filtering any results out to be fair
total_exons = length(unique(cgc_tbl$gene_exon))


# Hsu et al. by gene
top_hits_ge_Hsu = by(filtered_tbl, list(filtered_tbl$gene_exon), function(x) {two_best_guides(x,"Hsu2013")[1]},simplify=T )
second_hits_ge_Hsu = by(filtered_tbl, list(filtered_tbl$gene_exon), function(x) {two_best_guides(x,"Hsu2013")[2]},simplify=T )

# Moreno.Mateos2015OnTarget by gene
top_hits_ge_MM = by(filtered_tbl, list(filtered_tbl$gene_exon), function(x) {two_best_guides(x,"Moreno.Mateos2015OnTarget")[1]},simplify=T )
second_hits_ge_MM = by(filtered_tbl, list(filtered_tbl$gene_exon), function(x) {two_best_guides(x,"Moreno.Mateos2015OnTarget")[2]},simplify=T )

# summary at the gene level
length(second_hits_ge_Hsu[second_hits_ge_Hsu > 75])/total_exons
length(second_hits_ge_Hsu[second_hits_ge_MM > 50])/total_exons