cwlVersion: v1.0
class: CommandLineTool
label: Score for off-targets using CRISPRseek

requirements:
- class: InlineJavascriptRequirement
hints:
  - class: DockerRequirement
    dockerPull: aaronmck/flashfry

baseCommand: bash
arguments:
- valueFrom: "/usr/bin/memusg.sh"
  position: 1
- valueFrom: "Rscript"
  position: 2
- valueFrom: "/FlashFry_GIT/paper/CRISPRseek/run_timing_crisprseek.R"
  position: 3


stdout: $(inputs.outputFilename)

inputs:
  fasta:
    type: File
    inputBinding:
      position: 4

  mismatches:
    type: int
    inputBinding:
      position: 5

  outputFilename: string


outputs:
  output:
    type: File
    outputBinding:
      glob: $(inputs.outputFilename)
