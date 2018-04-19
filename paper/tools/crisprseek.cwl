cwlVersion: v1.0
class: CommandLineTool
label: Score for off-targets using CRISPRseek

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
- valueFrom: "Rscript"
  position: 5
- valueFrom: "/FlashFry_GIT/paper/CRISPRseek/run_timing_crisprseek.R"
  position: 6


stdout: $(inputs.outputFilename)

inputs:
  fasta:
    type: File
    inputBinding:
      position: 7

  mismatches:
    type: int
    inputBinding:
      position: 8

  outputFilename: string

  std_out:
    type: string

  std_err:
    type: string


outputs:
  outcalls:
    type: File
    outputBinding:
      glob: $(inputs.outputFilename)
  stdoutput:
    type: stdout
  outputerror:
    type: stderr
