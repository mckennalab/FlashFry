cwlVersion: v1.0
class: CommandLineTool
label: Score each target for off-targets in the genome

requirements:
- class: InlineJavascriptRequirement
hints:
  - class: DockerRequirement
    dockerPull: aaronmck/flashfry

stdout: $(inputs.std_out)
arguments:
 - { valueFrom: "echo foo 1>&2", shellQuote: False }
stderr: $(inputs.std_err)

baseCommand: /usr/bin/time
arguments:
- valueFrom: "-v"
  position: 1
- valueFrom: "java"
  position: 2
- valueFrom: "-Xmx8g"
  position: 3
- valueFrom: "-jar"
  position: 4
- valueFrom: "/FlashFry/flashfry.jar"
  position: 5
- valueFrom: "--analysis"
  position: 6
- valueFrom: "discover"
  position: 7
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
      
  std_out:
    type: string

  std_err:
    type: string

  mismatches:
    type: int
    inputBinding:
      prefix: '--maxMismatch'
      position: 12

outputs:
  outputscores:
    type: File
    outputBinding:
      glob: $(inputs.output_scores)
  output:
    type: stdout
  outputerror:
    type: stderr
