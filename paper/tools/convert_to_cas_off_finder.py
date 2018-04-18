import sys

infile = open(sys.argv[1])
output = open(sys.argv[2],"w")
mismatch = int(sys.argv[3])

output.write("/genome/chroms/\n")
output.write("NNNNNNNNNNNNNNNNNNNNNGG\n")

for line in infile:
    if not (">" in line):
        output.write(line + " " + str(mismatch))

output.close()
