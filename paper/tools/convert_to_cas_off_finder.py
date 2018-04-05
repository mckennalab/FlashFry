infile = open(sys.argv[1])
output = open(sys.argv[2],"w")

output.write("/genome/hg38.fa\n")
output.write("NNNNNNNNNNNNNNNNNNNNNRG\n")

for line in infile:
    if not (">" in line):
        output.write(line)

output.close()
