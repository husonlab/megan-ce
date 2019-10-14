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
import megan.core.ClassificationType;
import megan.io.IInputReader;
import megan.io.IOutputWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Format of a match in an RMA3 file
 * Created by huson on 5/16/14.
 */
public class ClassificationsFooterRMA3 extends BaseRMA3 {
    private final String defaultFormat;
    private boolean doKegg = false;
    private boolean doSeed = false;
    private boolean doCog = false;
    private boolean doPfam = false;

    private String classificationBlockFormat = ClassificationBlockRMA3.FORMAT;

    private final EnumMap<ClassificationType, Long> startLocation = new EnumMap<>(ClassificationType.class);
    private final EnumMap<ClassificationType, Long> endLocation = new EnumMap<>(ClassificationType.class);

    /**
     * constructor
     */
    public ClassificationsFooterRMA3() {
        super("ClassificationBlockFormat:String TaxStart:Long TaxEnd:Long");
        defaultFormat = getFormatDef();
    }

    /**
     * constructor
     */
    public ClassificationsFooterRMA3(boolean doKegg, boolean doSeed, boolean doCog, boolean doPfam) {
        super("TaxStart:Long TaxEnd:Long");
        defaultFormat = getFormatDef();
        if (doKegg)
            setDo(ClassificationType.KEGG);
        if (doSeed)
            setDo(ClassificationType.SEED);
        if (doCog)
            setDo(ClassificationType.COG);
        if (doCog)
            setDo(ClassificationType.PFAM);
    }

    @Override
    public void read(IInputReader reader, long startPos) throws IOException {
        reader.seek(startPos);
        setFormatDef(reader.readString());

        doKegg = doSeed = doCog = doPfam = false;

        FormatDefinition formatDefinition = FormatDefinition.fromString(getFormatDef());
        for (Pair<String, FormatDefinition.Type> pair : formatDefinition.getList()) {
            {
                switch (pair.getFirst()) {
                    case "ClassificationBlockFormat":
                        setClassificationBlockFormat(reader.readString());
                        break;
                    case "TaxStart":
                        setStart(ClassificationType.Taxonomy, reader.readLong());
                        break;
                    case "TaxEnd":
                        setEnd(ClassificationType.Taxonomy, reader.readLong());
                        break;
                    case "KeggStart":
                        setStart(ClassificationType.KEGG, reader.readLong());
                        doKegg = true;
                        break;
                    case "KeggEnd":
                        setEnd(ClassificationType.KEGG, reader.readLong());
                        break;
                    case "SeedStart":
                        setStart(ClassificationType.SEED, reader.readLong());
                        doSeed = true;
                        break;
                    case "SeedEnd":
                        setEnd(ClassificationType.SEED, reader.readLong());
                        break;
                    case "CogStart":
                        setStart(ClassificationType.COG, reader.readLong());
                        doCog = true;
                        break;
                    case "CogEnd":
                        setEnd(ClassificationType.COG, reader.readLong());
                        break;
                    case "PfamStart":
                        setStart(ClassificationType.PFAM, reader.readLong());
                        doPfam = true;
                        break;
                    case "PfamEnd":
                        setEnd(ClassificationType.PFAM, reader.readLong());
                        break;
                }
            }
        }
    }

    @Override
    public void write(IOutputWriter writer) throws IOException {
        writer.writeString(getFormatDef());

        FormatDefinition formatDefinition = FormatDefinition.fromString(getFormatDef());
        formatDefinition.startWrite();
        for (Pair<String, FormatDefinition.Type> pair : formatDefinition.getList()) {
            {
                switch (pair.getFirst()) {
                    case "ClassificationBlockFormat":
                        formatDefinition.write(writer, "ClassificationBlockFormat", getClassificationBlockFormat());
                        break;
                    case "TaxStart":
                        formatDefinition.write(writer, "TaxStart", getStart(ClassificationType.Taxonomy));
                        break;
                    case "TaxEnd":
                        formatDefinition.write(writer, "TaxEnd", getEnd(ClassificationType.Taxonomy));
                        break;
                    case "KeggStart":
                        formatDefinition.write(writer, "KeggStart", getStart(ClassificationType.KEGG));
                        break;
                    case "KeggEnd":
                        formatDefinition.write(writer, "KeggEnd", getEnd(ClassificationType.KEGG));
                        break;
                    case "SeedStart":
                        formatDefinition.write(writer, "SeedStart", getStart(ClassificationType.SEED));
                        break;
                    case "SeedEnd":
                        formatDefinition.write(writer, "SeedEnd", getEnd(ClassificationType.SEED));
                        break;
                    case "CogStart":
                        formatDefinition.write(writer, "CogStart", getStart(ClassificationType.COG));
                        break;
                    case "CogEnd":
                        formatDefinition.write(writer, "CogEnd", getEnd(ClassificationType.COG));
                        break;
                    case "PfamStart":
                        formatDefinition.write(writer, "PfamStart", getStart(ClassificationType.PFAM));
                        break;
                    case "PfamEnd":
                        formatDefinition.write(writer, "PfamEnd", getEnd(ClassificationType.PFAM));
                        break;
                }
            }
        }
        formatDefinition.finishWrite();
    }

    private void updateFormat() {
        setFormatDef(defaultFormat);
        if (doKegg)
            setFormatDef(getFormatDef() + " KeggStart:Long KeggEnd:Long");
        if (doSeed)
            setFormatDef(getFormatDef() + " SeedStart:Long SeedEnd:Long");
        if (doCog)
            setFormatDef(getFormatDef() + " CogStart:Long CogEnd:Long");
        if (doPfam)
            setFormatDef(getFormatDef() + " PfamStart:Long PfamEnd:Long");
    }

    public long getStart(ClassificationType type) {
        Long value = startLocation.get(type);
        return value != null ? value : 0L;
    }

    public void setStart(ClassificationType type, long location) {
        startLocation.put(type, location);
    }

    private long getEnd(ClassificationType type) {
        Long value = endLocation.get(type);
        return value != null ? value : 0L;
    }

    public void setEnd(ClassificationType type, long location) {
        endLocation.put(type, location);
    }

    public List<String> getAllNames() {
        ArrayList<String> names = new ArrayList<>(4);
        names.add(ClassificationType.Taxonomy.toString());
        if (doKegg)
            names.add("KEGG");
        if (doSeed)
            names.add("SEED");
        if (doCog)
            names.add("EGGNOG");
        if (doPfam)
            names.add("PFAM");
        return names;
    }

    public void clear() {
        startLocation.clear();
        endLocation.clear();
    }

    public void setDo(ClassificationType classificationType) {
        switch (classificationType) {
            case KEGG:
                doKegg = true;
                break;
            case SEED:
                doSeed = true;
                break;
            case COG:
                doCog = true;
                break;
            case PFAM:
                doPfam = true;
                break;
            default:
                return;
        }
        updateFormat();
    }

    public boolean isDo(ClassificationType classificationType) {
        switch (classificationType) {
            case KEGG:
                return doKegg;
            case SEED:
                return doSeed;
            case COG:
                return doCog;
            case PFAM:
                return doPfam;
            case Taxonomy:
                return true;
            default:
                return false;
        }
    }

    private String getClassificationBlockFormat() {
        return classificationBlockFormat;
    }

    private void setClassificationBlockFormat(String classificationBlockFormat) {
        this.classificationBlockFormat = classificationBlockFormat;
    }
}
