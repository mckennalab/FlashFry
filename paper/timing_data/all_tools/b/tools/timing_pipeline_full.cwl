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
    secondaryFiles:
      - .amb
      - .ann
      - .bwt
      - .pac
      - .sa
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
  outputbwa-aln:
    type: File
    outputSource: bwa_aln/outputerror
  outputbwa-samse:
    type: File
    outputSource: bwa_samse/outputerror

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
      count: guide_count
      casFile:  
        valueFrom: $("casoff" + inputs.count + "_i" + inputs.iter + ".input")
      fastq:
        valueFrom: $("casoff" + inputs.count + "_i" + inputs.iter + ".fastq")
      mismatches: allowed_mismatches
    out: [casFileOut,fastqOut]

  casoff:
    run: cas-off.cwl
    in:
      count: guide_count
      input: casoffPrep/casFileOut
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
      reads: casoffPrep/fastqOut
      count: guide_count
      iter: allowed_mismatches
      mismatches: allowed_mismatches
      mismatchesTwo: allowed_mismatches
      stdOut:
        valueFrom: $("bwaaln" + inputs.count + "_i" + inputs.iter + ".stdout")
      stdErr: 
        valueFrom: $("bwaaln" + inputs.count + "_i" + inputs.iter + ".stderr")
    out: [stdoutput, outputerror]

  bwa_samse:
    run: bwasamse.cwl
    in:
      count: guide_count
      iter: allowed_mismatches
      maxoccurances: max_off_targets
      samfile:
        valueFrom: $("samse" + inputs.count + "_i" + inputs.iter + ".sam")
      fastq: casoffPrep/fastqOut
      sai: bwa_aln/stdoutput
      std_out: 
        valueFrom: $("samse" + inputs.count + "_i" + inputs.iter + ".stdout")
      std_err: 
        valueFrom: $("samse" + inputs.count + "_i" + inputs.iter + ".stderr")

    out: [stdoutput,outputerror,samout]
