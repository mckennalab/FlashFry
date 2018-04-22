import sys

infile = open(sys.argv[1])
output_casoff = open(sys.argv[2],"w")
output_fastq = open(sys.argv[3],"w")
mismatch = int(sys.argv[4])

output_casoff.write("/genome/chroms/\n")
output_casoff.write("NNNNNNNNNNNNNNNNNNNNNGG\n")

quals = "HHHHHHHHHHHHHHHHHHHHHHH"

for line in infile:
    if not (">" in line):
        output_casoff.write(line.strip("\n") + " " + str(mismatch) + "\n")
        output_fastq.write("@" + line.strip("\n") + "\n" + line + "+\n" + quals + "\n")

output_casoff.close()
output_fastq.close()
