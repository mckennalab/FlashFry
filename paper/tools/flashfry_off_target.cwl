cwlVersion: v1.0
class: CommandLineTool
label: Score each target for off-targets in the genome

requirements:
- class: InlineJavascriptRequirement
hints:
  - class: DockerRequirement
    dockerPull: aaronmck/flashfry
    
baseCommand: java
arguments:
- valueFrom: "-Xmx2g"
  position: 1
- valueFrom: "-jar"
  position: 2
- valueFrom: "/FlashFry/flashfry.jar"
  position: 3
- valueFrom: "--analysis"
  position: 4
- valueFrom: "discover"
  position: 5
- valueFrom: "--database"
  position: 8
- valueFrom: "/genome/hg38"
  position: 9
  
inputs:
  fasta:
    type: File
    inputBinding:
      prefix: '--fasta'
      position: 10
  
  output_scores:
    type: string
    inputBinding:
      prefix: '--output'
      position: 11

outputs:
  output:
    type: File
    outputBinding:
      glob: $(inputs.output_scores)
