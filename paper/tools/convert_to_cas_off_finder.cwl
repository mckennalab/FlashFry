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

outputs:
  output:
    type: File
    outputBinding:
      glob: $(inputs.casFile)
