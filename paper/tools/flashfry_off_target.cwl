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

baseCommand: taskset
arguments:
- valueFrom: "-c"
  position: 1
- valueFrom: "0"
  position: 2
- valueFrom: "/usr/bin/time"
  position: 3
- valueFrom: "-v"
  position: 4
- valueFrom: "java"
  position: 5
- valueFrom: "-Xmx15g"
  position: 6
- valueFrom: "-jar"
  position: 7
- valueFrom: "/FlashFry/flashfry.jar"
  position: 8
- valueFrom: "--analysis"
  position: 9
- valueFrom: "discover"
  position: 10
- valueFrom: "--database"
  position: 11
- valueFrom: "/genome/hg38"
  position: 12
  
inputs:
  fasta:
    type: File
    inputBinding:
      prefix: '--fasta'
      position: 13
  
  output_scores:
    type: string
    inputBinding:
      prefix: '--output'
      position: 14
      
  std_out:
    type: string

  std_err:
    type: string

  mismatches:
    type: int
    inputBinding:
      prefix: '--maxMismatch'
      position: 15

outputs:
  outputscores:
    type: File
    outputBinding:
      glob: $(inputs.output_scores)
  output:
    type: stdout
  outputerror:
    type: stderr
