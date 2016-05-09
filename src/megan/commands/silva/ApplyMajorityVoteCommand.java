/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.commands.silva;

import jloda.graph.Node;
import jloda.gui.commands.ICommand;
import jloda.gui.director.IDirector;
import jloda.util.Pair;
import jloda.util.parse.NexusStreamParser;
import megan.commands.CommandBase;
import megan.core.Document;
import megan.core.SyncArchiveAndDataTable;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.data.UpdateItemList;
import megan.rma2.ClassReadIdIteratorRMA2;
import megan.rma2.RMA2Connector;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class ApplyMajorityVoteCommand extends CommandBase implements ICommand {

    @Override
    public String getName() {
        return "Apply Majority Vote";
    }

    @Override
    public String getDescription() {
        return "Apply a the majority vote filter. Reads with a defined percentage of matches assigned to one taxon will bypass the LCA and assign the read to this taxon.";
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }

    @Override
    public boolean isCritical() {
        return true;
    }

    @Override
    public boolean isApplicable() {
        Document doc = getDoc();
        return !doc.getMeganFile().isReadOnly() && doc.getMeganFile().hasDataConnector();
    }

    @Override
    public void apply(NexusStreamParser np) throws Exception {
        np.matchIgnoreCase("apply majorityVote ");
        double confidence = 0d;
        if (np.peekMatchIgnoreCase("voteConfidence")) {
            np.matchIgnoreCase("voteConfidence=");
            confidence = np.getDouble(0d, 100d);
        }
        RMA2Connector con = (RMA2Connector) getDoc().getConnector();
        Map<Long, Integer> readKey2TaxId = new HashMap<>();
        System.out.println("Loading Classification values for all Reads");
        long now = System.currentTimeMillis();
        ClassReadIdIteratorRMA2 classreadIt = new ClassReadIdIteratorRMA2("Taxonomy", new File(getDoc().getMeganFile().getFileName()));
        while (classreadIt.hasNext()) {
            Pair<Integer, List<Long>> pair = classreadIt.next();
            int classId = pair.getFirstInt();
            List<Long> readIds = pair.get2();
            for (Long readId : readIds) {
                readKey2TaxId.put(readId, classId);
            }

        }
        classreadIt.close();

        System.out.println("Loading Classification values for all Reads - completed; took: " + TimeUnit.MILLISECONDS.toSeconds((System.currentTimeMillis() - now)) + " seconds");
        Document doc = getDoc();

        System.out.println("Loading Classification Values for all Reads");
        now = System.currentTimeMillis();
        final IReadBlockIterator it = con.getAllReadsIterator(doc.getMinScore(), doc.getMaxExpected(), true, true);
        int changed = 0;
        int num = 0;
        Map<Integer, Integer> id2treelevel = new HashMap<>();
        while (it.hasNext()) {
            num++;
            if (num % 1000 == 0) {
                System.out.println(num + " of " + readKey2TaxId.size() + " read analyzed. In seconds: " + TimeUnit.MILLISECONDS.toSeconds((System.currentTimeMillis() - now)));

            }
            IReadBlock irb = it.next();
            Map<Integer, Integer> tax2count = new HashMap<>();
            int nom = 0;
            doc.setMaxExpected(0.0f);
            doc.setTopPercent(2.0f);
            doc.setMinScore(300);
            doc.setMinSupport(1);
            List<IMatchBlock> activeMatches = getActiveMatches(doc.getMinScore(), doc.getMaxExpected(), doc.getTopPercent(), irb.getMatchBlocks());
            for (IMatchBlock imb : activeMatches) {

                if (tax2count.containsKey(imb.getTaxonId())) {
                    tax2count.put(imb.getTaxonId(), tax2count.get(imb.getTaxonId()) + 1);
                } else {
                    tax2count.put(imb.getTaxonId(), 1);
                }
                nom++;
            }
            if (nom == 0)
                continue;
            double largestPerc = 0;
            int taxidOfLargestPerc = -1;
            for (Entry<Integer, Integer> entry : tax2count.entrySet()) {
                double perc = ((double) entry.getValue() / (double) nom) * 100d;
                if (perc > largestPerc) {
                    largestPerc = perc;
                    taxidOfLargestPerc = entry.getKey();
                }
            }

            if (largestPerc >= confidence) {
                if (readKey2TaxId.get(irb.getUId()) == taxidOfLargestPerc) {

                } else {
                    readKey2TaxId.put(irb.getUId(), taxidOfLargestPerc);
                    changed++;
                }


            } else {
                //move up tax tree

                Map<Integer, Integer> tax2counttmp = new HashMap<>();
                while (true) {
                    int taxlvl2update = 0;
                    for (Entry<Integer, Integer> entry : tax2count.entrySet()) {
                        int taxid = entry.getKey();
                        int thistaxlvl = 0;
                        if (!id2treelevel.containsKey(taxid)) {
                            int treelvl = getTreeLevel(taxid);
                            id2treelevel.put(taxid, treelvl);
                        }
                        thistaxlvl = id2treelevel.get(taxid);
                        if (thistaxlvl > taxlvl2update) {
                            taxlvl2update = thistaxlvl;
                        }
                    }


                    for (Entry<Integer, Integer> entry : tax2count.entrySet()) {
                        int taxid = entry.getKey();
                        int count = entry.getValue();
                        int newTaxId = 1;
                        if (taxlvl2update == id2treelevel.get(taxid)) {
                            if (taxid != 1)
                                newTaxId = (Integer) TaxonomyData.getTree().getANode(taxid).getFirstInEdge().getSource().getInfo();
                        } else {
                            newTaxId = taxid;
                        }

                        if (tax2counttmp.containsKey(newTaxId)) {
                            tax2counttmp.put(newTaxId, tax2counttmp.get(newTaxId) + count);
                        } else {
                            tax2counttmp.put(newTaxId, count);
                        }
                    }

                    tax2count = tax2counttmp;
                    tax2counttmp = new HashMap<>();
                    largestPerc = 0;
                    taxidOfLargestPerc = -1;
                    for (Entry<Integer, Integer> entry2 : tax2count.entrySet()) {
                        double perc = ((double) entry2.getValue() / (double) nom) * 100d;
                        if (perc > largestPerc) {
                            largestPerc = perc;
                            taxidOfLargestPerc = entry2.getKey();
                        }
                    }

                    if (largestPerc >= confidence) {
                        if (readKey2TaxId.get(irb.getUId()) == taxidOfLargestPerc) {
                        } else {
                            readKey2TaxId.put(irb.getUId(), taxidOfLargestPerc);
                            changed++;
                        }
                        break;
                    }

                }

            }


        }

        UpdateItemList list = new UpdateItemList(1);
        for (Entry<Long, Integer> rk2taxid : readKey2TaxId.entrySet()) {
            list.addItem(rk2taxid.getKey(), 1, new Integer[]{rk2taxid.getValue()});
        }
        System.out.println("Done retrieving reads. Took " + TimeUnit.MILLISECONDS.toSeconds((System.currentTimeMillis() - now)) + " seconds.");
        now = System.currentTimeMillis();
        System.out.println("Writing back classifications.");
        con.updateClassifications(new String[]{"Taxonomy"}, list, doc.getProgressListener());
        SyncArchiveAndDataTable.syncRecomputedArchive2Summary(doc.getTitle(), "LCA", doc.getBlastMode(), doc.getParameterString(), con, doc.getDataTable(), (int) doc.getAdditionalReads());

        SyncArchiveAndDataTable.syncRecomputedArchive2Summary(doc.getTitle(), "LCA", doc.getBlastMode(), doc.getParameterString(), con, doc.getDataTable(), (int) doc.getAdditionalReads());
        doc.setDirty(false);
        getViewer().updateView(IDirector.ALL);

        execute("update reinduce=true;");
        getDoc().saveAuxiliaryData();
        System.out.println("Writing to RMA took: " + TimeUnit.MILLISECONDS.toSeconds((System.currentTimeMillis() - now)) + " seconds.");
        System.out.println("DONE");

    }

    private List<IMatchBlock> getActiveMatches(float minscore, float minExpected, float topPercent, IMatchBlock[] matchblocks) {
        double biggestScore = 0;
        double toppercentMinScore = 0;
        double minExp = 0;
        for (IMatchBlock imb : matchblocks) {
            float bs = imb.getBitScore();
            if (bs > biggestScore) {
                biggestScore = bs;
            }
        }
        if (minExpected == 0) {
            minExp = 100000;
        } else {
            minExp = minExpected;
        }
        if (!(topPercent == 0)) {
            toppercentMinScore = (100d - topPercent) / 100d * biggestScore;
        } else {
            toppercentMinScore = minscore;
        }
        List<IMatchBlock> activeMatches = new ArrayList<>();
        for (IMatchBlock imb : matchblocks) {
            float bs = imb.getBitScore();
            float minex = imb.getExpected();
            if (bs > toppercentMinScore) {
                if (minex < minExp) {
                    activeMatches.add(imb);
                }
            }
        }
        return activeMatches;


    }

    @Override
    public String getSyntax() {
        return "apply majorityVote voteConfidence=<%ofConfidence>";
    }

    @Override
    public void actionPerformed(ActionEvent ev) {

    }

    public int getTreeLevel(int taxid) {
        int taxlvl = 0;
        Node v = TaxonomyData.getTree().getANode(taxid);
        int leafTaxId = -1;
        while (true) {
            if (leafTaxId == -1) {
                leafTaxId = (Integer) v.getInfo();
            }
            Integer taxId = (Integer) v.getInfo();
            if (taxId != null)
                taxlvl++;
            if (v.getInDegree() > 0)
                v = v.getFirstInEdge().getSource();
            else
                break;
        }
        return taxlvl;
    }

}
