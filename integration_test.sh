UNZIPED=./data_package
JAR=target/scala-2.12/FlashFry-assembly-1.9.1.jar
TEST_DATABASE=$UNZIPED/chr22_cas9ngg_database
mkdir tmp

java -Xmx4g -jar $JAR \
     index \
     --tmpLocation ./tmp \
     --database $UNZIPED/chr22_cas9ngg_database \
     --reference $UNZIPED/chr22.fa.gz \
     --enzyme spcas9ngg

java -Xmx4g -jar $JAR \
     discover \
     --database $UNZIPED/chr22_cas9ngg_database \
     --fasta $UNZIPED/EMX1_GAGTCCGAGCAGAAGAAGAAGGG.fasta \
     --output $UNZIPED/EMX1.output


java -Xmx4g -jar $JAR \
     score \
     --input $UNZIPED/EMX1.output \
     --output $UNZIPED/EMX1.output.scored \
     --scoringMetrics doench2014ontarget,doench2016cfd,dangerous,hsu2013,minot \
     --database $UNZIPED/chr22_cas9ngg_database



java \
    -jar $JAR \
    score  \
    --input $UNZIPED/EMX1.output \
    --output $UNZIPED/EMX1.output.scored_with_ots \
    --scoringMetrics doench2014ontarget,doench2016cfd,dangerous,hsu2013,minot \
    --database $UNZIPED/chr22_cas9ngg_database \
    --includeOTs

# check all three output files
md5 -q $UNZIPED/EMX1.output | xargs -I {} test 895e282bf486c359667e2c3e0e0e0260 = {}
echo "discovery OK if 0 == " $?

md5 -q $UNZIPED/EMX1.output.scored | xargs -I {} test a371c5764dea4ab1814813500d6c2e44 = {}
echo "scoring without OTs OK if 0 == " $?

md5 -q $UNZIPED/EMX1.output.scored_with_ots | xargs -I {} test 8fc264624da0d9119caa976b8c226f6c = {}
echo "scoring with OT output  OK if 0 == " $?



rm -r ./tmp
