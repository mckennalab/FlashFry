import sys

infile = open(sys.argv[1])
output = open(sys.argv[2],"w")

output.write("/genome/chroms/\n")
output.write("NNNNNNNNNNNNNNNNNNNNNGG\n")

for line in infile:
    if not (">" in line):
        output.write(line)

output.close()
