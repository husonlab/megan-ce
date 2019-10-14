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
 * fileFooter of RMA3 file
 * Created by huson on 5/16/14.
 */
public class FileFooterRMA3 extends BaseRMA3 {
    private String creator;
    private long creationDate;

    private String alignmentFile;
    private String alignmentFileFormat;
    private long alignmentFileSize;

    private String readsFile;
    private String readsFileFormat;
    private long readsFileSize;

    private String blastMode;

    private long matchesStart;
    private long matchesFooter;

    private long classificationsStart;
    private long classificationsFooter;

    private long auxStart;
    private long auxFooter;

    private long fileFooter;


    /**
     * constructor
     */
    public FileFooterRMA3() {

        super("Creator:String CreationDate:Long" +
                " AlignmentsFile:String AlignmentFileFormat:String AlignmentFileSize:Long" +
                " ReadsFile:String ReadsFileFormat:String ReadsFileSize:Long" +
                " BlastModeUtils:String" +
                " MatchesStart:Long MatchesFooter:Long" +
                " ClassificationsStart:Long ClassificationsFooter:Long" +
                " AuxStart:Long AuxFooter:Long FileFooter:Long");
    }

    /**
     * read from an RMA3 file
     *
     * @param reader
     * @throws IOException
     */
    public void read(IInputReader reader, long startPos) throws IOException {
        reader.seek(startPos);
        setFormatDef(reader.readString());
        FormatDefinition formatDefinition = FormatDefinition.fromString(getFormatDef());

        for (Pair<String, FormatDefinition.Type> pair : formatDefinition.getList()) {
            switch (pair.getFirst()) {
                case "Creator":
                    setCreator(reader.readString());
                    break;
                case "CreationDate":
                    setCreationDate(reader.readLong());
                    break;
                case "AlignmentsFile":
                    setAlignmentFile(reader.readString());
                    break;
                case "AlignmentFileFormat":
                    setAlignmentFileFormat(reader.readString());
                    break;
                case "AlignmentFileSize":
                    setAlignmentFileSize(reader.readLong());
                    break;
                case "ReadsFile":
                    setReadsFile(reader.readString());
                    break;
                case "ReadsFileFormat":
                    setReadsFileFormat(reader.readString());
                    break;
                case "ReadsFileSize":
                    setReadsFileSize(reader.readLong());
                    break;
                case "BlastModeUtils":
                    setBlastMode(reader.readString());
                    break;
                case "MatchesStart":
                    setMatchesStart(reader.readLong());
                    break;
                case "MatchesFooter":
                    setMatchesFooter(reader.readLong());
                    break;
                case "ClassificationsStart":
                    setClassificationsStart(reader.readLong());
                    break;
                case "ClassificationsFooter":
                    setClassificationsFooter(reader.readLong());
                    break;
                case "AuxStart":
                    setAuxStart(reader.readLong());
                    break;
                case "AuxFooter":
                    setAuxFooter(reader.readLong());
                    break;
                case "FileFooter":
                    setFileFooter(reader.readLong());
                    break;
            }
        }
    }

    /**
     * write to an RMA3 file
     *
     * @param writer
     * @return location of fileFooter in file
     * @throws IOException
     */
    public void write(IOutputWriter writer) throws IOException {
        writer.writeString(getFormatDef());

        final FormatDefinition formatDefinition = FormatDefinition.fromString(getFormatDef());
        formatDefinition.startWrite();
        for (Pair<String, FormatDefinition.Type> pair : formatDefinition.getList()) {
            {
                switch (pair.getFirst()) {
                    case "Creator":
                        formatDefinition.write(writer, "Creator", getCreator());
                        break;
                    case "CreationDate":
                        formatDefinition.write(writer, "CreationDate", getCreationDate());
                        break;
                    case "AlignmentsFile":
                        formatDefinition.write(writer, "AlignmentsFile", getAlignmentFile());
                        break;
                    case "AlignmentFileFormat":
                        formatDefinition.write(writer, "AlignmentFileFormat", getAlignmentFileFormat());
                        break;
                    case "AlignmentFileSize":
                        formatDefinition.write(writer, "AlignmentFileSize", getAlignmentFileSize());
                        break;
                    case "ReadsFile":
                        formatDefinition.write(writer, "ReadsFile", getReadsFile());
                        break;
                    case "ReadsFileFormat":
                        formatDefinition.write(writer, "ReadsFileFormat", getReadsFileFormat());
                        break;
                    case "ReadsFileSize":
                        formatDefinition.write(writer, "ReadsFileSize", getReadsFileSize());
                        break;
                    case "BlastModeUtils":
                        formatDefinition.write(writer, "BlastModeUtils", getBlastMode());
                        break;
                    case "MatchesStart":
                        formatDefinition.write(writer, "MatchesStart", getMatchesStart());
                        break;
                    case "MatchesFooter":
                        formatDefinition.write(writer, "MatchesFooter", getMatchesFooter());
                        break;
                    case "ClassificationsStart":
                        formatDefinition.write(writer, "ClassificationsStart", getClassificationsStart());
                        break;
                    case "ClassificationsFooter":
                        formatDefinition.write(writer, "ClassificationsFooter", getClassificationsFooter());
                        break;
                    case "AuxStart":
                        formatDefinition.write(writer, "AuxStart", getAuxStart());
                        break;
                    case "AuxFooter":
                        formatDefinition.write(writer, "AuxFooter", getAuxFooter());
                        break;
                    case "FileFooter":
                        formatDefinition.write(writer, "FileFooter", getFileFooter());
                        break;
                }
            }
        }
        formatDefinition.finishWrite();
    }

    private String getCreator() {
        return creator;
    }

    private void setCreator(String creator) {
        this.creator = creator;
    }

    private long getCreationDate() {
        return creationDate;
    }

    private void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public String getAlignmentFile() {
        return alignmentFile;
    }

    public void setAlignmentFile(String alignmentFile) {
        this.alignmentFile = alignmentFile;
    }

    public String getAlignmentFileFormat() {
        return alignmentFileFormat;
    }

    private void setAlignmentFileFormat(String alignmentFileFormat) {
        this.alignmentFileFormat = alignmentFileFormat;
    }

    public String getBlastMode() {
        return blastMode;
    }

    private void setBlastMode(String blastMode) {
        this.blastMode = blastMode;
    }

    public long getAlignmentFileSize() {
        return alignmentFileSize;
    }

    private void setAlignmentFileSize(long alignmentFileSize) {
        this.alignmentFileSize = alignmentFileSize;
    }

    public String getReadsFile() {
        return readsFile;
    }

    public void setReadsFile(String readsFile) {
        this.readsFile = readsFile;
    }

    public String getReadsFileFormat() {
        return readsFileFormat;
    }

    private void setReadsFileFormat(String readsFileFormat) {
        this.readsFileFormat = readsFileFormat;
    }

    public long getReadsFileSize() {
        return readsFileSize;
    }

    private void setReadsFileSize(long readsFileSize) {
        this.readsFileSize = readsFileSize;
    }

    public long getEndMatches() {
        return getMatchesFooter();
    }

    public long getFileFooter() {
        return fileFooter;
    }

    public void setFileFooter(long fileFooter) {
        this.fileFooter = fileFooter;
    }

    public long getMatchesFooter() {
        return matchesFooter;
    }

    private void setMatchesFooter(long matchesFooter) {
        this.matchesFooter = matchesFooter;
    }

    public long getClassificationsFooter() {
        return classificationsFooter;
    }

    public void setClassificationsFooter(long classificationsFooter) {
        this.classificationsFooter = classificationsFooter;
    }

    public long getClassificationsStart() {
        return classificationsStart;
    }

    private void setClassificationsStart(long classificationsStart) {
        this.classificationsStart = classificationsStart;
    }

    public long getAuxStart() {
        return auxStart;
    }

    public void setAuxStart(long auxStart) {
        this.auxStart = auxStart;
    }

    public long getAuxFooter() {
        return auxFooter;
    }

    public void setAuxFooter(long auxFooter) {
        this.auxFooter = auxFooter;
    }

    public long getMatchesStart() {
        return matchesStart;
    }

    private void setMatchesStart(long matchesStart) {
        this.matchesStart = matchesStart;
    }
}
