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
package megan.rma3;

import jloda.util.Pair;
import megan.io.IInputReader;
import megan.io.IOutputWriter;

import java.io.IOException;

/**
 * Footer for matches
 * Created by huson on 5/16/14.
 */
public class MatchFooterRMA3 extends BaseRMA3 {
    private long numberOfReads;
    private long numberOfMatches;

    private int maxMatchesPerRead = 100;

    private boolean useKegg = false;
    private boolean useSeed = false;
    private boolean useCog = false;
    private boolean usePfam = false;

    private String readFormatDef = "";
    private String matchFormatDef = "";

    /**
     * constructor
     *
     * @param formatDef
     */
    public MatchFooterRMA3(String formatDef) {
        super(formatDef);
    }

    /**
     * default constructor
     */
    public MatchFooterRMA3() {
        this("TotalReads:Long TotalMatches:Long MaxMatchesPerRead:Integer UseKegg:Integer UseSeed:Integer UseCog:Integer UsePfam:Integer ReadFormat:String MatchFormat:String ");
    }

    @Override
    public void read(IInputReader reader, long startPos) throws IOException {
        reader.seek(startPos);
        setFormatDef(reader.readString());
        FormatDefinition formatDefinition = FormatDefinition.fromString(getFormatDef());
        for (Pair<String, FormatDefinition.Type> pair : formatDefinition.getList()) {
            switch (pair.getFirst()) {
                case "TotalReads":
                    setNumberOfReads(reader.readLong());
                    break;
                case "TotalMatches":
                    setNumberOfMatches(reader.readLong());
                    break;
                case "MaxMatchesPerRead":
                    setMaxMatchesPerRead(reader.readInt());
                    break;
                case "UseKegg":
                    setUseKegg(reader.readInt() != 0);
                    break;
                case "UseSeed":
                    setUseSeed(reader.readInt() != 0);
                    break;
                case "UseCog":
                    setUseCog(reader.readInt() != 0);
                    break;
                case "UsePfam":
                    setUsePfam(reader.readInt() != 0);
                    break;
                case "ReadFormat":
                    setReadFormatDef(reader.readString());
                    break;
                case "MatchFormat":
                    setMatchFormatDef(reader.readString());
                    break;
            }
        }
    }

    @Override
    public void write(IOutputWriter writer) throws IOException {
        writer.writeString(getFormatDef());
        FormatDefinition formatDefinition = FormatDefinition.fromString(getFormatDef());
        formatDefinition.startWrite();
        for (Pair<String, FormatDefinition.Type> pair : formatDefinition.getList()) {
            switch (pair.getFirst()) {
                case "TotalReads":
                    formatDefinition.write(writer, "TotalReads", getNumberOfReads());
                    break;
                case "TotalMatches":
                    formatDefinition.write(writer, "TotalMatches", getNumberOfMatches());
                    break;
                case "MaxMatchesPerRead":
                    formatDefinition.write(writer, "MaxMatchesPerRead", getMaxMatchesPerRead());
                    break;
                case "UseKegg":
                    formatDefinition.write(writer, "UseKegg", isUseKegg() ? 1 : 0);
                    break;
                case "UseSeed":
                    formatDefinition.write(writer, "UseSeed", isUseSeed() ? 1 : 0);
                    break;
                case "UseCog":
                    formatDefinition.write(writer, "UseCog", isUseCog() ? 1 : 0);
                    break;
                case "UsePfam":
                    formatDefinition.write(writer, "UsePfam", isUsePfam() ? 1 : 0);
                    break;
                case "ReadFormat":
                    formatDefinition.write(writer, "ReadFormat", getReadFormatDef());
                    break;
                case "MatchFormat":
                    formatDefinition.write(writer, "MatchFormat", getMatchFormatDef());
                    break;
            }
        }
        formatDefinition.finishWrite();
    }

    public long getNumberOfReads() {
        return numberOfReads;
    }

    public void incrementNumberOfReads() {
        numberOfReads++;
    }

    private void setNumberOfReads(long numberOfReads) {
        this.numberOfReads = numberOfReads;
    }

    public long getNumberOfMatches() {
        return numberOfMatches;
    }

    public void incrementNumberOfMatches() {
        numberOfMatches++;
    }

    private void setNumberOfMatches(long numberOfMatches) {
        this.numberOfMatches = numberOfMatches;
    }

    private int getMaxMatchesPerRead() {
        return maxMatchesPerRead;
    }

    private void setMaxMatchesPerRead(int maxMatchesPerRead) {
        this.maxMatchesPerRead = maxMatchesPerRead;
    }

    private boolean isUseKegg() {
        return useKegg;
    }

    private void setUseKegg(boolean useKegg) {
        this.useKegg = useKegg;
    }

    private boolean isUseSeed() {
        return useSeed;
    }

    private void setUseSeed(boolean useSeed) {
        this.useSeed = useSeed;
    }

    private boolean isUseCog() {
        return useCog;
    }

    private void setUseCog(boolean useCog) {
        this.useCog = useCog;
    }

    private boolean isUsePfam() {
        return usePfam;
    }

    private void setUsePfam(boolean usePfam) {
        this.usePfam = usePfam;
    }

    public String getReadFormatDef() {
        return readFormatDef;
    }

    private void setReadFormatDef(String readFormatDef) {
        this.readFormatDef = readFormatDef;
    }

    public String getMatchFormatDef() {
        return matchFormatDef;
    }

    private void setMatchFormatDef(String matchFormatDef) {
        this.matchFormatDef = matchFormatDef;
    }

}
