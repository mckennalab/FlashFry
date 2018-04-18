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


baseCommand: /usr/bin/time
arguments:
- valueFrom: "-v"
  position: 1
- valueFrom: "Rscript"
  position: 2
- valueFrom: "/FlashFry_GIT/paper/CRISPRseek/run_timing_crisprseek.R"
  position: 3


stdout: $(inputs.outputFilename)

inputs:
  fasta:
    type: File
    inputBinding:
      position: 4

  mismatches:
    type: int
    inputBinding:
      position: 5

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
