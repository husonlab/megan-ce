/*
 *  Copyright (C) 2017 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package megan.algorithms;

import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import megan.classification.Classification;
import megan.classification.IdMapper;
import megan.classification.data.ClassificationFullTree;
import megan.core.Document;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.util.interval.Interval;
import megan.util.interval.IntervalTree;

import java.util.*;

/**
 * performs taxonId assignment using a coverage based algorithm
 * Created by huson on 4/12/17.
 */
public class AssignmentUsingCoverageBasedLCA implements IAssignmentAlgorithm {
    private final Comparator<StartStopEvent> comparator;
    private final Map<Integer, Integer> taxon2weight = new HashMap<>();
    private final ClassificationFullTree fullTree;

    private final AssignmentUsingWeightedLCA assignmentUsingWeightedLCA;

    private final IntervalTree<Integer> intervals = new IntervalTree<>();
    private final float topPercentFactor;

    private StartStopEvent[] events = new StartStopEvent[10000]; // not final because may get resized...

    private Taxon2SpeciesMapping taxon2SpeciesMapping;

    /**
     * constructor
     */
    public AssignmentUsingCoverageBasedLCA(Document doc, final float topPercent) {
        comparator = createComparator();
        assignmentUsingWeightedLCA = new AssignmentUsingWeightedLCA(Classification.Taxonomy, null, null, null, doc.getWeightedLCAPercent(), false);
        assignmentUsingWeightedLCA.setIgnoreAncestors(false); // don't ignore ancestors
        fullTree = assignmentUsingWeightedLCA.getFullTree();
        topPercentFactor = (topPercent == 0 || topPercent == 100 ? 1 : Math.min(1f, topPercent / 100.0f));
        try {
            taxon2SpeciesMapping = (ProgramProperties.get("CollapseSpeciesLongReadLCA", false) ? Taxon2SpeciesMapping.getInstance(doc.getProgressListener()) : null);
            if (taxon2SpeciesMapping != null)
                System.err.println("Using collapseSpecies option: At most one alignment per species used");
        } catch (CanceledException e) {
            Basic.caught(e);
        }
    }

    /**
     * compute taxonId id
     *
     * @param activeMatches
     * @param readBlock
     * @return taxonId id
     */
    public int computeId(BitSet activeMatches, IReadBlock readBlock) {
        if (readBlock.getNumberOfMatches() == 0)
            return IdMapper.NOHITS_ID;

        /*
        System.err.println("READ: "+readBlock.getReadName());
        for(int m=0;m<readBlock.getNumberOfAvailableMatchBlocks();m++) {
            if(activeMatches.get(m)) {
                final IMatchBlock matchBlock=readBlock.getMatchBlock(m);
                System.err.println(TaxonomyData.getName2IdMap().get(matchBlock.getTaxonId())+": "+matchBlock.getBitScore()
                +" range: "+Math.min(matchBlock.getAlignedQueryStart(),matchBlock.getAlignedQueryEnd())+" - "
                        +Math.max(matchBlock.getAlignedQueryStart(),matchBlock.getAlignedQueryEnd()));
            }
        }
        */

        computeTaxon2WeightMapping(activeMatches, readBlock, taxon2weight);

        final String address = assignmentUsingWeightedLCA.computeWeightedLCA(getPercentToCover(), taxon2weight);
        if (address == null)
            return 0;
        else
            return fullTree.getAddress2Id(address);
    }

    /**
     * compute the taxonId 2 weight mapping
     *
     * @param activeMatches0
     * @param readBlock
     */
    private void computeTaxon2WeightMapping(BitSet activeMatches0, IReadBlock readBlock, Map<Integer, Integer> taxon2weight) {
        final BitSet activeMatches;
        if (topPercentFactor < 1) {
            activeMatches = (new BitSet());
            activeMatches.or(activeMatches0);
            intervals.clear();
            for (int m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
                final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
                if (matchBlock.getTaxonId() > 0) {
                    final Interval<Integer> interval = new Interval<>(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd(), m);
                    intervals.add(interval);
                } else
                    activeMatches.clear(m); // doesn't have a tax-id, don't use
            }
            for (Interval<Integer> interval : intervals) {
                final IMatchBlock matchBlock = readBlock.getMatchBlock(interval.getData());

                for (Interval<Integer> overlapper : intervals.getIntervals(interval)) {
                    if (overlapper.contains(interval) && matchBlock.getBitScore() < topPercentFactor * readBlock.getMatchBlock(overlapper.getData()).getBitScore()) {
                        activeMatches.clear(interval.getData());
                        break;
                    }
                }
            }
        } else
            activeMatches = activeMatches0;


        int numberOfEvents = 0;

        for (int m = 0; m < readBlock.getNumberOfAvailableMatchBlocks(); m++) {
            if (activeMatches.get(m)) {
                final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
                int taxonId = matchBlock.getTaxonId();
                if (taxonId > 0) {
                    if (numberOfEvents + 1 >= events.length) { // need enough to add two new events
                        StartStopEvent[] tmp = new StartStopEvent[2 * events.length];
                        System.arraycopy(events, 0, tmp, 0, numberOfEvents);
                        events = tmp;
                    }
                    if (events[numberOfEvents] == null)
                        events[numberOfEvents] = new StartStopEvent();
                    events[numberOfEvents++].set(true, Math.min(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd()), m);
                    if (events[numberOfEvents] == null)
                        events[numberOfEvents] = new StartStopEvent();
                    events[numberOfEvents++].set(false, Math.max(matchBlock.getAlignedQueryStart(), matchBlock.getAlignedQueryEnd()), m);
                }
            }
        }
        Arrays.sort(events, 0, numberOfEvents, comparator);

        taxon2weight.clear();

        final BitSet currentMatches = new BitSet(); // set of matches currently active

        StartStopEvent previousEvent = null;
        for (int c = 0; c < numberOfEvents; c++) {
            final StartStopEvent currentEvent = events[c];
            if (previousEvent == null) {
                if (!currentEvent.isStart())
                    throw new RuntimeException("Taxon end before begin: " + currentEvent);
                currentMatches.set(currentEvent.getMatchId());
            } else {
                if (currentEvent.getPos() > previousEvent.getPos()) {
                    final int segmentLength = (currentEvent.getPos() - previousEvent.getPos() + 1); // length of segment

                    if (segmentLength > 0) {
                        // System.err.println("Segment: "+previousEvent.getPos()+" - "+currentEvent.getPos()+" length: "+segmentLength);

                        // setup
                        final HashMap<Integer, Integer> match2taxonId;
                        if (taxon2SpeciesMapping != null) { // this means that isCollapseSpecies is true
                            int orginalTaxonSeen = -1; // -1: seen nothing: 0: seen more than one taxon, otherwise (n): all taxa seen are of type n
                            final Map<Integer, Pair<Integer, Float>> speciesToMatchIdAndScore = new HashMap<>();
                            for (int m = currentMatches.nextSetBit(0); m != -1; m = currentMatches.nextSetBit(m + 1)) {
                                final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
                                final int matchLength = Math.abs(matchBlock.getAlignedQueryStart() - matchBlock.getAlignedQueryEnd()) + 1;
                                final float bitScorePerBase = matchBlock.getBitScore() / matchLength;
                                final int originalTaxonId = matchBlock.getTaxonId();
                                final int taxonId = taxon2SpeciesMapping.getSpeciesOrReturnTaxonId(originalTaxonId);
                                if (originalTaxonId > 0) {
                                    switch (orginalTaxonSeen) {
                                        case -1:
                                            orginalTaxonSeen = originalTaxonId;
                                            break;
                                        case 0: //  have seen more than one
                                            break;
                                        default: // if we see another taxon then set taxon seen to 0
                                            if (originalTaxonId != orginalTaxonSeen)
                                                orginalTaxonSeen = 0;
                                    }
                                }
                                // System.err.println("Mapping "+matchBlock.getTaxonId()+" -> "+taxonId);

                                if (taxonId > 0) {
                                    final Pair<Integer, Float> matchAndScore = speciesToMatchIdAndScore.get(taxonId);
                                    if (matchAndScore != null) {
                                        if (bitScorePerBase > matchAndScore.getSecond()) {
                                            matchAndScore.setFirst(m);
                                            matchAndScore.setSecond(bitScorePerBase);
                                        }
                                    } else {
                                        speciesToMatchIdAndScore.put(taxonId, new Pair<>(m, bitScorePerBase));
                                    }
                                }
                            }
                            if (speciesToMatchIdAndScore.size() >= 1 && orginalTaxonSeen == 0) { // only use species-based filtering if more than one original taxon seen
                                match2taxonId = new HashMap<>();
                                for (Integer id : speciesToMatchIdAndScore.keySet()) {
                                    Pair<Integer, Float> matchAndScore = speciesToMatchIdAndScore.get(id);
                                    // System.err.println("match m="+ matchAndScore.getFirst()+" taxon: "+id+" ("+ TaxonomyData.getName2IdMap().get(id)+") score: "+matchAndScore.getSecond());
                                    match2taxonId.put(matchAndScore.getFirst(), id);
                                }
                            } else { // if all
                                match2taxonId = null;
                            }
                        } else
                            match2taxonId = null;

                        // compute total bit score for segment as sum of bit scores per segment
                        float totalBitScorePerBase = 0;
                        {
                            for (int m = currentMatches.nextSetBit(0); m != -1; m = currentMatches.nextSetBit(m + 1)) {
                                if (match2taxonId == null || match2taxonId.containsKey(m)) {
                                    final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
                                    final int matchLength = Math.abs(matchBlock.getAlignedQueryStart() - matchBlock.getAlignedQueryEnd()) + 1;
                                    final float bitScorePerBase = matchBlock.getBitScore() / matchLength;
                                    totalBitScorePerBase += bitScorePerBase;
                                }
                            }
                            if (totalBitScorePerBase == 0)
                                totalBitScorePerBase = 1;
                        }

                        for (int m = currentMatches.nextSetBit(0); m != -1; m = currentMatches.nextSetBit(m + 1)) {
                            if (match2taxonId == null || match2taxonId.containsKey(m)) {
                                final IMatchBlock matchBlock = readBlock.getMatchBlock(m);
                                final int originalTaxonId = matchBlock.getTaxonId();
                                final int taxonId = (match2taxonId == null ? originalTaxonId : match2taxonId.get(m));
                                if (taxonId > 0) {
                                    final int matchLength = Math.abs(matchBlock.getAlignedQueryStart() - matchBlock.getAlignedQueryEnd()) + 1;
                                    final float bitScorePerBase = matchBlock.getBitScore() / matchLength;

                                    final int score = Math.round((10000 * segmentLength * bitScorePerBase) / totalBitScorePerBase); // multiple by 10000 to avoid truncation when converting to int
                                    final Integer weight = taxon2weight.get(taxonId);
                                    //System.err.println(taxonId+" ("+TaxonomyData.getName2IdMap().get(taxonId) + ") adding: " + score);

                                    if (weight == null)
                                        taxon2weight.put(taxonId, score);
                                    else
                                        taxon2weight.put(taxonId, weight + score);
                                }
                            }
                        }
                    }
                }

                if (currentEvent.isStart()) {
                    currentMatches.set(currentEvent.getMatchId());
                } else { // is end event
                    currentMatches.clear(currentEvent.getMatchId());
                }
            }
            previousEvent = currentEvent;
        }
        /*
         {
            System.err.println("Final weights for read:");
            for (int id : taxon2weight.keySet()) {
                System.err.println(id + " (" + TaxonomyData.getName2IdMap().get(id) + ") score: " + taxon2weight.get(id));
            }
        }
        */
    }

    @Override
    public int getLCA(int id1, int id2) {
        if (id1 == 0)
            return id2;
        else if (id2 == 0)
            return id1;
        else
            return fullTree.getAddress2Id(LCAAddressing.getCommonPrefix(new String[]{fullTree.getAddress(id1), fullTree.getAddress(id2)}, 2, false));
    }

    public float getPercentToCover() {
        return assignmentUsingWeightedLCA.getPercentToCover();
    }

    private Comparator<StartStopEvent> createComparator() {
        return new Comparator<StartStopEvent>() {
            @Override
            public int compare(StartStopEvent a, StartStopEvent b) {
                if (a.getPos() < b.getPos())
                    return -1;
                else if (a.getPos() > b.getPos())
                    return 1;
                else if (a.isStart() && b.isEnd())
                    return -1;
                else if (a.isEnd() && b.isStart())
                    return 1;
                else
                    return 0;
            }
        };
    }

    private class StartStopEvent {
        private boolean start;
        private int pos;
        private int matchId;

        public void set(boolean start, int pos, int matchId) {
            this.start = start;
            this.pos = pos;
            this.matchId = matchId;
        }

        public boolean isStart() {
            return start;
        }

        public boolean isEnd() {
            return !start;
        }

        public int getPos() {
            return pos;
        }

        public int getMatchId() {
            return matchId;
        }
    }
}
