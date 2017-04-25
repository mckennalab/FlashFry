package bitcoding;

import com.aparapi.Kernel;
import com.aparapi.Range;
import crispr.GuideIndex;
import crispr.ResultsAggregator;

import java.util.ArrayList;

/**
 * a place to put parellel GPU implementations of bit encoding tests
 */
public class GPUBitBlockCompare {
    /**
     * given a block of longs representing the targets and their positions, add any potential off-targets sequences to
     * each guide it matches
     *
     * @param blockOfTargetsAndPositions an array of longs representing a block of encoded target and positions
     * @param guides                     the guides
     * @return an mapping from a potential guide to it's discovered off target sequences
     */
    public static void compareLinearBlock(Long[] blockOfTargetsAndPositions,
                                          int numberOfTargets,
                                          ArrayList<GuideIndex> guides,
                                          ResultsAggregator aggregator,
                                          BitEncoding bitEncoding,
                                          int maxMismatches,
                                          BinAndMask bin) {

        final long[] data = new long[numberOfTargets];
        final long[] mismatches = new long[numberOfTargets * guides.size()];
        final long[] guideSequences = new long[guides.size()];

        int offset = 0;
        int dataOffset = 0;

        // process each target in the block
        while (offset < blockOfTargetsAndPositions.length) {
            int count = bitEncoding.getCount(blockOfTargetsAndPositions[offset]);
            data[dataOffset] = blockOfTargetsAndPositions[offset];

            offset = offset + count + 1;
            dataOffset++;
        }

        int guideOffset = 0;
        while (guideOffset < guides.size()) {
            guideSequences[guideOffset] = guides.get(guideOffset).guide();
            guideOffset += 1;
        }

        Kernel kernel = new Kernel() {

            @Override
            public void run() {
                int i = getGlobalId();
                mismatches[i] = bitEncoding.mismatches(guideSequences[i % guideSequences.length], data[i % data.length],BitEncoding.stringMask());
            }
        };

        Range range = Range.create(mismatches.length);
        kernel.execute(range);
    }

}
