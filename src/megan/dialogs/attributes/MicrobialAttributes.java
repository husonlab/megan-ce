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
package megan.dialogs.attributes;

import jloda.swing.util.ResourceManager;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.core.Document;
import megan.main.MeganProperties;
import megan.viewer.MainViewer;
import megan.viewer.TaxonomyData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * parses and loads microbial attributes
 * Daniel Huson, 7.2012
 */
public class MicrobialAttributes {
    private final String[] knownAttributes = new String[]{"Gram Stain", "Shape", "Arrangment", "Endospores", "Motility",
            "Salinity", "Oxygen Req", "Habitat", "Temp. range", "Optimal temp.", "Pathogenic in", "Disease"};

    private final Map<String, Set<Integer>> attributeAndState2taxa = new HashMap<>();
    private final Set<String> attributes = new HashSet<>();
    private final Map<String, Set<String>> attribute2states = new HashMap<>();
    private final Map<Integer, Set<String>> tax2attributeAndState = new HashMap<>();
    private static MicrobialAttributes instance;

    /**
     * constructor
     */
    private MicrobialAttributes() {

    }

    /**
     * get the instance
     *
     * @return instance
     */
    public static MicrobialAttributes getInstance() throws IOException {
        if (instance == null) {
            instance = new MicrobialAttributes();
            String fileName = ProgramProperties.get(MeganProperties.MICROBIALATTRIBUTESFILE);
            try (InputStream ins = ResourceManager.getFileAsStream(fileName)) {
                instance.readData(ins);
            } catch (Exception ex) {
                Basic.caught(ex);
                instance = null;
            }
            Document.loadVersionInfo("Microbial attributes", fileName);
        }
        return instance;
    }

    /**
     * read the attribute data from a stream
     *
     * @param ins
     * @throws IOException
     */
    private void readData(InputStream ins) throws IOException {
        Set<String> attributesOfInterest = new HashSet<>(Arrays.asList(knownAttributes));

        Map<String, Integer> attribute2column = new HashMap<>();
        Map<Integer, String> column2attribute = new HashMap<>();

        BufferedReader r = new BufferedReader(new InputStreamReader(ins));
        String aLine;
        int numberOfTokens = 0;
        while ((aLine = r.readLine()) != null) {
            if (aLine.length() > 0) {
                if (aLine.startsWith("## Columns:")) {
                    String[] tokens = aLine.split("\t");
                    for (int i = 1; i < tokens.length; i++) {
                        String label = tokens[i];
                        if (label.startsWith("\""))
                            label = label.substring(1, tokens[i].length() - 1); // stri[
                        attribute2column.put(label, i - 1); // first token in line is not a column header...
                        if (attributesOfInterest.contains(label)) {
                            column2attribute.put(i - 1, label);
                            attributes.add(label);
                        }
                    }
                    numberOfTokens = tokens.length - 1;
                } else if (!aLine.startsWith("#")) {
                    String[] tokens = aLine.split("\t");
                    if (tokens.length < numberOfTokens)
                        System.err.println("Too few tokens in line: " + aLine);
                    else if (tokens.length > numberOfTokens)
                        System.err.println("Too many tokens in line: " + aLine);
                    else {
                        Integer taxonId = Integer.parseInt(tokens[attribute2column.get("Taxonomy ID")]);
                        for (int i = 0; i < tokens.length; i++) {
                            if (attributesOfInterest.contains(column2attribute.get(i))) {
                                String state = Basic.capitalizeWords(tokens[i].trim());
                                if (state.length() == 0 || (state.equals("-") && i != attribute2column.get("Gram Stain")))
                                    state = "Unknown";
                                if ((state.equals("_") && i == attribute2column.get("Gram Stain")))
                                    state = "-";
                                if (state.equalsIgnoreCase("n"))
                                    state = "No";
                                Set<String> states = attribute2states.computeIfAbsent(column2attribute.get(i), k -> new HashSet<>());
                                states.add(state);
                                String key = column2attribute.get(i) + ":" + state;
                                Set<Integer> taxa = attributeAndState2taxa.computeIfAbsent(key, k -> new HashSet<>());
                                Set<String> attributeAndStates = tax2attributeAndState.computeIfAbsent(taxonId, k -> new HashSet<>());
                                attributeAndStates.add(key);
                                taxa.add(taxonId);
                            }
                        }
                    }
                }
            }
        }
        cleanStates();
    }

    /**
     * this tries to fix all the misspellings etc in the microbial attributes table
     */
    private void cleanStates() {
        System.err.println("Cleaning microbial attributes file:");
        for (String attribute : attribute2states.keySet()) {
            String[] originalStates = attribute2states.get(attribute).toArray(new String[0]);
            String[] states = new String[originalStates.length];
            for (int i = 0; i < states.length; i++) {
                states[i] = originalStates[i].toLowerCase().replaceAll(":", ": ").replaceAll(",", "").replaceAll(" or ", " ")
                        .replaceAll(" and ", " ").replaceAll("-", " ");
                if (states[i].endsWith("s"))
                    states[i] = states[i].substring(0, states[i].length() - 1);
                if (attribute.equals("Endospores") && states[i].equals("n"))
                    states[i] = "no";
                states[i] = states[i].trim();
            }

            int[] mapTo = new int[states.length];
            for (int i = 0; i < mapTo.length; i++)
                mapTo[i] = i;

            for (int i = 0; i < states.length; i++) {
                Set<String> iSet = new HashSet<>(Arrays.asList(states[i].split(" ")));

                for (int j = i + 1; j < states.length; j++) {
                    boolean same = false;

                    Set<String> jSet = new HashSet<>(Arrays.asList(states[j].split(" ")));
                    if (iSet.equals(jSet))
                        same = true;

                    if (same) {
                        System.err.println(attribute + ": '" + originalStates[i] + "' == '" + originalStates[j] + "'");
                        for (int k = 0; k < mapTo.length; k++) {
                            if (mapTo[k] == j)
                                mapTo[k] = i;
                        }
                    }
                }
            }
            String[] bestStates = new String[originalStates.length];
            for (int i = 0; i < mapTo.length; i++) {
                int j = mapTo[i];
                if (j <= i) {
                    String newBestState = Basic.capitalizeWords(originalStates[i]).replaceAll(" In ", " in ").replaceAll(" And ", " and ").replaceAll(" Or ", " or ");
                    if (newBestState.endsWith(",") || newBestState.endsWith("."))
                        newBestState = newBestState.substring(0, newBestState.length() - 1);
                    if (bestStates[j] == null || newBestState.length() < bestStates[j].length() || newBestState.compareTo(bestStates[j]) < 0)
                        bestStates[j] = newBestState;
                }
            }
            for (int i = 0; i < mapTo.length; i++) {
                int j = mapTo[i];
                if (j <= i) {
                    attribute2states.get(attribute).remove(originalStates[i]);
                    if (i == j) {
                        attribute2states.get(attribute).add(bestStates[j]);
                    }
                    String oldKey = attribute + ":" + originalStates[i];
                    String newKey = attribute + ":" + bestStates[j];
                    Set<Integer> taxa = attributeAndState2taxa.get(oldKey);
                    for (Integer t : taxa) {
                        tax2attributeAndState.get(t).remove(oldKey);
                        tax2attributeAndState.get(t).add(newKey);
                    }
                    attributeAndState2taxa.keySet().remove(oldKey);
                    attributeAndState2taxa.put(newKey, taxa);
                }
            }
        }
        System.err.println("Done");
    }

    public Set<String> getAttributes() {
        return attributes;
    }

    public Set<String> getStates(String attribute) {
        return attribute2states.get(attribute);
    }

    public Map<String, Set<Integer>> getAttributeAndState2taxa() {
        return attributeAndState2taxa;
    }

    private Set<String> getAttributesAndStates(Integer taxonId) {
        return tax2attributeAndState.get(taxonId);
    }

    /**
     * compute the values for attribute-state pairs
     *
     * @param mainViewer
     * @return dataset to attribute-states to values
     */
    public Map<String, Map<String, Integer>> getDataSet2AttributeState2Value(MainViewer mainViewer) {
        Document doc = mainViewer.getDir().getDocument();
        Map<String, Map<String, Number>> tax2series2value = new HashMap<>();
        doc.getTaxonName2DataSet2SummaryCount(tax2series2value);

        Map<String, Map<String, Integer>> dataSet2AttributeState2Value = new HashMap<>();

        for (String taxonName : tax2series2value.keySet()) {
            int taxonId = TaxonomyData.getName2IdMap().get(taxonName);
            if (taxonId != 0) {
                Set<String> attributesAndStates = getAttributesAndStates(taxonId);
                if (attributesAndStates != null) {
                    String taxName = TaxonomyData.getName2IdMap().get(taxonId);
                    if (taxName != null) {
                        Map<String, Number> series2value = tax2series2value.get(taxName);
                        for (String series : series2value.keySet()) {
                            if (series2value.get(series) != null) {
                                Map<String, Integer> attributeState2value = dataSet2AttributeState2Value.computeIfAbsent(series, k -> new HashMap<>());
                                for (String key : attributesAndStates) {
                                    Integer value = attributeState2value.get(key);
                                    if (value == null)
                                        attributeState2value.put(key, series2value.get(series).intValue());
                                    else
                                        attributeState2value.put(key, value + series2value.get(series).intValue());
                                }
                            }
                        }
                    }
                }
            }
        }
        return dataSet2AttributeState2Value;
    }
}
