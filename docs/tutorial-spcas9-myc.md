# Profiling a coding locus with SpCas9-NGG: a MYC tutorial

This tutorial builds a human whole-genome FlashFry database for the `spcas9-ngg-20` profile, extracts the coding
exons of **MYC**, discovers candidate guides, evaluates their genomic off-targets, and scores the resulting guide set.
The same database can be reused for any other locus on the same genome assembly.

The workflow is deliberately explicit about genome and annotation versions. The example uses the GENCODE release 50
GRCh38 primary assembly and its matching annotation. Do not mix a FASTA and GTF from different assemblies or releases.

## What the profile means

The stable profile ID `spcas9-ngg-20` describes a 20-base SpCas9 spacer followed by a 3-prime `NGG` PAM:

```text
[20-base spacer][NGG PAM]
```

FlashFry reports the complete 23-base target in guide orientation, even for a reverse-strand genomic site. The first 20
bases of the `target` output column are therefore always the sequence used to make the guide; the final three bases are
the PAM.

Use this profile when the experiment truly requires NGG targets. `spcas9-ngg-nag-20` is a different profile and builds
a different database because it admits both NGG and NAG sites.

## Requirements

The commands below assume Bash and the following software:

- Java 8
- FlashFry built from this repository
- `curl`, `gzip`, `awk`, and `sort`
- [SAMtools](https://www.htslib.org/) for FASTA indexing
- [BEDTools](https://bedtools.readthedocs.io/) for genomic sequence extraction
- Python 3 for the optional shortlisting example

Whole-genome indexing is a one-time, compute- and storage-intensive step. Use a local disk with ample free space for
the database and temporary files. Runtime and database size depend strongly on the reference and machine.

## 1. Build FlashFry and create a workspace

From the FlashFry repository root:

```bash
REPO_ROOT="$(pwd)"

sbt assembly

export FLASHFRY_JAR="$REPO_ROOT/target/scala-2.12/FlashFry-assembly-1.15.jar"
test -s "$FLASHFRY_JAR"

export WORK_DIR="$REPO_ROOT/tutorial-work/spcas9-myc"
mkdir -p "$WORK_DIR"/{reference,database/tmp,locus,results}
cd "$WORK_DIR"
```

If using a prebuilt JAR, skip `sbt assembly` and set `FLASHFRY_JAR` to its absolute path. The JAR must recognize the
stable `spcas9-ngg-20` profile ID; an older release may recognize only the legacy `spcas9ngg` alias.

## 2. Download a matched genome and annotation

[GENCODE release 50](https://www.gencodegenes.org/human/) supplies both the GRCh38 primary-assembly genome and a GTF
whose sequence names match that FASTA. Pinning the release makes the tutorial reproducible.

```bash
export GENCODE_RELEASE=50
export GENCODE_BASE="https://ftp.ebi.ac.uk/pub/databases/gencode/Gencode_human/release_${GENCODE_RELEASE}"

export GENOME_GZ="$WORK_DIR/reference/GRCh38.primary_assembly.genome.fa.gz"
export GENOME_FA="$WORK_DIR/reference/GRCh38.primary_assembly.genome.fa"
export GTF_GZ="$WORK_DIR/reference/gencode.v${GENCODE_RELEASE}.primary_assembly.annotation.gtf.gz"

curl --fail --location --retry 3 \
  "$GENCODE_BASE/GRCh38.primary_assembly.genome.fa.gz" \
  --output "$GENOME_GZ"

curl --fail --location --retry 3 \
  "$GENCODE_BASE/gencode.v${GENCODE_RELEASE}.primary_assembly.annotation.gtf.gz" \
  --output "$GTF_GZ"

gzip -t "$GENOME_GZ"
gzip -t "$GTF_GZ"

gzip -dc "$GENOME_GZ" > "$GENOME_FA"
samtools faidx "$GENOME_FA"

head -n 2 "$GENOME_FA"
cut -f1,2 "$GENOME_FA.fai" | head
```

The primary assembly contains the assembled chromosomes and primary scaffolds, but excludes alternate loci, patches,
and haplotypes. If those sequences matter to the experiment, use GENCODE's `ALL` FASTA and matching `ALL` annotation
for both indexing and locus extraction.

## 3. Build the whole-genome SpCas9-NGG database

The database path is a prefix shared by the binary database and its required `.header` companion.

```bash
export DATABASE="$WORK_DIR/database/GRCh38.spCas9-NGG-20"

java -Xmx4g -jar "$FLASHFRY_JAR" \
  index \
  --reference "$GENOME_FA" \
  --tmpLocation "$WORK_DIR/database/tmp" \
  --database "$DATABASE" \
  --enzyme spcas9-ngg-20 \
  --binSize 7

test -s "$DATABASE"
test -s "$DATABASE.header"

grep -E '^(profile|targetCodec)=' "$DATABASE.header"
```

The header check should identify `spcas9-ngg-20` and `packed24-v1`. This profile contains 23 total bases, so its target
sequence and occurrence count retain FlashFry's compact legacy representation. Mismatch comparisons still operate on a
single 64-bit value.

Keep the database and `.header` together. Once indexing succeeds, the files under `database/tmp` are no longer needed.

## 4. Select the MYC coding exons

A gene can have many transcripts. This example uses the
**[MANE Select](https://www.ncbi.nlm.nih.gov/refseq/MANE/)** transcript, a representative transcript jointly selected
by Ensembl/GENCODE and NCBI. For a production experiment, decide whether one representative transcript is enough or
whether every coding isoform should be targeted.

Do not download a concatenated transcript CDS and scan it as one sequence: doing so creates artificial 23-base windows
across exon-exon junctions that do not exist in genomic DNA. Instead, extract each genomic CDS interval as a separate
FASTA record.

The following command converts the one-based, closed GTF coordinates to zero-based, half-open BED coordinates and adds
six genomic bases on each side. Those bases provide sequence context for the Doench 2014 on-target model.

```bash
export GENE=MYC
export CONTEXT_BASES=6
export CDS_BED="$WORK_DIR/locus/${GENE}.MANE-Select.CDS.with-context.bed"

gzip -dc "$GTF_GZ" |
  awk -F '\t' -v OFS='\t' -v gene="$GENE" -v flank="$CONTEXT_BASES" '
    $3 == "CDS" &&
    $9 ~ ("gene_name \"" gene "\";") &&
    $9 ~ /tag "MANE_Select";/ {
      start = $4 - 1 - flank
      if (start < 0) start = 0
      end = $5 + flank
      print $1, start, end, gene "_MANE_CDS_" ++n, 0, $7
    }
  ' |
  sort -k1,1 -k2,2n > "$CDS_BED"

test -s "$CDS_BED"
cat "$CDS_BED"
```

With GENCODE v50, the expected BED records are:

```text
chr8    127736587    127736629    MYC_MANE_CDS_1    0    +
chr8    127738241    127739025    MYC_MANE_CDS_2    0    +
chr8    127740389    127740961    MYC_MANE_CDS_3    0    +
```

The BED start is zero-based and the end is excluded. These records already include the six context bases; the original
CDS boundaries are six bases inside each interval.

## 5. Extract the MYC locus FASTA

```bash
export LOCUS_FASTA="$WORK_DIR/locus/${GENE}.MANE-Select.CDS.with-context.fa"

bedtools getfasta \
  -fi "$GENOME_FA" \
  -bed "$CDS_BED" \
  -name \
  -fo "$LOCUS_FASTA"

grep -c '^>' "$LOCUS_FASTA"
head -n 4 "$LOCUS_FASTA"
```

The record count should be three for the v50 MYC MANE Select model. BEDTools leaves each CDS interval separate, which
prevents splice-junction artifacts. FlashFry searches both genomic strands, so strand-specific FASTA extraction is not
required.

The six-base interval padding means discovered targets at least substantially overlap a CDS boundary rather than being
drawn from an unrelated distant intronic region. A small number of targets at the outer FASTA boundaries may still lack
enough context for an on-target model; FlashFry reports `NA` for that metric rather than inventing sequence.

## 6. Discover candidates and genomic off-targets

This step finds every 20-base spacer followed by NGG in the MYC FASTA, then searches the whole-genome database for
targets with up to four spacer mismatches.

```bash
export DISCOVERY="$WORK_DIR/results/${GENE}.spCas9-NGG-20.discovery.tsv"

java -Xmx4g -jar "$FLASHFRY_JAR" \
  discover \
  --database "$DATABASE" \
  --fasta "$LOCUS_FASTA" \
  --output "$DISCOVERY" \
  --maxMismatch 4 \
  --flankingSequence "$CONTEXT_BASES" \
  --maximumOffTargets 5000

test -s "$DISCOVERY"

awk 'NR > 1 { guides++; if ($6 == "OVERFLOW") overflow++ }
     END { print "guides=" guides, "overflow=" (overflow + 0) }' \
  "$DISCOVERY"

cut -f1-8 "$DISCOVERY" | head
```

Important discovery columns include:

- `target`: normalized `[20-base spacer][NGG]` sequence.
- `context`: the target plus captured flanking sequence.
- `orientation`: `FWD` or `RVS` relative to the extracted genomic FASTA record.
- `overflow`: `OVERFLOW` means the guide exceeded `--maximumOffTargets` and its search is not exhaustive.
- `otCount`: total genomic occurrences represented by retained matches.
- `offTargets`: encoded target sequences, occurrence counts, and mismatch counts used during scoring.

The `contig`, `start`, and `stop` columns refer to each extracted FASTA record. The BEDTools-generated FASTA record name
retains the source GRCh38 interval, while `start` and `stop` are zero-based coordinates within that record. Add
`--positionOutput` to `discover` if individual genome positions for every off-target must be retained; this makes the
discovery file substantially larger.

## 7. Score the guide set

The following suite combines commonly used SpCas9 efficiency, specificity, and sequence-quality metrics:

```bash
export SCORED="$WORK_DIR/results/${GENE}.spCas9-NGG-20.scored.tsv"

java -Xmx4g -jar "$FLASHFRY_JAR" \
  score \
  --input "$DISCOVERY" \
  --output "$SCORED" \
  --database "$DATABASE" \
  --maxMismatch 4 \
  --scoringMetrics hsu2013,doench2014ontarget,doench2016cfd,dangerous,minot \
  --numericOutput

test -s "$SCORED"

head -n 1 "$SCORED" | tr '\t' '\n' | nl -ba
awk 'END { print "scored guides=" NR - 1 }' "$SCORED"
```

The scoring command excludes overflowed guides because their off-target collections are incomplete. Consequently, the
scored row count can be lower than the discovery row count.

### How to read the metrics

| Output column | Interpretation | Preferred direction |
| --- | --- | --- |
| `Hsu2013` | MIT/Hsu aggregate off-target specificity score | Higher is better |
| `Doench2014OnTarget` | Sequence-context model of guide activity | Higher is better |
| `DoenchCFD_maxOT` | Highest CFD activity among retained non-identical genomic matches | Lower is better |
| `DoenchCFD_specificityscore` | Occurrence-weighted aggregate CFD specificity | Higher is better |
| `dangerous_GC` | Spacer GC fraction with `--numericOutput` | Usually avoid extremes |
| `dangerous_polyT` | `1` when the spacer contains `TTTT`, otherwise `0` | `0` is preferred |
| `dangerous_in_genome` | Number of exact genomic matches | `1` is usually preferred |
| `basesDiffToClosestHit` | Mismatches to the closest non-identical retained hit | Higher is better |
| `closestHitCount` | Occurrences at that closest mismatch distance | Lower is better |
| `0-1-2-3-4_mismatch` | Occurrence histogram by mismatch count | Fewer low-mismatch hits |

`DoenchCFD_maxOT` is thresholded by FlashFry at 0.023; values below that threshold are reported as zero. The
`dangerous_in_genome` count includes the intended exact genomic target, so one—not zero—is the usual uniqueness check.

The implementations correspond to the [Hsu et al. 2013](https://doi.org/10.1038/nbt.2647),
[Doench et al. 2014](https://doi.org/10.1038/nbt.3026), and
[Doench et al. 2016](https://doi.org/10.1038/nbt.3437) models. Scores are useful ranking evidence, not guarantees of
activity or safety in a particular cell type.

## 8. Produce an example shortlist

There is no universally correct way to collapse activity, specificity, cloning constraints, and experimental context
into one number. The following example applies transparent quality filters and then sorts lexicographically by aggregate
CFD specificity, maximum CFD risk, Hsu specificity, and Doench on-target score. It does **not** define a new biological
score.

```bash
export TOP_GUIDES="$WORK_DIR/results/${GENE}.spCas9-NGG-20.top20.tsv"

python3 - "$SCORED" "$TOP_GUIDES" <<'PY'
import csv
import sys

input_path, output_path = sys.argv[1:]

with open(input_path, newline="") as handle:
    rows = list(csv.DictReader(handle, delimiter="\t"))

eligible = []
for row in rows:
    try:
        gc = float(row["dangerous_GC"])
        on_target = float(row["Doench2014OnTarget"])
        hsu = float(row["Hsu2013"])
        max_cfd = float(row["DoenchCFD_maxOT"])
        specificity = float(row["DoenchCFD_specificityscore"])
    except (KeyError, ValueError):
        continue

    if not 0.25 <= gc <= 0.75:
        continue
    if row["dangerous_polyT"] != "0":
        continue
    if row["dangerous_in_genome"] != "1":
        continue

    row["spacer"] = row["target"][:20]
    eligible.append(row)

eligible.sort(key=lambda row: (
    -float(row["DoenchCFD_specificityscore"]),
    float(row["DoenchCFD_maxOT"]),
    -float(row["Hsu2013"]),
    -float(row["Doench2014OnTarget"]),
))

columns = [
    "target", "spacer", "contig", "start", "orientation",
    "Doench2014OnTarget", "Hsu2013", "DoenchCFD_maxOT",
    "DoenchCFD_specificityscore", "dangerous_GC",
    "basesDiffToClosestHit", "closestHitCount", "0-1-2-3-4_mismatch",
]

with open(output_path, "w", newline="") as handle:
    writer = csv.DictWriter(handle, fieldnames=columns, delimiter="\t", extrasaction="ignore")
    writer.writeheader()
    writer.writerows(eligible[:20])

print(f"eligible={len(eligible)} wrote={min(20, len(eligible))} to {output_path}")
PY

head -n 10 "$TOP_GUIDES"
```

The `spacer` column is the 20-base sequence normally ordered or cloned into an SpCas9 guide construct. Confirm the
vector's transcription requirements, strand conventions, and any added 5-prime base before ordering oligonucleotides.

For a real experiment, also consider which coding exon is shared across relevant isoforms, the desired cut position,
known variants in the cells or population, chromatin state, delivery method, and an orthogonal experimental off-target
assay.

## 9. Apply the workflow to another gene

The genome database does not need to be rebuilt. Change the gene symbol and repeat the BED extraction, locus FASTA,
discovery, and scoring steps:

```bash
export GENE=TP53
```

The extraction command uses `-v gene="$GENE"`, so no other change is required. Always inspect the resulting BED before
continuing. If the file is empty, check whether the symbol is current and whether that gene has a MANE Select coding
transcript in the pinned annotation.

To target every annotated coding isoform instead of MANE Select, remove the `tag "MANE_Select"` condition, deduplicate
overlapping CDS intervals, and preserve each genomic interval as its own FASTA record. Do not concatenate exons before
guide discovery.

## Troubleshooting

### `Unknown nuclease profile 'spcas9-ngg-20'`

Build the JAR from the current repository. Older releases may expose only the `spcas9ngg` compatibility alias.

### The database exists but discovery reports a missing header

The binary database and `$DATABASE.header` are a pair. Use the database prefix—not the `.header` filename—with
`--database`.

### The MYC BED file is empty

Confirm that the GTF is GENCODE v50 primary assembly and inspect MYC CDS records without the MANE filter:

```bash
gzip -dc "$GTF_GZ" |
  awk -F '\t' '$3 == "CDS" && $9 ~ /gene_name "MYC";/' |
  head
```

If records exist but lack `MANE_Select`, choose a transcript explicitly or use another documented transcript-selection
rule rather than silently combining isoforms.

### `Doench2014OnTarget` is `NA`

That target lacks the required four upstream and three downstream bases in its captured context, usually because it is
too close to an extracted FASTA boundary. Increase the BED padding, rerun discovery, and retain only targets that overlap
the original CDS interval.

### Discovery and scored guide counts differ

Scoring intentionally filters `OVERFLOW` guides. Increase `--maximumOffTargets` and rerun discovery if exhaustive
results for highly repetitive guides are required, keeping in mind the associated memory and output-size cost.

### Reference coordinates or contig names do not agree

Use the genome FASTA and GTF from the same provider, assembly, region set, and release. GTF starts are one-based and
inclusive; BED and FlashFry output starts are zero-based, with the stop coordinate excluded.

## Reproducibility checklist

Record the following with the final guide set:

- FlashFry commit or release and exact JAR filename.
- Profile ID: `spcas9-ngg-20`.
- GENCODE release: `50`.
- Assembly and region set: GRCh38 primary assembly.
- `--maxMismatch`, `--maximumOffTargets`, and sequence-context settings.
- Scoring metric list and any post-scoring filters.
- Whether alternate loci, sample-specific variants, or population variants were considered.
