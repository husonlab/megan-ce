/*
 * Copyright (C) 2020. Daniel H. Huson
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
 *
 */
package megan.ms.client.connector;

import jloda.util.Basic;
import jloda.util.ProgressListener;
import jloda.util.Single;
import megan.core.Document;
import megan.core.SampleAttributeTable;
import megan.data.*;
import megan.ms.Utilities;
import megan.ms.client.ClientMS;
import megan.ms.clientdialog.service.RemoteServiceManager;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MeganServer connector
 * Daniel Huson, 8.2020
 */
public class MSConnector implements IConnector {
    private final ClientMS client;
    private String fileName;

    public MSConnector(String serverFileName) {
        final String serverURL = RemoteServiceManager.getServerURL(serverFileName);
        final String user = RemoteServiceManager.getUser(serverFileName);
        final String passwordMD5 = RemoteServiceManager.getPassword(serverFileName);
        final String filePath = RemoteServiceManager.getFilePath(serverFileName);
        client = new ClientMS(serverURL, null, 0, user, passwordMD5, 100);
        setFile(filePath);
    }

    @Override
    public void setFile(String filename) {
        this.fileName = filename;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public long getUId() throws IOException {
        return client.getAsLong("fileUid?file=" + fileName);
    }

    @Override
    public IReadBlockIterator getAllReadsIterator(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return new ReadBlockIteratorMS(client, fileName, minScore, maxExpected, wantReadSequence, wantMatches);
    }

    @Override
    public IReadBlockIterator getReadsIterator(String classification, int classId, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return getReadsIteratorForListOfClassIds(classification, Collections.singletonList(classId), minScore, maxExpected, wantReadSequence, wantMatches);
    }

    @Override
    public IReadBlockIterator getReadsIteratorForListOfClassIds(String classification, Collection<Integer> classIds, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return new ReadBlockIteratorMS(client, fileName, classification, classIds, minScore, maxExpected, wantReadSequence, wantMatches);
    }

    @Override
    public IReadBlockGetter getReadBlockGetter(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return new ReadBlockGetterMS(client, fileName, wantReadSequence, wantMatches);
    }

    @Override
    public String[] getAllClassificationNames() throws IOException {
        return Basic.getLinesFromString(client.getAsString("getClassificationNames?file=" + fileName)).toArray(new String[0]);
    }

    @Override
    public int getClassificationSize(String classificationName) throws IOException {
        return Basic.parseInt(client.getAsString("getClassificationSize?file=" + fileName + "&classification=" + classificationName));
    }

    @Override
    public int getClassSize(String classificationName, int classId) throws IOException {
        return Basic.parseInt(client.getAsString("getClassSize?file=" + fileName + "&classification=" + classificationName + "&sum=true&classId=" + classId));
    }

    @Override
    public IClassificationBlock getClassificationBlock(String classificationName) throws IOException {
        return Utilities.getClassificationBlockFromBytes(client.getAsBytes("getClassificationBlock?file=" + fileName + "&classification=" + classificationName + "&binary=true"));
    }

    @Override
    public void updateClassifications(String[] classificationNames, List<UpdateItem> updateItems, ProgressListener progressListener) throws IOException {
        System.err.println("updateClassifications: not implemented");
    }

    @Override
    public IReadBlockIterator getFindAllReadsIterator(String regEx, FindSelection findSelection, Single<Boolean> canceled) throws IOException {
        return new FindAllReadsIterator(regEx, findSelection, getAllReadsIterator(0, 10, true, true), canceled);
    }

    @Override
    public int getNumberOfReads() throws IOException {
        return Basic.parseInt(client.getAsString("getNumberOfReads?file=" + fileName));
    }

    @Override
    public int getNumberOfMatches() throws IOException {
        return Basic.parseInt(client.getAsString("getNumberOfMatches?file=" + fileName));
    }

    @Override
    public void setNumberOfReads(int numberOfReads) throws IOException {
        System.err.println("setNumberOfReads: not implemented");
    }

    @Override
    public void putAuxiliaryData(Map<String, byte[]> label2data) throws IOException {
        System.err.println("putAuxiliaryData: not implemented");
    }

    @Override
    public Map<String, byte[]> getAuxiliaryData() throws IOException {
        return Utilities.getAuxiliaryDataFromBytes(client.getAsBytes("getAuxiliaryData?file=" + fileName + "&binary=true"));
    }

    /**
     * load the set megan summary file
     */
    public void loadMeganSummaryFile(Document document) throws IOException {
        final String fileContent=Basic.toString(getAuxiliaryData().get("FILE_CONTENT"));
        document.loadMeganSummary(new BufferedReader(new StringReader(fileContent)));
     }
}
