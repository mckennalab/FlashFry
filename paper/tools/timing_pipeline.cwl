cwlVersion: v1.0
class: Workflow

requirements:
  - class: InlineJavascriptRequirement
  - class: StepInputExpressionRequirement
    
inputs:
  guide_count: int
  allowed_mismatches: int
  max_off_targets: int
  genome: 
    type: File

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
      count: guide_count
      iter: allowed_mismatches
      output_fasta:
        valueFrom: $("flashfry" + inputs.count + "_i" + inputs.iter + ".fasta")
      random_count: guide_count
    out: [output]

  crisprseek:
    run: crisprseek.cwl
    in:
      count: guide_count
      fasta: random_guides/output
      mismatches: allowed_mismatches
      iter: allowed_mismatches
      outputFilename:
        valueFrom: $("crisprSeek" + inputs.count + "_i" + inputs.iter + ".output")
      std_out: 
        valueFrom: $("crisprSeek" + inputs.count + "_i" + inputs.iter +  ".stdout")
      std_err: 
        valueFrom: $("crisprSeek" + inputs.count + "_i" + inputs.iter + ".stderr")
    out: [outcalls,stdoutput,outputerror]

  flashfry:
    run: flashfry_off_target.cwl
    in:
      count: guide_count
      fasta: random_guides/output
      mismatches: allowed_mismatches
      iter: allowed_mismatches
      output_scores: 
        valueFrom: $("flashfry" + inputs.count + "_i" + inputs.iter + ".output")
      std_out: 
        valueFrom: $("flashfry" + inputs.count + "_i" + inputs.iter + ".stdout")
      std_err: 
        valueFrom: $("flashfry" + inputs.count + "_i" + inputs.iter + ".stderr")
    out: [outputscores,output,outputerror]

  casoffPrep:
    run: convert_to_cas_off_finder.cwl
    in:
      mismatches: allowed_mismatches
      fasta: random_guides/output
      iter: allowed_mismatches
      casFile:  
        valueFrom: $("casoff" + inputs.count + "_i" + inputs.iter + ".input")
      fastq:
        valueFrom: $("casoff" + inputs.count + "_i" + inputs.iter + ".fastq")
      mismatches: guide_count
    out: [casFile,fastq]

  casoff:
    run: cas-off.cwl
    in:
      count: guide_count
      input: casoffPrep/casFile
      iter: allowed_mismatches
      output_ots:  
        valueFrom: $("casoff" + inputs.count + "_i" + inputs.iter +  ".output")
      std_out:  
        valueFrom: $("casoff" + inputs.count + "_i" + inputs.iter + ".stdout")
      std_err:  
        valueFrom: $("casoff" + inputs.count + "_i" + inputs.iter + ".stderr")
    out: [stdoutput,outputerror,outcalls]

  bwa_aln:
    run: bwaaln.cwl
    in:
      reads: casoffPrep/fastq
      indexGenome: genome
      count: guide_count
      iter: allowed_mismatches

      mismatches: allowed_mismatches
      mismatchesTwo: allowed_mismatches
      std_err: 
        valueFrom: $("bwaaln" + inputs.count + "_i" + inputs.iter + ".stderr")
      std_out: 
        valueFrom: $("bwaaln" + inputs.count + "_i" + inputs.iter + ".stdout")
    out: [stdoutput, outputerror]

  bwa_samse:
    run: bwasamse.cwl
    in:
      count: guide_count
      iter: allowed_mismatches
      maxoccurances: max_off_targets
      samfile:
        valueFrom: $("samse" + inputs.count + "_i" + inputs.iter + ".sam")
      fastq: casoffPrep/fastq
      sai: bwa_aln/std_out
      ref: genome
      std_out: 
        valueFrom: $("samse" + inputs.count + "_i" + inputs.iter + ".stdout")
      std_err: 
        valueFrom: $("samse" + inputs.count + "_i" + inputs.iter + ".stderr")

    out: [stdoutput,stderror,samout]