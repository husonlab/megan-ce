/*
 *  Copyright (C) 2015 Daniel H. Huson
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

package megan.fxdialogs.matchlayout;

import javafx.beans.property.*;

/**
 * match item to display in table
 * Created by huson on 2/11/17.
 */
public class MatchItem {
    public MatchItem(int taxonId, String taxonName, Float totalBitScore, Float disjointBitScore, Float maxScore, int hits) {
        setTaxonId(taxonId);
        if (taxonId > 0 && taxonName != null && taxonName.trim().length() > 0)
            setTaxonName(taxonName);
        else
            setTaxonName("Unknown");
        setHits(hits);
        if (totalBitScore != null)
            setTotalBitScore(totalBitScore);
        if (disjointBitScore != null)
            setDisjointBitScore(disjointBitScore);
        if (maxScore != null)
            setMaxBitScore(maxScore);
    }

    private IntegerProperty taxonId;

    public void setTaxonId(Integer value) {
        taxonIdProperty().set(value);
    }

    public Integer getTaxonId() {
        return taxonIdProperty().get();
    }

    public IntegerProperty taxonIdProperty() {
        if (taxonId == null) taxonId = new SimpleIntegerProperty(this, "taxonId");
        return taxonId;
    }

    private StringProperty taxonName;

    public void setTaxonName(String value) {
        taxonNameProperty().set(value);
    }

    public String getTaxonName() {
        return taxonNameProperty().get();
    }

    public StringProperty taxonNameProperty() {
        if (taxonName == null) taxonName = new SimpleStringProperty(this, "taxonName");
        return taxonName;
    }

    private IntegerProperty hits;

    public void setHits(Integer value) {
        hitsProperty().set(value);
    }

    public Integer getHits() {
        return hitsProperty().get();
    }

    public IntegerProperty hitsProperty() {
        if (hits == null) hits = new SimpleIntegerProperty(this, "hits");
        return hits;
    }

    private FloatProperty totalBitScore;

    public void setTotalBitScore(Float value) {
        totalBitScoreProperty().set(value);
    }

    public Float getTotalBitScore() {
        return totalBitScoreProperty().get();
    }

    public FloatProperty totalBitScoreProperty() {
        if (totalBitScore == null) totalBitScore = new SimpleFloatProperty(this, "totalBitScore");
        return totalBitScore;
    }

    private FloatProperty disjointBitScore;

    public void setDisjointBitScore(Float value) {
        disjointBitScoreProperty().set(value);
    }

    public Float getDisjointBitScore() {
        return disjointBitScoreProperty().get();
    }

    public FloatProperty disjointBitScoreProperty() {
        if (disjointBitScore == null) disjointBitScore = new SimpleFloatProperty(this, "disjointBitScore");
        return disjointBitScore;
    }

    private FloatProperty maxBitScore;

    public void setMaxBitScore(Float value) {
        maxBitScoreProperty().set(value);
    }

    public Float getMaxBitScore() {
        return maxBitScoreProperty().get();
    }

    public FloatProperty maxBitScoreProperty() {
        if (maxBitScore == null) maxBitScore = new SimpleFloatProperty(this, "maxBitScore");
        return maxBitScore;
    }
}
