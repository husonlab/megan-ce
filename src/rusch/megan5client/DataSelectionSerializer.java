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
package rusch.megan5client;

import megan.data.DataSelection;
import megan.data.FindSelection;
import rusch.megan5client.connector.Megan5ServerConnector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Make {@link megan.data.DataSelection} and {@link FindSelection} readable for REST
 *
 * @author Hans-Joachim Ruscheweyh
 * 5:36:04 PM - Oct 28, 2014
 */
public class DataSelectionSerializer {


    /**
     * serialize data selection
     */
    private static List<String> serializeDataSelection(DataSelection dataSelection) {
        List<String> datasel = new ArrayList<>();

        if (dataSelection.isWantReadText()) {
            datasel.add("useRead");
        }
        if (dataSelection.isWantReadText()) {
            datasel.add("useReadName");
        }
        if (dataSelection.isWantReadText()) {
            datasel.add("useReadHeader");
        }
        if (dataSelection.isWantReadText()) {
            datasel.add("useReadSequence");
        }
        if (dataSelection.isWantReadText()) {
            datasel.add("useMateUId");
        }
        if (dataSelection.isWantReadText()) {
            datasel.add("useReadLength");
        }
        if (dataSelection.isWantMatches()) {
            datasel.add("useReadComplexity");
        }
        if (dataSelection.isWantReadText()) {
            datasel.add("useReadOriginalNumberOfMatches");
        }
        if (dataSelection.isWantMatches()) {
            datasel.add("useReadNumberOfMatches");
        }
        if (dataSelection.isWantMatches()) {
            datasel.add("useReadWeight");
        }
        if (dataSelection.isWantMatches()) {
            datasel.add("useMatchText");
        }
        if (dataSelection.isWantMatches()) {
            datasel.add("useMatchLength");
        }
        if (dataSelection.isWantMatches()) {
            datasel.add("useMatchTaxonId");
        }
        if (dataSelection.isWantMatches()) {
            datasel.add("useMatchSeedId");
        }
        if (dataSelection.isWantMatches()) {
            datasel.add("useMatchKeggId");
        }
        if (dataSelection.isWantMatches()) {
            datasel.add("useMatchCogId");
        }
        if (dataSelection.isWantMatches()) {
            datasel.add("useMatchExpected");
        }
        if (dataSelection.isWantMatches()) {
            datasel.add("useMatchPercentIdentity");
        }
        if (dataSelection.isWantMatches()) {
            datasel.add("useMatchRefSeq");
        }
        return datasel;

    }

    /**
     * serialize data selection
     */
    public static List<String> serializeDataSelection(boolean wantReadText, boolean wantMatches) {
        DataSelection dataSelection = new DataSelection();
        dataSelection.setWantReadText(wantReadText);
        dataSelection.setWantMatches(wantMatches);
        return serializeDataSelection(dataSelection);
    }


    public static DataSelection deserializeDataSelection(String[] dataSelection) {
        List<String> d = Arrays.asList(dataSelection);
        DataSelection datasel = new DataSelection();
        if (d.contains("useRead")) {
            datasel.setWantReadText(true);
        }
        if (d.contains("useReadName")) {
            datasel.setWantReadText(true);
        }
        if (d.contains("useReadHeader")) {
            datasel.setWantReadText(true);
        }
        if (d.contains("useReadSequence")) {
            datasel.setWantReadText(true);
        }
        if (d.contains("useMateUId")) {
            datasel.setWantReadText(true);
        }
        if (d.contains("useReadLength")) {
            datasel.setWantReadText(true);
        }
        if (d.contains("useReadComplexity")) {
            datasel.setWantReadText(true);
        }
        if (d.contains("useReadOriginalNumberOfMatches")) {
            datasel.setWantReadText(true);
        }
        if (d.contains("useReadNumberOfMatches")) {
            datasel.setWantReadText(true);
        }
        if (d.contains("useReadWeight")) {
            datasel.setWantReadText(true);
        }
        if (d.contains("useMatchText")) {
            datasel.setWantReadText(true);
            datasel.setWantMatches(true);
        }
        if (d.contains("useMatchIgnore")) {
            datasel.setWantReadText(true);
            datasel.setWantMatches(true);
        }
        if (d.contains("useMatchBitScore")) {
            datasel.setWantReadText(true);
            datasel.setWantMatches(true);
        }
        if (d.contains("useMatchLength")) {
            datasel.setWantReadText(true);
            datasel.setWantMatches(true);
        }
        if (d.contains("useMatchTaxonId")) {
            datasel.setWantReadText(true);
            datasel.setWantMatches(true);
        }
        if (d.contains("useMatchSeedId")) {
            datasel.setWantReadText(true);
            datasel.setWantMatches(true);
        }
        if (d.contains("useMatchKeggId")) {
            datasel.setWantReadText(true);
            datasel.setWantMatches(true);
        }
        if (d.contains("useMatchCogId")) {
            datasel.setWantReadText(true);
            datasel.setWantMatches(true);
        }
        if (d.contains("useMatchExpected")) {
            datasel.setWantReadText(true);
            datasel.setWantMatches(true);
        }
        if (d.contains("useMatchPercentIdentity")) {
            datasel.setWantReadText(true);
            datasel.setWantMatches(true);
        }
        if (d.contains("useMatchRefSeq")) {
            datasel.setWantReadText(true);
            datasel.setWantMatches(true);
        }
        return datasel;
    }

    public static FindSelection deserializeFindSelection(String[] findSelection) {
        List<String> d = Arrays.asList(findSelection);
        FindSelection datasel = new FindSelection();
        if (d.contains("useMatchText")) {
            datasel.useMatchText = true;
        }
        if (d.contains("useReadHeader")) {
            datasel.useReadHeader = true;
        }
        if (d.contains("useReadName")) {
            datasel.useReadName = true;
        }
        if (d.contains("useReadSequence")) {
            datasel.useReadSequence = true;
        }
        return datasel;
    }

    public static List<String> serializeFindSelection(FindSelection d) {
        if (d == null) {
            return new ArrayList<>();
        }
        List<String> datasel = new ArrayList<>();
        if (d.useMatchText) {
            datasel.add("useMatchText");
        }
        if (d.useReadHeader) {
            datasel.add("useReadHeader");
        }
        if (d.useReadName) {
            datasel.add("useReadName");
        }
        if (d.useReadSequence) {
            datasel.add("useReadSequence");
        }
        return datasel;
    }

    public static void main(String[] args) {
        FindSelection findSel = new FindSelection();
        findSel.useMatchText = true;
        findSel.useReadHeader = true;
        findSel.useReadName = true;
        findSel.useReadSequence = true;
        System.out.println(Megan5ServerConnector.httpArray2(serializeFindSelection(findSel)));
    }

}
