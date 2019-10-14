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
package rusch.megan5client.connector;

import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import jloda.util.Single;
import megan.data.*;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import rusch.megan5client.*;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * The {@link IConnector} instance for datasets hosted at the Megan5Server
 *
 * @author Hans-Joachim Ruscheweyh
 * 4:08:22 PM - Nov 1, 2014
 */
public class Megan5ServerConnector implements IConnector {
    private final RestTemplate restTemplate;
    private final HttpEntity<String> request;
    private final String url;
    private String fileId;

    private static final ConcurrentMap<String, Object> url2response = new ConcurrentHashMap<>();

    /**
     * Create connection and apply authentication.
     * <p/>
     * !this method doesn't test the connection. So things can still go wrong later!
     *
     * @param url
     * @param userName
     * @param password
     */
    public Megan5ServerConnector(String url, String userName, String password) {
        final String plainCreds = userName + ":" + password;
        final byte[] plainCredsBytes = plainCreds.getBytes();
        final byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        final String base64Creds = new String(base64CredsBytes);
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Creds);
        request = new HttpEntity<>(headers);
        restTemplate = new RestTemplate();
        if (url.endsWith("/")) {
            this.url = url;
        } else {
            this.url = url + "/";
        }
    }

    public RMADataset[] getAvailiableDatasets() {
        final String requestURL = url + RMAControllerMappings.LIST_DATASETS_MAPPING;
        if (!url2response.containsKey(requestURL))
            url2response.put(requestURL, restTemplate.exchange(requestURL, HttpMethod.GET, request, RMADataset[].class).getBody());
        return (RMADataset[]) url2response.get(requestURL);
    }

    @Override
    public void setFile(String filename) throws IOException {
        this.fileId = filename;

    }

    @Override
    public boolean isReadOnly() throws IOException {
        final String requestURL = url + RMAControllerMappings.IS_READ_ONLY_MAPPING + "?fileId=" + fileId;
        if (!url2response.containsKey(requestURL))
            url2response.put(requestURL, restTemplate.exchange(url + RMAControllerMappings.IS_READ_ONLY_MAPPING + "?fileId=" + fileId, HttpMethod.GET, request, Boolean.class).getBody());
        return (Boolean) url2response.get(requestURL);
    }

    @Override
    public long getUId() throws IOException {
        final String requestURL = url + RMAControllerMappings.GET_UID_MAPPING + "?fileId=" + fileId;
        if (!url2response.containsKey(requestURL))
            url2response.put(requestURL, restTemplate.exchange(requestURL, HttpMethod.GET, request, Long.class).getBody());
        return (Long) url2response.get(requestURL);
    }

    @Override
    public IReadBlockIterator getAllReadsIterator(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        final ResponseEntity<ReadBlockPage> response = restTemplate.exchange(url + RMAControllerMappings.GET_ALL_READS_ITERATOR_MAPPING + "?fileId=" + fileId + "&minScore=" + minScore + "&maxExpected=" + maxExpected + "&dataSelection=" + httpArray2(DataSelectionSerializer.serializeDataSelection(wantReadSequence, wantMatches)), HttpMethod.GET, request, ReadBlockPage.class);
        final ReadBlockPage blocks = response.getBody();
        return new ReadBlockIterator(this, blocks);
    }

    @Override
    public IReadBlockIterator getReadsIterator(String classification, int classId, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        final ResponseEntity<ReadBlockPage> response = restTemplate.exchange(url + RMAControllerMappings.GET_READS_ITERATOR_MAPPING + "?fileId=" + fileId + "&minScore=" + minScore + "&maxExpected=" + maxExpected + "&classification=" + classification + "&classId=" + classId + "&dataSelection=" + httpArray2(DataSelectionSerializer.serializeDataSelection(wantReadSequence, wantMatches)), HttpMethod.GET, request, ReadBlockPage.class);
        final ReadBlockPage blocks = response.getBody();
        return new ReadBlockIterator(this, blocks);
    }

    @Override
    public IReadBlockIterator getReadsIteratorForListOfClassIds(String classification, Collection<Integer> classIds, float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        final ResponseEntity<ReadBlockPage> response = restTemplate.exchange(url + RMAControllerMappings.GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_MAPPING + "?fileId=" + fileId + "&minScore=" + minScore + "&maxExpected=" + maxExpected + "&classification=" + classification + "&classIds=" + httpArray(classIds) + "&dataSelection=" + httpArray2(DataSelectionSerializer.serializeDataSelection(wantReadSequence, wantMatches)), HttpMethod.GET, request, ReadBlockPage.class);
        final ReadBlockPage blocks = response.getBody();
        return new ReadBlockIterator(this, blocks);
    }

    @Override
    public IReadBlockGetter getReadBlockGetter(float minScore, float maxExpected, boolean wantReadSequence, boolean wantMatches) throws IOException {
        return new ReadBlockGetter(fileId, minScore, maxExpected, wantReadSequence, wantMatches, this);
    }


    @Override
    public String[] getAllClassificationNames() throws IOException {
        final String requestURL = url + RMAControllerMappings.GET_ALL_CLASSIFICATION_NAMES_MAPPING + "?fileId=" + fileId;
        if (!url2response.containsKey(requestURL))
            url2response.put(requestURL, restTemplate.exchange(requestURL, HttpMethod.GET, request, String[].class).getBody());
        return (String[]) url2response.get(requestURL);

    }

    @Override
    public int getClassificationSize(String classificationName) throws IOException {
        final String requestURL = url + RMAControllerMappings.GET_CLASSIFICATION_SIZE_MAPPING + "?fileId=" + fileId + "&classification=" + classificationName;
        if (!url2response.containsKey(requestURL))
            url2response.put(requestURL, restTemplate.exchange(requestURL, HttpMethod.GET, request, Integer.class).getBody());
        return (Integer) url2response.get(requestURL);
    }

    @Override
    public int getClassSize(String classificationName, int classId) throws IOException {
        final String requestURL = url + RMAControllerMappings.GET_CLASS_SIZE_MAPPING + "?fileId=" + fileId + "&classification=" + classificationName + "&classId=" + classId;
        if (!url2response.containsKey(requestURL))
            url2response.put(requestURL, restTemplate.exchange(requestURL, HttpMethod.GET, request, Integer.class).getBody());
        return (Integer) url2response.get(requestURL);
    }

    @Override
    public IClassificationBlock getClassificationBlock(String classificationName) throws IOException {
        final String requestURL = url + RMAControllerMappings.GET_CLASSIFICATIONBLOCK_MAPPING + "?fileId=" + fileId + "&classification=" + classificationName;
        if (!url2response.containsKey(requestURL))
            url2response.put(requestURL, restTemplate.exchange(requestURL, HttpMethod.GET, request, ClassificationBlockServer.class).getBody());
        return new ClassificationBlock((ClassificationBlockServer) url2response.get(requestURL));
    }

    @Override
    public void updateClassifications(String[] classificationNames, List<UpdateItem> updateItems, ProgressListener progressListener) throws IOException, CanceledException {
        System.err.println("updateClassifications(String[] classificationNames,List<UpdateItem> updateItems, ProgressListener progressListener): not implemented");
    }

    @Override
    public IReadBlockIterator getFindAllReadsIterator(String regEx, FindSelection findSelection, Single<Boolean> canceled) throws IOException {
        final ResponseEntity<ReadBlockPage> response = restTemplate.exchange(url + RMAControllerMappings.GET_FIND_ALL_READS_ITERATOR_MAPPING + "?fileId=" + fileId + "&regEx=" + regEx + "&findSelection=" + httpArray2(DataSelectionSerializer.serializeFindSelection(findSelection)), HttpMethod.GET, request, ReadBlockPage.class);
        ReadBlockPage blocks = response.getBody();
        return new ReadBlockIterator(this, blocks);
    }

    @Override
    public int getNumberOfReads() throws IOException {
        final String requestURL = url + RMAControllerMappings.GET_NUMBER_OF_READS_MAPPING + "?fileId=" + fileId;
        if (!url2response.containsKey(requestURL))
            url2response.put(requestURL, restTemplate.exchange(requestURL, HttpMethod.GET, request, Integer.class).getBody());
        return (Integer) url2response.get(requestURL);
    }

    @Override
    public int getNumberOfMatches() throws IOException {
        final String requestURL = url + RMAControllerMappings.GET_NUMBER_OF_MATCHES_MAPPING + "?fileId=" + fileId;
        if (!url2response.containsKey(requestURL))
            url2response.put(requestURL, restTemplate.exchange(requestURL, HttpMethod.GET, request, Integer.class).getBody());
        return (Integer) url2response.get(requestURL);
    }

    @Override
    public void setNumberOfReads(int numberOfReads) throws IOException {
        System.err.println("setNumberOfReads(int numberOfReads): not implemented");

    }

    @Override
    public void putAuxiliaryData(Map<String, byte[]> label2data)
            throws IOException {
        System.err.println("putAuxiliaryData(Map<String, byte[]> label2data): not implemented");

    }

    @Override
    public Map<String, byte[]> getAuxiliaryData() throws IOException {
        final String requestURL = url + RMAControllerMappings.GET_AUXILIARY_MAPPING + "?fileId=" + fileId;
        if (!url2response.containsKey(requestURL)) {
            ResponseEntity<Map> response = restTemplate.exchange(requestURL, HttpMethod.GET, request, Map.class);
            final Map<String, String> map = response.getBody();
            final Map<String, byte[]> map2 = new HashMap<>();
            for (Entry<String, String> entry : map.entrySet()) {
                map2.put(entry.getKey(), entry.getValue().getBytes());
            }
            url2response.put(requestURL, map2);
        }
        return (Map<String, byte[]>) url2response.get(requestURL);
    }


    /**
     * Concatenate with comma
     *
     * @param classIds
     * @return
     */
    private String httpArray(Collection<Integer> classIds) {
        StringBuilder s = new StringBuilder();
        for (Object o : classIds) {
            s.append(o.toString()).append(",");
        }
        return s.substring(0, s.length() - 1);
    }

    /**
     * Concatenate with comma
     *
     * @param classIds
     * @return
     */
    public static String httpArray2(List<String> classIds) {
        StringBuilder s = new StringBuilder();
        for (Object o : classIds) {
            s.append(o.toString()).append(",");
        }
        return s.substring(0, s.length() - 1);
    }

    public IReadBlock getReadBlock(long readUid, String fileId, float minScore, float maxExpected, boolean wantReadText, boolean wantMatches) {
        ResponseEntity<ReadBlockServer> response = restTemplate.exchange(url + RMAControllerMappings.GET_READ_MAPPING + "?fileId=" + fileId + "&readUid=" + readUid + "&minScore=" + minScore + "&maxExpected=" + maxExpected, HttpMethod.GET, request, ReadBlockServer.class);
        ReadBlockServer blocks = response.getBody();
        return new ReadBlock(blocks);
    }

    /**
     * Get next {@link ReadBlockPage}.
     *
     * @param pageId
     * @return
     */
    public ReadBlockPage retrieveReadBlockPage(String pageId) {
        ResponseEntity<ReadBlockPage> response = restTemplate.exchange(url + RMAControllerMappings.LOAD_READ_PAGE_MAPPING + "?pageId=" + pageId, HttpMethod.GET, request, ReadBlockPage.class);
        return response.getBody();
    }


    /**
     * Get a List of users
     *
     * @return
     */
    public String[] listUsers() {
        ResponseEntity<String[]> response = restTemplate.exchange(url + RMAControllerMappings.LIST_USERS_MAPPING, HttpMethod.GET, request, String[].class);
        return response.getBody();
    }

    /**
     * Add a single user
     *
     * @param user
     * @param password
     * @param isAdmin
     * @return
     */
    public String addUser(String user, String password, boolean isAdmin) {
        ResponseEntity<String> response = restTemplate.exchange(url + RMAControllerMappings.ADD_USER_MAPPING + "userName=" + user + "&password=" + password + "&isAdmin=" + isAdmin, HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    /**
     * Remove a user
     *
     * @param user
     * @return
     */
    public String removeUser(String user) {
        ResponseEntity<String> response = restTemplate.exchange(url + RMAControllerMappings.REMOVE_USER_MAPPING + "userName=" + user, HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    /**
     * Get log entries
     *
     * @return
     */
    public String[] getLogEntries() {
        ResponseEntity<String[]> response = restTemplate.exchange(url + RMAControllerMappings.GET_LOG_MAPPING, HttpMethod.GET, request, String[].class);
        return response.getBody();
    }

    /**
     * Get Info text
     *
     * @return
     */
    public String getInfo() {
        final String requestURL = url + RMAControllerMappings.GET_INFO_MAPPING;
        if (!url2response.containsKey(requestURL))
            url2response.put(requestURL, restTemplate.exchange(requestURL, HttpMethod.GET, request, String.class).getBody());
        return (String) url2response.get(requestURL);
    }

    public static void clearCache() {
        url2response.clear();
    }
}
