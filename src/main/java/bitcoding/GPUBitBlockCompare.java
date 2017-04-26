package bitcoding;

import com.aparapi.Kernel;
import com.aparapi.Range;
import crispr.GuideIndex;
import crispr.ResultsAggregator;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * a place to put parellel GPU implementations of bit encoding tests
 */
public class GPUBitBlockCompare {

    int counter = 0;

    public GPUBitBlockCompare() {

    }

    /**
     * given a block of longs representing the targets and their positions, add any potential off-targets sequences to
     * each guide it matches
     *
     * @param blockOfTargetsAndPositions an array of longs representing a block of encoded target and positions
     * @param guides                     the guides
     * @return an mapping from a potential guide to it's discovered off target sequences
     */
    public static void compareLinearBlock(long[] blockOfTargetsAndPositions,
                                          int numberOfTargets,
                                          GuideIndex[] guides,
                                          ResultsAggregator aggregator,
                                          final BitEncoding bitEncoding,
                                          int maxMismatches) {

        final long[] target = new long[numberOfTargets];
        final long[] mismatches = new long[numberOfTargets * guides.length];
        final long[] guideSequences = new long[guides.length];
        final int[] targetOffsetInLarge = new int[numberOfTargets];
        final long upperBits = BitEncoding.upperBits();
        final long mask = bitEncoding.mParameterPack().comparisonBitEncoding();
        int offset = 0;
        int dataOffset = 0;

        // process each target in the block
        while (offset < blockOfTargetsAndPositions.length) {
            int count = bitEncoding.getCount(blockOfTargetsAndPositions[offset]);
            target[dataOffset] = blockOfTargetsAndPositions[offset];

            targetOffsetInLarge[dataOffset] = offset;
            offset = offset + count + 1;
            dataOffset++;
        }

        int guideIndex = 0;
        while (guideIndex < guides.length) {
            guideSequences[guideIndex] = guides[guideIndex].guide();
            guideIndex += 1;
        }

        Kernel kernel = new Kernel() {

            @Override
            public void run() {
                int i = getGlobalId();
                mismatches[i] = localMismatches(guideSequences[i % guideSequences.length], target[i % target.length], mask, upperBits);
            }

            private int localMismatches(long encoding1, long encoding2, long additionalMask, long upperBits) {
                long firstComp = ((encoding1 ^ encoding2) & additionalMask);
                return (pop((firstComp & upperBits) | ((firstComp << 1) & upperBits)));
            }
            /**
             * Returns the number of bits set in the long -- from the aparapi example code, replaces popcnt
             */
            private int pop ( long x) {
                x = x - ((x >>> 1) & 0x5555555555555555L);
                x = (x & 0x3333333333333333L) + ((x >>> 2) & 0x3333333333333333L);
                x = (x + (x >>> 4)) & 0x0F0F0F0F0F0F0F0FL;
                x = x + (x >>> 8);
                x = x + (x >>> 16);
                x = x + (x >>> 32);
                return (int) x & 0x7F;
            }
        };

        Range range = Range.create(mismatches.length);

        kernel.execute(range);
        System.out.println("Device = " + kernel.getTargetDevice().getShortDescription() + " with number of targets " + numberOfTargets);

        //System.out.println("Execution mode = " + kernel.toString());

        // now move any results back to the respective guide
        int retIndex = 0;
        while (retIndex < mismatches.length) {
            if (mismatches[retIndex] <= maxMismatches) {
                int guideOffset = retIndex % guideSequences.length;
                int targetOffset = retIndex % target.length;
                int targetPositionSize = bitEncoding.getCount(target[targetOffset]);

                aggregator.updateOT(guides[guideOffset], new crispr.CRISPRHit(target[targetOffset], Arrays.copyOfRange(blockOfTargetsAndPositions, targetOffset, targetOffset + targetPositionSize + 1)));
            }
            retIndex++;
        }
        kernel.dispose();

    }
}
