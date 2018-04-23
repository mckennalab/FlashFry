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
- valueFrom: "samse"
  position: 6

inputs:
  maxoccurances:
    type: int
    inputBinding:
      prefix: "-n"
      position: 7

  samfile: 
    type: string
    inputBinding:
      prefix: "-f"
      position: 8

  ref:
    type: string
    default: /genome/hg38.fa
    inputBinding:
      position: 9

  sai:
    type: File
    inputBinding:
      position: 10

  fastq:
    type: File
    inputBinding:
      position: 11
    
  std_out:
    type: string

  std_err:
    type: string

outputs:
  stdoutput:
    type: stdout
    
  outputerror:
    type: stderr

  samout:
    type: File
    outputBinding:
      glob: $(inputs.samfile)
    
    
