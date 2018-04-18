cwlVersion: v1.0
class: Workflow


requirements:
  - class: InlineJavascriptRequirement
  - class: StepInputExpressionRequirement
inputs:
  guide_count: int
  

outputs:
  outputcasoff: 
    type: File
    outputSource: casoff/outputerror
  outputflashfry: 
    type: File
    outputSource: flashfry/outputerror
  outputcrisprseek: 
    type: File
    outputSource: crisprseek/outputerror

steps:
  random_guides:
    run: flashfry_random.cwl
    in:
      output_fasta:
        valueFrom: $("flashfry" + inputs.guide_count + ".fasta")
      random_count: guide_count
    out: [output]

  crisprseek:
    run: crisprseek.cwl
    in:
      fasta: random_guides/output
      mismatches: guide_count
      outputFilename:
        valueFrom: $("crisprSeek" + inputs.guide_count + ".output")
      std_out: 
        valueFrom: $("crisprSeek" + inputs.guide_count + ".stdout")
      std_err: 
        valueFrom: $("crisprSeek" + inputs.guide_count + ".stderr")
    out: [outcalls,stdoutput,outputerror]

  flashfry:
    run: flashfry_off_target.cwl
    in:
      fasta: random_guides/output
      mismatches: guide_count
      output_scores: 
        valueFrom: $("flashfry" + inputs.guide_count + ".output")
      std_out: 
        valueFrom: $("flashfry" + inputs.guide_count + ".stdout")
      std_err: 
        valueFrom: $("flashfry" + inputs.guide_count + ".stderr")
    out: [outputscores,output,outputerror]

  casoffPrep:
    run: convert_to_cas_off_finder.cwl
    in:
      fasta: random_guides/output
      casFile:  
        valueFrom: $("casoff" + inputs.guide_count + ".input")
      mismatches: guide_count
    out: [output]

  casoff:
    run: cas-off.cwl
    in:
      input: casoffPrep/output
      output_ots:  
        valueFrom: $("casoff" + inputs.guide_count + ".output")
      std_out:  
        valueFrom: $("casoff" + inputs.guide_count + ".stdout")
      std_err:  
        valueFrom: $("casoff" + inputs.guide_count + ".stderr")
    out: [stdoutput,outputerror,outcalls]
