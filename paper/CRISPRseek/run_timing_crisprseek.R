args = commandArgs(trailingOnly=TRUE)

library(CRISPRseek)
library("BSgenome.Hsapiens.UCSC.hg38")
library(TxDb.Hsapiens.UCSC.hg38.knownGene)
library(org.Hs.eg.db)
outputDir <- getwd() # we dont really care about the output, just the time it takes

inputFilePath <- system.file("extdata", args[1], package = "CRISPRseek")
REpatternFile <- system.file("extdata", "NEBenzymes.fa", package = "CRISPRseek")

results <- offTargetAnalysis(args[1], findgRNAsWithREcutOnly = FALSE,
                             REpatternFile = REpatternFile,findPairedgRNAOnly = FALSE,
                             BSgenomeName = Hsapiens, txdb = TxDb.Hsapiens.UCSC.hg38.knownGene,
                             orgAnn = org.Hs.egSYMBOL, max.mismatch = as.numeric(args[2]),
                             outputDir = outputDir,overwrite = TRUE)