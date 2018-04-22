cwlVersion: v1.0
class: CommandLineTool
label: Convert a fasta list of targets into the Cas-Off file format

requirements:
- class: InlineJavascriptRequirement
hints:
  - class: DockerRequirement
    dockerPull: aaronmck/flashfry

baseCommand: python
arguments:
- valueFrom: "/FlashFry_GIT/paper/tools/convert_to_cas_off_finder.py"
  position: 1
  
inputs:
  fasta:
    type: File
    inputBinding:
      position: 2
  
  casFile:
    type: string
    inputBinding:
      position: 3

  fastq:
    type: string
    inputBinding:
      position: 4

  mismatches:
    type: int
    inputBinding:
      position: 5

outputs:
  casFileOut:
    type: File
    outputBinding:
      glob: $(inputs.casFile)
  fastqOut:
    type: File
    outputBinding:
      glob: $(inputs.fastq)
