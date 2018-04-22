cwlVersion: v1.0
class: CommandLineTool
label: Use flashfry to make some random guides

requirements:
- class: InlineJavascriptRequirement
hints:
  - class: DockerRequirement
    dockerPull: aaronmck/flashfry

baseCommand: java
arguments:
- valueFrom: "-Xmx2g"
  position: 3
- valueFrom: "-jar"
  position: 4
- valueFrom: "/FlashFry/flashfry.jar"
  position: 5
- valueFrom: "--analysis"
  position: 6
- valueFrom: "random"
  position: 7
- valueFrom: "--enzyme"
  position: 8
- valueFrom: "spcas9ngg"
  position: 9
- valueFrom: "--onlyUnidirectional"
  position: 10
  
  
inputs:
  random_count:
    type: int
    inputBinding:
      prefix: '--randomCount'
      position: 11
  
  output_fasta:
    type: string
    inputBinding:
      prefix: '--outputFile'
      position: 12

outputs:
  output:
    type: File
    outputBinding:
      glob: $(inputs.output_fasta)
