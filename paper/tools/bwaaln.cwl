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

# run BWA with the following parameters:
# aln -o 0 -m 20000000 -n <mismatch_count> -k <mismatch_count> -N -l 20 <humanRef>

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
- valueFrom: "bwa"
  position: 5
- valueFrom: "aln"
  position: 6
- valueFrom: "-o"
  position: 7
- valueFrom: "0"
  position: 8
- valueFrom: "-m"
  position: 9
- valueFrom: "20000000"
  position: 10
- valueFrom: "-N"
  position: 11
- valueFrom: "-l"
  position: 12
- valueFrom: "20"
  position: 13

inputs:
  indexGenome:
    type: File
    inputBinding:
      position: 16
    secondaryFiles:
      - .amb
      - .ann
      - .bwt
      - .pac
      - .sa
    
  mismatches:
    type: int
    inputBinding:
      prefix: "-n"
      position: 14

  mismatchesTwo:
    type: int
    inputBinding:
      prefix: "-k"
      position: 15

  reads:
    type: File
    inputBinding:
      position: 17

  std_out:
    type: string

  std_err:
    type: string

outputs:
  stdoutput:
    type: stdout
    
  outputerror:
    type: stderr

