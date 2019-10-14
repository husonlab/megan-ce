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

import jloda.swing.window.NotificationsInSwing;
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
 * Objects of this class read in the official NCBI Microbial Attributes Table
 * (https://www.ncbi.nlm.nih.gov/genomes/lproks.cgi/) and assign these attributes
 * to taxa leaves (no inner nodes!) contained in the subtrees of Bacteria and Archeae
 * of the current dataset.
 *
 * @author drichter
 */
public class AttributeData {
    public final static String[] attributeList = {"Gram Stain", "Endospores", "Motility",
            "Pathogenic", "Salinity", "Oxygen Req", "Habitat", "Temp Range", "Genome Size", "GC Content", "Group", "Domain"};
    //    private final int DESEASE_POS = 19;
    private final Map<String, Integer> attribute2index = new HashMap<>();

    private int nrOfUnclassifiedTaxa = 0;

    private final Hashtable<String, String[]> attributes2Properties = new Hashtable<>();
    private final Hashtable<String, String[]> taxaName2AttributesRawData = new Hashtable<>(500);
    private final Hashtable<String, Hashtable<String, ArrayList<String>>> attribute2kind2taxaNames = new Hashtable<>();
    private final Hashtable<String, Hashtable<String, String>> taxaName2Attributes2Properties = new Hashtable<>();

    private static AttributeData instance = null;

    /**
     * gets an instance
     *
     * @return instance
     */
    static public AttributeData getInstance() {
        if (instance == null)
            instance = new AttributeData(ProgramProperties.get(MeganProperties.MICROBIALATTRIBUTESFILE));
        return instance;
    }

    /**
     * Constructor
     *
     * @param filename
     */
    private AttributeData(String filename) {
        try {
            System.err.println("Loading microbial attributes: ");
            loadAttributeData(filename);
            System.err.println("done (" + attribute2kind2taxaNames.size() + ")");
        } catch (IOException e) {
            Basic.caught(e);
            NotificationsInSwing.showError(MainViewer.getLastActiveFrame(), "Init failed: " + e.getMessage());
        }
        setTaxaAttributes();
        setAttributes2Properties();
        sumUpAttributeValues();
        Document.loadVersionInfo("Microbial attributes", filename);

    }


    /**
     * load the id 2 name mapping
     *
     * @param fileName
     */
    private void loadAttributeData(String fileName) throws IOException {
        InputStream ins = ResourceManager.getFileAsStream(fileName);
        loadAttributeData(ins);
        ins.close();
    }

    /**
     * Each line is read in and a field of strings is generated:
     * [0]: Project ID
     * [1]: Taxonomy ID
     * [2]: Organism Name
     * [3]: Domain
     * [4]: Group
     * [5]: Sequence Status
     * [6]: Genome Size
     * [7]: GC Content
     * [8]: Gram Stain
     * [9]: Shape
     * [10]: Arrangment
     * [11]: Endospores
     * [12]: Motility
     * [13]: Salinity
     * [14]: Oxygen Req
     * [15]: Habitat
     * [16]: Temp. range
     * [17]: Opt Temp
     * [18]: Pathogenic in
     * [19]: Disease
     * <p/>
     * Fields [8] and [11]-[16] are controlled vocabulary
     * <p/>
     * ## Columns:
     * 0 "Accession project ID"
     * 1 "Project ID"
     * 2 "Taxonomy ID"
     * 3 "Organism Name"
     * 4 "Domain" "Group"
     * 5 "Sequence Status"
     * 6 "Genome Size"
     * 7 "GC Content"
     * 8 "Gram Stain"
     * 9 "Shape"
     * 10 "Arrangment"
     * 11 "Endospores"
     * 12 "Motility"
     * 13 "Salinity"
     * 14 "Oxygen Req"
     * 15 "Habitat"
     * 16 "Temp. range"
     * 17 "Optimal temp."
     * 18 "Pathogenic in"
     * 19 "Disease"
     * 20 "Genbank accessions"
     * 21 "Refseq accessions"
     */
    private void loadAttributeData(InputStream ins) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(ins));
        String aLine;
        while ((aLine = r.readLine()) != null) {

            if (aLine.length() == 0) {
            } else if (aLine.startsWith("##")) {
                String[] fields = aLine.split("\\t");
                for (int i = 0; i < fields.length; i++) {
                    String label = fields[i].trim();
                    if (label.length() > 0) {
                        if (label.charAt(0) == '\"')
                            label = label.substring(1, label.length() - 1); // remove quotes
                        attribute2index.put(label, i);
                    }
                }

            } else {
                String[] fields = aLine.split("\\t");
                String idString = fields[attribute2index.get("Taxonomy ID")];
                if (idString != null && Basic.isInteger(idString)) {
                    Integer taxId = Basic.parseInt(idString);
                    String taxName = TaxonomyData.getName2IdMap().get(taxId);
                    if (taxName != null)
                        this.taxaName2AttributesRawData.put(taxName, fields);
                }
            }
        }
    }

    /**
     * Each attribute like 'Gram Stain' gets its properties like 'Yes', 'No',... .
     */
    private void setAttributes2Properties() {
        String[] gramAttributes = {"Positive", "Negative", "Unknown"};
        String[] endosporesAttributes = {"Yes", "No", "Unknown"};
        String[] motilityAttributes = {"Yes", "No", "Unknown"};
        String[] pathogenicAttributes = {"Yes", "No", "Unknown"};
        String[] salinityAttributes = {"Non-halophilic", "Mesophilic", "Moderate halophile", "Extreme halophile", "Unknown"};
        String[] oxygenReqAttributes = {"Aerobic", "Microaerophilic", "Facultative", "Anaerobic", "Unknown"};
        String[] habitatAttributes = {"Host-associated", "Aquatic", "Terrestrial", "Specialized", "Multiple", "Unknown"};
        String[] tempRangeAttributes = {"Cryophilic", "Psychrophilic", "Mesophilic", "Thermophilic", "Hyperthermophilic", "Unknown"};
        this.attributes2Properties.put(AttributeData.attributeList[0], gramAttributes);
        this.attributes2Properties.put(AttributeData.attributeList[1], endosporesAttributes);
        this.attributes2Properties.put(AttributeData.attributeList[2], motilityAttributes);
        this.attributes2Properties.put(AttributeData.attributeList[3], pathogenicAttributes);
        this.attributes2Properties.put(AttributeData.attributeList[4], salinityAttributes);
        this.attributes2Properties.put(AttributeData.attributeList[5], oxygenReqAttributes);
        this.attributes2Properties.put(AttributeData.attributeList[6], habitatAttributes);
        this.attributes2Properties.put(AttributeData.attributeList[7], tempRangeAttributes);
    }


    /**
     * Assigns the organism attributes to each existing taxon
     */
    private void setTaxaAttributes() {
        for (String taxname : getMicrobialTaxa()) {
            if (taxaName2AttributesRawData.containsKey(taxname)) {
                String[] attributes = this.taxaName2AttributesRawData.get(taxname);
                Hashtable<String, String> attribute2Property = new Hashtable<>(); //e.g. <Gram Stain,"+">
                if (attributes == null) {
                    for (String anAttributeList : AttributeData.attributeList) {
                        attribute2Property.put(anAttributeList, "");
                    }
                } else {
                    int GRAM_POS = 8;
                    attribute2Property.put(AttributeData.attributeList[0], attributes[GRAM_POS]);
                    int ENDOSPORES_POS = 11;
                    attribute2Property.put(AttributeData.attributeList[1], attributes[ENDOSPORES_POS]);
                    int MOTILITY_POS = 12;
                    attribute2Property.put(AttributeData.attributeList[2], attributes[MOTILITY_POS]);
                    //    private final int OPTTEMP_POS = 17;
                    //17
                    int PATHOGENIC_POS = 18;
                    attribute2Property.put(AttributeData.attributeList[3], attributes[PATHOGENIC_POS]);
                    int SALINITY_POS = 13;
                    attribute2Property.put(AttributeData.attributeList[4], attributes[SALINITY_POS]);
                    int OXYGEN_POS = 14;
                    attribute2Property.put(AttributeData.attributeList[5], attributes[OXYGEN_POS]);
                    int HABITAT_POS = 15;
                    attribute2Property.put(AttributeData.attributeList[6], attributes[HABITAT_POS]);
                    int TEMPRANGE_POS = 16;
                    attribute2Property.put(AttributeData.attributeList[7], attributes[TEMPRANGE_POS]);
                    int GENOMESIZE_POS = 6;
                    attribute2Property.put(AttributeData.attributeList[8], attributes[GENOMESIZE_POS]);
                    int GCCONTENT_POS = 7;
                    attribute2Property.put(AttributeData.attributeList[9], attributes[GCCONTENT_POS]);
                    int GROUP_POS = 4;
                    attribute2Property.put(AttributeData.attributeList[10], attributes[GROUP_POS]);
                    int KINGDOM_POS = 3;
                    attribute2Property.put(AttributeData.attributeList[11], attributes[KINGDOM_POS]);
                }
                taxaName2Attributes2Properties.put(taxname, attribute2Property);
            }
        }
    }

    public Hashtable<String, Hashtable<String, String>> getTaxaName2Attributes2Properties() {
        return taxaName2Attributes2Properties;
    }


    /**
     * Returns the index of a property of a certain attribute.
     *
     * @param attribute
     * @param prop      the property to find
     * @return index
     */
    private int getPropertyIndex(String attribute, String prop) {
        String[] propArray = this.attributes2Properties.get(attribute);
        //if a "null" property specifier is found, it is set to Unknown
        switch (prop) {
            case "":
                prop = "Unknown";
                break;
            case "Moderate halophilic":
                prop = "Moderate halophile";
                break;
            case "Extreme halophilic":
                prop = "Extreme halophile";
                break;
        }
        for (int i = 0; i < propArray.length; i++) {
            if (propArray[i].equals(prop)) {
                return i;
            }
        }
        System.err.println("getPropertyIndex: attribute not found: " + prop);
        return 0;
    }

    /**
     * @return the attributes2Properties
     */
    public final Hashtable<String, String[]> getAttributes2Properties() {
        return this.attributes2Properties;
    }

    /**
     * Returns a hastable containing all taxa names for each attribute and each property.
     * e.g. ['gram stain', 'yes', [tax1,tax2,...]]
     *
     * @return the attributes2TaxaNames
     */
    public final Hashtable<String, Hashtable<String, ArrayList<String>>> getAttribute2kind2taxaNames() {
        return this.attribute2kind2taxaNames;
    }

    /**
     * Returns all bacteria and archeae that got mapped be
     * at least one read in the taxonomy
     *
     * @return the microbialTaxa
     */
    public final Set<String> getMicrobialTaxa() {
        return taxaName2AttributesRawData.keySet();
    }

    public static ArrayList<String> getAttributeList() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList(attributeList));
        return list;
    }

    public final int getNrOfUnclassifiedTaxa() {
        return this.nrOfUnclassifiedTaxa;
    }

    public final void setNrOfUnclassifiedTaxa(int nrOfUnclassifiedTaxa) {
        this.nrOfUnclassifiedTaxa = nrOfUnclassifiedTaxa;
    }

    /**
     * Sums up the attributes2Values hashtable.
     * For each taxa in the dataset, its attributes are evaluated and
     * summed up.
     */
    private void sumUpAttributeValues() {
        Collection<String> microbialTaxa = taxaName2AttributesRawData.keySet();
        for (String taxname : microbialTaxa) {
            if (!taxaName2Attributes2Properties.containsKey(taxname)) {
                continue;
            }
            Hashtable<String, String> taxonAttributes = taxaName2Attributes2Properties.get(taxname);
            if (taxonAttributes.size() > 0) {
                for (String attribute : AttributeData.attributeList) {
                    String parsed = taxonAttributes.get(attribute);
                    switch (attribute) {
                        case "Gram Stain":
                        case "Endospores":
                        case "Motility":
                            switch (parsed) {
                                case "+":
                                case "Yes":
                                    this.updateAttributes2TaxaNames(attribute, 0, taxname);
                                    break;
                                case "-":
                                case "No":
                                    updateAttributes2TaxaNames(attribute, 1, taxname);
                                    break;
                                case "":  //unknown
                                    this.updateAttributes2TaxaNames(attribute, 2, taxname);
                                    break;
                            }
                            break;
                        case "Pathogenic":
                            if (parsed.equals("")) { //Unknown
                                this.updateAttributes2TaxaNames(attribute, 2, taxname);
                            } else if (parsed.equals("No")) {
                                this.updateAttributes2TaxaNames(attribute, 1, taxname);
                            } else {
                                this.updateAttributes2TaxaNames(attribute, 0, taxname);
                            }
                            break;
                        default:
                            int propertyIndex = this.getPropertyIndex(attribute, parsed);
                            this.updateAttributes2TaxaNames(attribute, propertyIndex, taxname);
                            break;
                    }
                }
            }
        }
    }

    /**
     * For each attribute and for its properties, the taxa names are stored in a hashtable.
     *
     * @param attribute
     * @param propertyIndex
     * @param taxname
     */
    private void updateAttributes2TaxaNames(String attribute, int propertyIndex, String taxname) {
        String property = this.attributes2Properties.get(attribute)[propertyIndex];
        if (this.attribute2kind2taxaNames.containsKey(attribute)) {
            Hashtable<String, ArrayList<String>> property2TaxaNames = this.attribute2kind2taxaNames.get(attribute);
            if (property2TaxaNames.containsKey(property)) {
                ArrayList<String> propertyList = property2TaxaNames.get(property);
                propertyList.add(taxname);
            } else {
                ArrayList<String> l = new ArrayList<>();
                l.add(taxname);
                property2TaxaNames.put(property, l);
            }
        } else {
            Hashtable<String, ArrayList<String>> property2TaxaNames = new Hashtable<>();
            ArrayList<String> l = new ArrayList<>();
            l.add(taxname);
            property2TaxaNames.put(property, l);
            this.attribute2kind2taxaNames.put(attribute, property2TaxaNames);
        }
    }

    /**
     * give an attribute 2 series 2 values mapping for taxa, computes one for microbial attributes
     *
     * @param attribute2series2valueForTaxa
     * @param attribute2series2value
     * @param orderedLabels
     */
    public static void getAttributes2DataSet2Values(Map<String, Map<String, Number>> attribute2series2valueForTaxa, Map<String, Map<String, Number>> attribute2series2value, List<String> orderedLabels) {
        getInstance().computeAttributes2DataSet2Values(attribute2series2valueForTaxa, attribute2series2value, orderedLabels);
    }

    /**
     * give an attribute 2 series 2 values mapping for taxa, computes one for microbial attributes tree window
     *
     * @param attribute2series2valueForTaxa
     * @param attribute2taxa2value
     */
    public static void getAttributes2Taxa2Values(Map<String, Map<String, Number>> attribute2series2valueForTaxa,
                                                 Map<String, Map<String, Number>> attribute2taxa2value) {
        getInstance().computeAttributes2Taxa2Values(attribute2series2valueForTaxa, attribute2taxa2value);
    }

    /**
     * give an attribute 2 series 2 values mapping for taxa, computes one for microbial attributes
     *
     * @param attribute2series2valueForTaxa
     * @param attribute2series2value
     * @param orderedLabels
     */
    private void computeAttributes2DataSet2Values(Map<String, Map<String, Number>> attribute2series2valueForTaxa, Map<String, Map<String, Number>> attribute2series2value, List<String> orderedLabels) {
        orderedLabels.clear();
        attribute2series2value.clear();

        // figure out which datasets are present
        Set<String> datasets = new HashSet<>();
        for (String label : attribute2series2valueForTaxa.keySet()) {
            Map<String, Number> series2value = attribute2series2valueForTaxa.get(label);
            datasets.addAll(series2value.keySet());
        }

        for (String attribute : attribute2kind2taxaNames.keySet()) {
            Map<String, ArrayList<String>> kind2taxaNames = attribute2kind2taxaNames.get(attribute);
            for (String kind : kind2taxaNames.keySet()) {
                String attributeKindPair = attribute + ":" + kind;
                orderedLabels.add(attributeKindPair);

                Map<String, Number> series2value = new TreeMap<>();
                for (String dataset : datasets)
                    series2value.put(dataset, 0);
                attribute2series2value.put(attributeKindPair, series2value);

                List<String> taxonNames = kind2taxaNames.get(kind);
                for (String taxon : taxonNames) {
                    Map<String, Number> series2valuesForTaxa = attribute2series2valueForTaxa.get(taxon);
                    if (series2valuesForTaxa != null) {
                        for (String dataset : series2valuesForTaxa.keySet()) {
                            int sum = series2valuesForTaxa.get(dataset).intValue();
                            Number value = series2value.get(dataset);
                            if (value != null)
                                sum += value.intValue();
                            series2value.put(dataset, sum);
                        }
                    }
                }
            }
        }
    }

    /**
     * give an attribute 2 series 2 values mapping for taxa, computes one for microbial attributes tree window
     *
     * @param taxa2dataset2value
     * @param attribute2taxa2value
     */
    private void computeAttributes2Taxa2Values(Map<String, Map<String, Number>> taxa2dataset2value,
                                               Map<String, Map<String, Number>> attribute2taxa2value) {
        attribute2taxa2value.clear();

        for (String attribute : attribute2kind2taxaNames.keySet()) {
            Map<String, ArrayList<String>> kind2taxaNames = attribute2kind2taxaNames.get(attribute);
            for (String kind : kind2taxaNames.keySet()) {
                String attributeKindPair = attribute + ":" + kind;

                List<String> taxonNames = kind2taxaNames.get(kind);
                for (String taxonName : taxonNames) {
                    Map<String, Number> dataset2value = taxa2dataset2value.get(taxonName);
                    if (dataset2value != null) {
                        int sum = 0;
                        for (String dataset : dataset2value.keySet()) {
                            if (dataset2value.get(dataset) != null)
                                sum += dataset2value.get(dataset).intValue();
                        }
                        if (sum > 0) {
                            Map<String, Number> taxa2value;
                            if (attribute2taxa2value.get(attributeKindPair) != null) {
                                taxa2value = attribute2taxa2value.get(attributeKindPair);
                            } else {
                                taxa2value = new TreeMap<>();
                                attribute2taxa2value.put(attributeKindPair, taxa2value);
                            }
                            taxa2value.put(taxonName, sum);
                        }
                    }
                }
            }
        }
    }
}
