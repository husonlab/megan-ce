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
package megan.classification;

import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.classification.util.MultiWords;
import megan.classification.util.TaggedValueIterator;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * this class does the work of parsing ids based on the given idMapper
 * Daniel Huson, 4.2015, 3.2016
 */
public class IdParser {
    public static final String[] ACCESSION_TAGS = new String[]{"gb|", "ref|"};
    public static final String REFSEQ_TAG = "ref|";

    public static final String PROPERTIES_FIRST_WORD_IS_ACCESSION = "FirstWordIsAccession";
    public static final String PROPERTIES_ACCESSION_TAGS = "AccessionTags";

    public enum Algorithm {First_Hit, Majority, LCA}

    private final IdMapper idMapper;
    private boolean useTextParsing;
    private Algorithm algorithm = Algorithm.First_Hit;
    private final Set<Integer> disabledIds = new HashSet<>();

    private final Map<Integer, Integer> id2count = new HashMap<>();
    private final Set<Integer> ids = new HashSet<>();
    private final Set<Integer> disabled = new HashSet<>();

    private final boolean isTaxonomy;

    private final MultiWords multiWords;
    private final Map<Integer, int[]> id2segment = new HashMap<>();

    private final TaggedValueIterator taggedIds;
    private final TaggedValueIterator accTaggedIds;

    private int maxWarnings = 10;

    /**
     * constructor
     *
     * @param idMapper
     */
    public IdParser(IdMapper idMapper) {
        this.idMapper = idMapper;
        this.useTextParsing = idMapper.isUseTextParsing();
        disabledIds.addAll(idMapper.getDisabledIds());
        isTaxonomy = idMapper.getCName().equals(Classification.Taxonomy);

        multiWords = new MultiWords();

        final boolean accessionOrDB=(idMapper.isActiveMap(IdMapper.MapType.Accession) && idMapper.isLoaded(IdMapper.MapType.Accession))||(idMapper.isActiveMap(IdMapper.MapType.MeganMapDB) && idMapper.isLoaded(IdMapper.MapType.MeganMapDB));

        taggedIds = new TaggedValueIterator(false, true, idMapper.getIdTags());
        accTaggedIds = new TaggedValueIterator(ProgramProperties.get(PROPERTIES_FIRST_WORD_IS_ACCESSION, true), accessionOrDB, ProgramProperties.get(PROPERTIES_ACCESSION_TAGS, ACCESSION_TAGS));
    }

    /**
     * Attempt to determine Id from header line.
     *
     * @param headerString
     * @return ID or 0
     */
    public int getIdFromHeaderLine(String headerString) throws IOException {
        if (headerString == null)
            return 0;

        ids.clear();
        disabled.clear();

        headerString = headerString.trim();

        // look for ID tag:
        if (taggedIds.isEnabled()) {
            taggedIds.restart(headerString);
            for (String label : taggedIds) {
                try {
                    int id = Integer.parseInt(label);
                    if (id != 0) {
                        if (disabledIds.contains(id))
                            disabled.add(id);
                        else {
                            switch (algorithm) {
                                default:
                                case First_Hit:
                                    return id;
                                case LCA:
                                    ids.add(id);
                                    break;
                                case Majority:
                                    final Integer count = id2count.get(id);
                                    id2count.put(id, count == null ? 1 : count + 1);
                                    break;
                            }
                        }
                    }
                } catch (NumberFormatException ex) {
                    if (maxWarnings > 0) {
                        System.err.println("parseInt() failed: " + label);
                        maxWarnings--;
                    }
                }
            }
        }

        // if synonyms file given, try to find occurrence of synonym

        if (idMapper.isActiveMap(IdMapper.MapType.Synonyms) && idMapper.isLoaded(IdMapper.MapType.Synonyms)) {
            final int countLabels = multiWords.compute(headerString);
            for (int i = 0; i < countLabels; i++) {
                final String label = multiWords.getWord(i);
                Integer id = idMapper.getSynonymsMap().get(label);
                if (id != null && id != 0) {
                    if (disabledIds.contains(id))
                        disabled.add(id);
                    else {
                        switch (algorithm) {
                            default:
                            case First_Hit:
                                return id;
                            case LCA:
                                ids.add(id);
                                break;
                            case Majority:
                                final Integer count = id2count.get(id);
                                id2count.put(id, count == null ? 1 : count + 1);
                                break;
                        }
                    }
                }
            }
        }

        // Look for accession mapping
        if (accTaggedIds.isEnabled()) {
            accTaggedIds.restart(headerString);

            for (String label : accTaggedIds) {
                final int id = idMapper.getAccessionMap().get(label);
                if (id > 0) {
                    if (disabledIds.contains(id))
                        disabled.add(id);
                    else {
                        switch (algorithm) {
                            default:
                            case First_Hit:
                                return id;
                            case LCA:
                                ids.add(id);
                                break;
                            case Majority:
                                final Integer count = id2count.get(id);
                                id2count.put(id, count == null ? 1 : count + 1);
                                break;
                        }
                    }
                }
            }
        }

        // if text parsing is allowed and no ids have been found yet:
        if (isTaxonomy && useTextParsing && ids.size() == 0 && disabled.size() == 0) {
            // parse taxonomic path in which taxa are separated by ;
            final int countSemiColons = Basic.countOccurrences(headerString, ';');
            if (countSemiColons > 0 && countSemiColons >= (headerString.length() / 32)) // assume is taxonomy path e.g. Bacteria;Proteobacteria;
            {
                // find last legal name
                int taxId = 0;
                String[] tokens = headerString.split(";");
                for (String token : tokens) {
                    token = token.trim();
                    final int newTaxId = idMapper.getName2IdMap().get(token);
                    if (newTaxId != 0 && (taxId == 0 || idMapper.fullTree.isDescendant(taxId, newTaxId))) {
                        taxId = newTaxId;
                    }
                }
                if (taxId != 0)
                    return taxId;
            }

            for (int left = headerString.indexOf('['); left != -1; left = headerString.indexOf('[', left + 1)) {
                try {
                    Integer id = null;
                    final int right = headerString.indexOf(']', left + 1);
                    if (right > left) {
                        if (idMapper.isActiveMap(IdMapper.MapType.Synonyms) && idMapper.isLoaded(IdMapper.MapType.Synonyms)) {
                            id = idMapper.getSynonymsMap().get(headerString.substring(left + 1, right));
                        }
                        if (id == null) {
                            id = idMapper.getName2IdMap().get(headerString.substring(left + 1, right));
                        }
                        if (id != 0) {
                            if (disabledIds.contains(id))
                                disabled.add(id);
                            else {
                                switch (algorithm) {
                                    default:
                                    case First_Hit:
                                        return id;
                                    case LCA:
                                        ids.add(id);
                                        break;
                                    case Majority:
                                        final Integer count = id2count.get(id);
                                        id2count.put(id, count == null ? 1 : count + 1);
                                        break;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            // Name simply as text
            {
                id2segment.clear();
                int countLabels = multiWords.compute(headerString, 5, 120);
                for (int i = 0; i < countLabels; i++) {
                    final String label = multiWords.getWord(i);
                    final int id = idMapper.getName2IdMap().get(label);
                    if (id != 0 && !id2segment.containsKey(id)) {
                        boolean overlaps = false;
                        int[] pair = multiWords.getPair(i);
                        for (int[] previousPair : id2segment.values()) { // make sure this doesn't overlap some other segment already used for an id
                            if (pair[0] >= previousPair[0] && pair[0] <= previousPair[1] || pair[1] >= previousPair[0] && pair[1] <= previousPair[1]) {
                                overlaps = true;
                                break;
                            }
                        }
                        if (!overlaps) {
                            if (disabledIds.contains(id))
                                disabled.add(id);
                            else {
                                switch (algorithm) {
                                    default:
                                    case First_Hit:
                                        return id;
                                    case LCA:
                                        ids.add(id);
                                        break;
                                    case Majority:
                                        final Integer count = id2count.get(id);
                                        id2count.put(id, count == null ? 1 : count + 1);
                                        break;
                                }
                            }
                            id2segment.put(id, pair);
                        }
                    }
                }
            }
        }

        // compute final result:
        if (ids.size() == 1)
            return ids.iterator().next();
        if (ids.size() > 0) { // must be algorithm==LCA
            return idMapper.fullTree.getLCA(ids);
        } else if (id2count.size() > 0) { // must be algorithm=Majority
            int bestId = 0;
            int bestCount = 0;
            for (Integer id : id2count.keySet()) {
                Integer count = id2count.get(id);
                if (count > bestCount) {
                    bestCount = count;
                    bestId = id;
                }
            }
            return bestId;
        } else if (disabled.size() > 0) { // only have disabled ids, return one of them
            if (disabled.size() == 1)
                return disabled.iterator().next();
            switch (algorithm) {
                default:
                case Majority:
                case First_Hit:
                    return disabled.iterator().next();
                case LCA:
                    return idMapper.fullTree.getLCA(disabled);
            }
        }
        return 0;
    }

    /**
     * get the name of the classification
     *
     * @return name
     */
    public String getCName() {
        return idMapper.getCName();
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    public boolean isUseTextParsing() {
        return useTextParsing;
    }

    public void setUseTextParsing(boolean useTextParsing) {
        this.useTextParsing = useTextParsing;
    }

}
