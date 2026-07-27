# Nuclease profiles and database compatibility

This guide describes the invariants that connect target discovery, binary storage, off-target traversal, and scoring. Read it before adding a nuclease or changing an existing targeting rule.

## Core model

`NucleaseProfile` is the unit of targeting compatibility. An enzyme name alone is insufficient because one nuclease may support multiple spacer lengths, PAM sets, or experimentally calibrated scoring models.

A profile contains:

| Field | Purpose |
| --- | --- |
| `id` | Stable machine-readable identifier stored in version-2 database headers |
| `aliases` | Case- and punctuation-insensitive CLI names |
| `nuclease` | Biochemical identity and broad family |
| `spacerLength` | Number of guide-complementary bases |
| `pamSpec` | PAM side and equal-width IUPAC alternatives |
| `legacyHeaderId` | Numeric version-1 ID for one of the six historical profiles |

`TargetLayout` derives the total length, spacer and PAM ranges, comparison mask, and spacer-relative bin positions. These values should not be duplicated in consumers.

## Built-in modern profiles

| Profile ID | Spacer | PAM | PAM side | CLI aliases |
| --- | ---: | --- | --- | --- |
| `spg-ngn-20` | 20 | `NGN` | 3-prime | `spg` |
| `sacas9-nngrrt-21` | 21 | `NNGRRT` | 3-prime | `sacas9`, `sacas921` |
| `sacas9-nngrrt-22` | 22 | `NNGRRT` | 3-prime | `sacas922` |
| `ascas12a-tttv-23` | 23 | `TTTV` | 5-prime | `ascas12a`, `ascpf1` |
| `lbcas12a-tttv-23` | 23 | `TTTV` | 5-prime | `lbcas12a`, `lbcpf1` |
| `encas12a-tttn-23` | 23 | `TTTN` | 5-prime | `encas12a` |
| `cas12a-2xnls-tttn-23` | 23 | `TTTN` | 5-prime | `cas12a2xnls`, `2xnlscas12a` |

The SpG profile uses the full three-base `NGN` PAM context and remains distinct from SpCas9 so that targeting identity
and score applicability are not conflated. The unqualified `sacas9` alias selects the established 21-base SaCas9
profile. Use the stable profile ID or the length-specific alias when spacer length must be explicit. AsCas12a and
LbCas12a intentionally have separate profiles: shared target geometry does not make ortholog-specific activity or
off-target scores interchangeable.

## Normalized target orientation

Every discovered site is stored in guide orientation. Reverse-strand genomic sites are reverse-complemented before storage, so downstream code sees only two layouts:

```text
5-prime PAM: [PAM][spacer]
3-prime PAM: [spacer][PAM]
```

Use `layout.spacer(sequence)`, `layout.pamSequence(sequence)`, and `layout.binSequence(sequence, width, offset)`. Do not hard-code slices based on a specific Cas9 or Cas12a layout.

PAM alternatives must have equal width. FlashFry accepts the DNA IUPAC symbols `A`, `C`, `G`, `T`, `R`, `Y`, `K`, `M`, `S`, `W`, `B`, `D`, `H`, `V`, and `N`.

## Using a custom profile file

The `index` command can build a database from a Java properties file instead of a built-in `--enzyme`. For example:

```properties
id=mycas-ngn-20
aliases=mycas
nuclease=MyCas
family=cas9
spacerLength=20
pamSide=3prime
pams=NGN
```

The required properties are `id`, `nuclease`, `family`, `spacerLength`, `pamSide`, and `pams`. `aliases` is optional.
Use `5prime` or `3prime` for `pamSide`; separate multiple equal-width PAM patterns with commas.

Validate the file before indexing:

```bash
java -jar FlashFry.jar profile validate \
  --profileFile mycas.properties
```

Validation rejects missing or unknown properties, built-in name collisions, unsupported IUPAC symbols, unequal PAM
widths, and profiles whose spacer plus PAM exceeds 32 bases. It prints the normalized profile, target layout, selected
codec, and compatibility fingerprint.

Build a dedicated genome database with the validated profile:

```bash
java -jar FlashFry.jar index \
  --reference genome.fa \
  --tmpLocation tmp \
  --database mycas.db \
  --profileFile mycas.properties
```

`--profileFile` and `--enzyme` are mutually exclusive. The profile definition is embedded in the version-2 database
header, so subsequent `discover`, `score`, and `extract` commands need only the database path. Each database has one
fixed target geometry; use separate databases for different spacer lengths or PAM widths. Profile-specific scoring
models are accepted only when explicitly calibrated for the custom profile.

## Adding a built-in profile

1. Define or reuse an `Enzyme` and `EnzymeType` in `standards/Enzyme.scala`.
2. Add one immutable `NucleaseProfile` to `NucleaseProfiles`.
3. Add the profile to `NucleaseRegistry.builtIns` so its ID and aliases work at the CLI.
4. Leave `legacyHeaderId` empty. Values 1 through 6 belong permanently to the version-1 format.
5. Add tests for forward and reverse discovery, spacer/PAM extraction, mismatch masking, and database round-tripping.

Example:

```scala
val exampleCas12b = NucleaseProfile(
  id = "example-cas12b-ttn-20",
  aliases = Set("examplecas12b"),
  nuclease = ConfiguredEnzyme("ExampleCas12b", ConfiguredEnzymeType("cas12b")),
  spacerLength = 20,
  pamSpec = PamSpec(FivePrimePam, Vector("TTN")),
  legacyHeaderId = None
)
```

An unregistered profile can round-trip through a version-2 header when constructed programmatically or loaded with
`--profileFile`. Registration is required only for selection by `--enzyme`.

## Target codecs

Targets use two bits per DNA base and remain comparable as a single 64-bit value.

| Codec | Target width | Record layout |
| --- | --- | --- |
| `packed24-v1` | Up to 24 bases | `[48-bit sequence + 16-bit count][positions...]` |
| `wide32-v2` | 25–32 bases | `[64-bit sequence][count][positions...]` |

Codecs are selected from the profile's total target length. Persisted blocks must use `BitEncoding.encodeTargetRecord` and `decodeTargetRecord`; reading a count directly from a sequence value is valid only for the legacy codec.

## Header compatibility

FlashFry writes version-2 headers and reads versions 1 and 2.

- Version 1 stores one of six numeric profile IDs and always uses `packed24-v1` records.
- Version 2 stores the serialized profile, a targeting-definition fingerprint, and the codec ID.
- Registered profile IDs must match their serialized nuclease, family, spacer length, PAM side, and PAM patterns.
- Aliases and legacy IDs are excluded from the fingerprint because they do not change targeting behavior.

Changing an existing profile's targeting definition is a compatibility break. Add a new profile ID instead.

## Scoring applicability

Scoring models declare `ScoreApplicability` constraints. Empty dimensions are unrestricted; non-empty dimensions are combined with logical AND.

Use exact profile IDs for models calibrated on a particular spacer length, PAM set, or nuclease variant. Family-wide applicability should be used only when the scoring method is genuinely valid for the whole family. A new profile must not silently inherit a Cas9 or Cas12a model merely because its target length matches.

## Review checklist

- Discovery finds adjacent, overlapping, forward, and reverse targets.
- All filtering and folding operations use the spacer rather than the PAM.
- Bins are extracted from spacer coordinates for both PAM sides.
- Mismatch comparison covers every spacer base and excludes every PAM base.
- Linear and indexed traversal return identical results.
- Version-1 headers and all six legacy aliases remain readable.
- A version-2 database can be written, read, and scanned end to end.
- Scoring models accept only profiles for which they are calibrated.
