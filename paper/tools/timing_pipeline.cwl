cwlVersion: v1.0
class: Workflow
inputs:
  guide_count: int
  

outputs:
  classout:
    type: File
    outputSource: compile/classfile

steps:
  random_guides:
    run: flashfry_random.cwl
    in:
      random_count: guide_count
    out: [output]

  crisprseek:
    run: crisprseek.cwl
    in:
      fasta: random_guides.output
      mismatches: guide_count
      outputFilename: $("crisprSeek" + input.guide_count + ".output")
      std_out: $("crisprSeek" + input.guide_count + ".stdout")
      std_err: $("crisprSeek" + input.guide_count + ".stderr")
    out: [outcalls,output,outputerror]

  flashfry:
    run: crisprseek.cwl
    in:
      fasta: random_guides.output
      mismatches: guide_count
      output_scores: $("flashfry" + input.guide_count + ".output")
      std_out: $("flashfry" + input.guide_count + ".stdout")
      std_err: $("flashfry" + input.guide_count + ".stderr")
    out: [outputscores,output,outputerror]

  casoffPrep:
    run: convert_to_cas_off_finder.cwl
    in:
      fasta: random_guides.output
      casFile: $("casoff" + input.guide_count + ".input")
      mismatches: guide_count
    out: [output]

  casoff:
    run: cas-off.cwl
    in:
      input: casoffPrep.guide_count
      output_ots: $("casoff" + input.guide_count + ".output")
      std_out: $("casoff" + input.guide_count + ".stdout")
      std_err: $("casoff" + input.guide_count + ".stderr")
    out: [outcalls,output,outputerror]
