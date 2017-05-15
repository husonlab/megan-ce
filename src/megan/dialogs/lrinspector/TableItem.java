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

package megan.dialogs.lrinspector;

import javafx.beans.property.*;

/**
 * table item
 * Daniel Huson, 2/2017
 */
public class TableItem {
    private StringProperty readName;
    private StringProperty readSequence;
    private IntegerProperty readLength;
    private IntegerProperty classId;
    private StringProperty className;
    private IntegerProperty hits;
    private IntegerProperty percentCoverage;
    private IntegerProperty score;
    private IntegerProperty maxBitScore;
    private ObjectProperty<ReadLayoutPane> pane;

    /**
     * constructor
     *
     * @param readName
     * @param classId
     * @param className
     * @param score
     * @param maxScore
     * @param hits
     * @param percentCoverage
     * @param pane
     */
    public TableItem(String readName, String readSequence, String className, int classId, float score, float maxScore, int hits, int percentCoverage, ReadLayoutPane pane) {
        setReadName(readName);
        setReadSequence(readSequence);
        if (readSequence != null)
            setReadLength(readSequence.length());
        setClassId(classId);
        if (className == null)
            className = "Unknown";
        setClassName(className);
        setHits(hits);
        setScore(Math.round(score));
        setMaxBitScore(Math.round(maxScore));
        setPane(pane);
        setPercentCoverage(percentCoverage);
    }

    public void setReadName(String value) {
        readNameProperty().set(value);
    }

    public String getReadName() {
        return readNameProperty().get();
    }

    public StringProperty readNameProperty() {
        if (readName == null) readName = new SimpleStringProperty(this, "readName");
        return readName;
    }

    public void setReadSequence(String value) {
        readSequenceProperty().set(value);
    }

    public String getReadSequence() {
        return readSequenceProperty().get();
    }

    public StringProperty readSequenceProperty() {
        if (readSequence == null) readSequence = new SimpleStringProperty(this, "readSequence");
        return readSequence;
    }

    public void setReadLength(Integer value) {
        readLengthProperty().set(value);
    }

    public Integer getReadLength() {
        return readLengthProperty().get();
    }

    public IntegerProperty readLengthProperty() {
        if (readLength == null) readLength = new SimpleIntegerProperty(this, "readLength");
        return readLength;
    }


    public void setClassId(Integer value) {
        classIdProperty().set(value);
    }

    public Integer getClassId() {
        return classIdProperty().get();
    }

    public IntegerProperty classIdProperty() {
        if (classId == null) classId = new SimpleIntegerProperty(this, "classId");
        return classId;
    }

    public void setClassName(String value) {
        classNameProperty().set(value);
    }

    public String getClassName() {
        return classNameProperty().get();
    }

    public StringProperty classNameProperty() {
        if (className == null) className = new SimpleStringProperty(this, "className");
        return className;
    }

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

    public void setPercentCoverage(Integer value) {
        percentCoverageProperty().set(value);
    }

    public Integer getPercentCoverage() {
        return percentCoverageProperty().get();
    }

    public IntegerProperty percentCoverageProperty() {
        if (percentCoverage == null) percentCoverage = new SimpleIntegerProperty(this, "percentCoverage");
        return percentCoverage;
    }

    public void setScore(Integer value) {
        scoreProperty().set(value);
    }

    public Integer getScore() {
        return scoreProperty().get();
    }

    public IntegerProperty scoreProperty() {
        if (score == null) score = new SimpleIntegerProperty(this, "score");
        return score;
    }


    public void setMaxBitScore(Integer value) {
        maxBitScoreProperty().set(value);
    }

    public Integer getMaxBitScore() {
        return maxBitScoreProperty().get();
    }

    public IntegerProperty maxBitScoreProperty() {
        if (maxBitScore == null) maxBitScore = new SimpleIntegerProperty(this, "maxBitScore");
        return maxBitScore;
    }

    public ReadLayoutPane getPane() {
        return paneProperty().get();
    }

    public ObjectProperty<ReadLayoutPane> paneProperty() {
        if (pane == null) pane = new SimpleObjectProperty<>(this, "pane");
        return pane;
    }

    public void setPane(ReadLayoutPane pane) {
        paneProperty().set(pane);
    }

    public String toString() {
        return getReadName() + "\tlength=" + getReadLength() + "\tassignment=" + getClassName() + "\talignments=" + getHits() + "\tpercentCovered=" + getPercentCoverage() + "\ttotalScore=" + getScore() + "\tmaxScore=" + +getMaxBitScore();
    }
}
