/*
 *  Copyright (C) 2019 Daniel H. Huson
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
package megan.stats;

import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.ProgressListener;
import jloda.util.parse.NexusStreamParser;
import megan.commands.show.ShowMessageWindowCommand;
import megan.core.Director;
import megan.viewer.TaxonomyData;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * the combobox item for the resampling method
 * Daniel Huson, 2.2008
 */
public class ResamplingMethodItem extends JButton implements IMethodItem {
    private final ResamplingMethod resamplingMethod;
    private final JPanel panel;
    private final JTextField resamplingSizeTF;
    private final JTextField repeatitionsTF;
    private final JTextField percentileLeftTF;
    private final JCheckBox useInternalCB;
    private final JCheckBox useNoHitsUnassignedCB;

    private boolean optionUseUnassigned = false;
    private boolean optionUseInternal = false;

    public static final String NAME = "Resampling";

    /**
     * constructor
     */
    public ResamplingMethodItem() {
        super();
        setName(NAME);
        this.resamplingMethod = new ResamplingMethod();
        panel = new JPanel();
        panel.setLayout(new GridLayout(5, 2));
        panel.add(new JLabel("Resampling size:"));
        resamplingSizeTF = new JTextField("" + resamplingMethod.getOptionResamplingSize());
        panel.add(resamplingSizeTF);
        panel.add(new JLabel("Repeatitions:"));
        repeatitionsTF = new JTextField("" + resamplingMethod.getOptionRepeatitions());
        panel.add(repeatitionsTF);
        panel.add(new JLabel("Percentile:"));
        percentileLeftTF = new JTextField("" + (float) (resamplingMethod.getOptionLeftPercentile()));
        panel.add(percentileLeftTF);

        useInternalCB = new JCheckBox();
        useInternalCB.setSelected(getOptionUseInternal());
        panel.add(new JLabel("Include higher levels:"));
        panel.add(useInternalCB);


        useNoHitsUnassignedCB = new JCheckBox();
        useNoHitsUnassignedCB.setSelected(getOptionUseUnassigned());
        panel.add(new JLabel("Include unassigned:"));
        panel.add(useNoHitsUnassignedCB);
    }

    /**
     * get string representation
     *
     * @return string
     */
    public String toString() {
        return getName();
    }

    /**
     * gets the options panel associated with this statistical method
     *
     * @return options panel
     */
    public JPanel getOptionsPanel() {
        return panel;
    }

    /**
     * check whether current entries are valid
     *
     * @return true, if all entries valid
     */
    public boolean isApplicable() {
        try {

            resamplingMethod.setOptionResamplingSize(Integer.parseInt(resamplingSizeTF.getText()));
            resamplingMethod.setOptionRepeatitions(Integer.parseInt(repeatitionsTF.getText()));
            resamplingMethod.setOptionLeftPercentile(Double.parseDouble(percentileLeftTF.getText()));

            return resamplingMethod.isApplicable();
        } catch (NumberFormatException ex) {
            System.err.println("Error: " + ex);
            return false;
        }
    }

    /**
     * returns a string description of the chosen parameters
     *
     * @return
     */
    public String getOptionsString() {
        return "resamplingsize=" + resamplingSizeTF.getText() + " repeatitions=" + repeatitionsTF.getText()
                + " percentile=" + percentileLeftTF.getText()
                + " useinternal=" + useInternalCB.isSelected()
                + " useunassigned=" + useNoHitsUnassignedCB.isSelected();
    }

    /**
     * parse an options string
     *
     * @param string
     * @throws IOException
     */
    public void parseOptionString(String string) throws IOException {
        NexusStreamParser np = new NexusStreamParser(new StringReader(string));
        java.util.List tokens = np.getTokensLowerCase(null, null);
        resamplingMethod.setOptionResamplingSize((int) np.findIgnoreCase(tokens, "resamplingsize=", resamplingMethod.getOptionResamplingSize()));
        resamplingMethod.setOptionRepeatitions((int) np.findIgnoreCase(tokens, "repeatitions=", resamplingMethod.getOptionRepeatitions()));
        resamplingMethod.setOptionLeftPercentile(np.findIgnoreCase(tokens, "percentile=", (float) resamplingMethod.getOptionLeftPercentile()));
        if (np.findIgnoreCase(tokens, "includeinternal=true"))
            setOptionUseInternal(true);
        else if (np.findIgnoreCase(tokens, "includeinternal=false"))
            setOptionUseInternal(false);
        if (np.findIgnoreCase(tokens, "includeunassigned=true"))
            setOptionUseUnassigned(true);
        else if (np.findIgnoreCase(tokens, "includeunassigned=false"))
            setOptionUseUnassigned(false);

        np.checkFindDone(tokens);
    }

    /**
     * set the two input datasets for the resampling method
     *
     * @param m1
     * @param m2
     */
    public void setInput(Map<Integer, Float> m1, Map<Integer, Float> m2) {
        resamplingMethod.setInput(m1, m2);
    }

    /**
     * apply the calculation
     */
    public void apply(ProgressListener progressListener) throws CanceledException {
        resamplingMethod.apply(progressListener);
    }

    /**
     * get the output
     *
     * @return output
     */
    public Map<Integer, Double> getOutput() {
        return resamplingMethod.getOutput();
    }

    /**
     * rank taxa by decreasing score and display the result
     *
     * @param taxId2score
     * @param dir1
     * @param dir2
     */
    static void displayResult(Map<Integer, Double> taxId2score, Director dir1, Director dir2) {
        SortedSet<Pair<Integer, Double>> set = new TreeSet<>((p1, p2) -> {
            int taxId1 = p1.getFirst();
            double value1 = p1.getSecond();
            int taxId2 = p2.getFirst();
            double value2 = p2.getSecond();
            if (value1 > value2)
                return -1;
            else if (value1 < value2)
                return 1;
            else if (taxId1 < taxId2)
                return -1;
            else if (taxId1 > taxId2)
                return 1;
            else
                return 0;

        });

        for (Integer taxId : taxId2score.keySet()) {
            double value = taxId2score.get(taxId);
            set.add(new Pair<>(taxId, value));
        }

        java.util.List<Pair<String, Double>> list = new LinkedList<>();
        for (Pair<Integer, Double> p : set) {
            String taxName = TaxonomyData.getName2IdMap().get(p.getFirst());
            if (taxName != null) {
                Pair<String, Double> newPair = new Pair<>(taxName, p.getSecond());
                list.add(newPair);
            }
        }

        (new ShowMessageWindowCommand()).actionPerformed(null);

        System.err.println("==============================");
        System.err.println("RESULT OF STATISTICAL COMPARISON (resampling method)");
        System.err.println("Dataset 1: " + dir1.getTitle());
        System.err.println("Dataset 2: " + dir2.getTitle());
        System.err.println("Results ranked by score:");
        System.err.println("(Score >1: significantly higher count in dataset1, < -1: in dataset2)");

        for (Pair<String, Double> p : list) {
            System.err.println(p.getFirst() + ": " + (float) p.getSecond().doubleValue());
        }
        System.err.println("=============================");
    }


    public boolean getOptionUseUnassigned() {
        return optionUseUnassigned;
    }

    public void setOptionUseUnassigned(boolean optionUseUnassigned) {
        this.optionUseUnassigned = optionUseUnassigned;
    }

    public boolean getOptionUseInternal() {
        return optionUseInternal;
    }

    public void setOptionUseInternal(boolean optionUseInternal) {
        this.optionUseInternal = optionUseInternal;
    }
}
