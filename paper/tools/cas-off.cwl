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
- valueFrom: "/cas-off/cas-off"
  position: 5
- valueFrom: "C"
  position: 7

inputs:
  input:
    type: File
    inputBinding:
      position: 6

  output_ots:
    type: string
    inputBinding:
      position: 8

  std_out:
    type: string

  std_err:
    type: string

outputs:
  stdoutput:
    type: stdout
  outputerror:
    type: stderr
  outcalls:
    type: File
    outputBinding:
      glob: $(inputs.output_ots)