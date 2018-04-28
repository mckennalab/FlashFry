from subprocess import call

testrange = ["1","10", "100", "1000"]
mismatches = ["3","4","5"]

score_output = open("/home/ec2-user/ff_git/paper/tools/runtimes_full.txt","w")


def get_wall_time(inputFile):
    inp = open(inputFile)
    for line in inp:
        if "Elapsed (wall clock) time (h:mm:ss or m:ss):" in line:
            tm = line.strip("\n").split("):")[1]
            return tm
    return "UNKNOWN"

# Maximum resident set size (kbytes): 719640
def get_mem_usage(inputFile):
    inp = open(inputFile)
    for line in inp:
        if "Maximum resident set size (kbytes)" in line:
            tm = line.strip("\n").split("):")[1]
            return tm
    return "UNKNOWN"

for i in testrange:
    for mismatch in mismatches:
        # create the yml file to run the pipeline
        yml = "/home/ec2-user/ff_git/paper/tools/run_" + i + mismatch + "_" + "setup.yml"
        ymlOutput = open(yml,"w")
        ymlOutput.write("guide_count: " + i + "\n")
        ymlOutput.write("allowed_mismatches: " + mismatch + "\n")
        ymlOutput.write("genome:\n")
        ymlOutput.write("  class: File\n")
        ymlOutput.write("  path: /genome/hg38.fa\n")
        ymlOutput.write("max_off_targets: 2000\n")
        ymlOutput.close()

        # run cwl-runner with the above yaml file
        print "running " + yml
        call(["cwl-runner","/home/ec2-user/ff_git/paper/tools/timing_pipeline_full.cwl",yml])

        try:
            # recover the timing information from the final output
            score_output.write("BWA_ALN\t" + i + "\t" + mismatch + "\t" + get_wall_time("bwaaln" + i + "_i" + mismatch + ".stderr") + "\t" + get_mem_usage("bwaaln" + i + "_i" + mismatch + ".stderr") + "\n")
            score_output.write("BWA_SAMSE\t" + i + "\t" + mismatch + "\t" + get_wall_time("samse" + i + "_i" + mismatch + ".stderr") + "\t" + get_mem_usage("samse" + i + "_i" + mismatch + ".stderr") + "\n")
            score_output.write("FLASHFRY\t" + i + "\t" + mismatch + "\t" + get_wall_time("flashfry" + i + "_i" + mismatch + ".stderr") + "\t" + get_mem_usage("flashfry" + i + "_i" + mismatch + ".stderr") + "\n")
            score_output.write("CASOFF\t" + i + "\t" + mismatch + "\t" + get_wall_time("casoff" + i + "_i" + mismatch + ".stderr") + "\t" + get_mem_usage("casoff" + i + "_i" + mismatch + ".stderr") + "\n")
            score_output.write("CRISPSEEK\t" + i + "\t" + mismatch + "\t" + get_wall_time("crisprSeek" + i + "_i" + mismatch + ".stderr") + "\t" + get_mem_usage("crisprSeek" + i + "_i" + mismatch + ".stderr") + "\n")
        except:
            print("Unable to get timing for " + i)

score_output.close()
