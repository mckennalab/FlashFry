cwlVersion: v1.0
class: CommandLineTool
label: Score each target for off-targets in the genome

requirements:
- class: InlineJavascriptRequirement
hints:
  - class: DockerRequirement
    dockerPull: aaronmck/flashfry

baseCommand: bash
arguments:
- valueFrom: "/usr/bin/memusg.sh"
  position: 1
- valueFrom: "/cas-off/cas-off"
  position: 2
- valueFrom: "C"
  position: 4
  
inputs:
  input:
    type: File
    inputBinding:
      position: 3
  
  output_ots:
    type: string
    inputBinding:
      position: 5

outputs:
  output:
    type: File
    outputBinding:
      glob: $(inputs.output_ots)
