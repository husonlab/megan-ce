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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Mapping Classes for Megan5Server
 *
 * @author Hans-Joachim Ruscheweyh
 * 3:03:13 PM - Oct 27, 2014
 * <p/>
 * <p/>
 * Mapping file for all Connector requests
 */
public class RMAControllerMappings {
    //ADMIN


    public final static String GET_LOG_MAPPING = "admin/getLog";
    private final static String GET_LOG_METHOD = "GET";
    private final static List<Parameter> GET_LOG_PARAMETER = new ArrayList<>();
    private final static String GET_LOG_DESCRIPTION = "Get the most recent server log entries as a String[].";

    static {
    }

    private final static String GET_LOG_RETURN_OBJECT = "String[]";
    private final static Map<String, Object> GET_LOG_REQUEST = new HashMap<>();

    static {
        GET_LOG_REQUEST.put("Mapping Name", GET_LOG_MAPPING);
        GET_LOG_REQUEST.put("Method", GET_LOG_METHOD);
        GET_LOG_REQUEST.put("Parameter", GET_LOG_PARAMETER);
        GET_LOG_REQUEST.put("Return Object", GET_LOG_RETURN_OBJECT);
        GET_LOG_REQUEST.put("Description", GET_LOG_DESCRIPTION);
    }


    static {
        GET_LOG_REQUEST.put("Mapping Name", GET_LOG_MAPPING);
        GET_LOG_REQUEST.put("Method", GET_LOG_METHOD);
        GET_LOG_REQUEST.put("Parameter", GET_LOG_PARAMETER);
        GET_LOG_REQUEST.put("Return Object", GET_LOG_RETURN_OBJECT);
        GET_LOG_REQUEST.put("Description", GET_LOG_DESCRIPTION);
    }

    public final static String GET_INFO_MAPPING = "info";
    private final static String GET_INFO_METHOD = "GET";
    private final static List<Parameter> GET_INFO_PARAMETER = new ArrayList<>();
    private final static String GET_INFO_DESCRIPTION = "Get general information about server, its data and contact information.";

    static {
    }

    private final static String GET_INFO_RETURN_OBJECT = "String";
    private final static Map<String, Object> GET_INFO_REQUEST = new HashMap<>();

    static {
        GET_INFO_REQUEST.put("Mapping Name", GET_INFO_MAPPING);
        GET_INFO_REQUEST.put("Method", GET_INFO_METHOD);
        GET_INFO_REQUEST.put("Parameter", GET_INFO_PARAMETER);
        GET_INFO_REQUEST.put("Return Object", GET_INFO_RETURN_OBJECT);
        GET_INFO_REQUEST.put("Description", GET_INFO_DESCRIPTION);
    }


    private final static String UPDATE_DATASETS_MAPPING = "admin/updateDatasets";
    private final static String UPDATE_DATASETS_METHOD = "GET";
    private final static List<Parameter> UPDATE_DATASETS_PARAMETER = new ArrayList<>();
    private final static String UPDATE_DATASETS_DESCRIPTION = "Update the MeganServer filesystem on the server. This should be performed once new datasets should be published or after the config file was changed.";

    static {
    }

    private final static String UPDATE_DATASETS_RETURN_OBJECT = "String";
    private final static Map<String, Object> UPDATE_DATASETS_REQUEST = new HashMap<>();

    static {
        UPDATE_DATASETS_REQUEST.put("Mapping Name", UPDATE_DATASETS_MAPPING);
        UPDATE_DATASETS_REQUEST.put("Method", UPDATE_DATASETS_METHOD);
        UPDATE_DATASETS_REQUEST.put("Parameter", UPDATE_DATASETS_PARAMETER);
        UPDATE_DATASETS_REQUEST.put("Return Object", UPDATE_DATASETS_RETURN_OBJECT);
        UPDATE_DATASETS_REQUEST.put("Description", UPDATE_DATASETS_DESCRIPTION);
    }


    public final static String ADD_USER_MAPPING = "admin/addUser";
    private final static String ADD_USER_METHOD = "GET";
    private final static List<Parameter> ADD_USER_PARAMETER = new ArrayList<>();
    private final static String ADD_USER_DESCRIPTION = "Allows admins to add/edit users to the MeganServer instance.";

    static {
        Parameter p = new Parameter("userName", true, "default");
        Parameter p2 = new Parameter("password", true, "default");
        Parameter p3 = new Parameter("isAdmin", false, "false");
        ADD_USER_PARAMETER.add(p);
        ADD_USER_PARAMETER.add(p2);
        ADD_USER_PARAMETER.add(p3);
    }

    private final static String ADD_USER_RETURN_OBJECT = "String";
    private final static Map<String, Object> ADD_USER_REQUEST = new HashMap<>();

    static {
        ADD_USER_REQUEST.put("Mapping Name", ADD_USER_MAPPING);
        ADD_USER_REQUEST.put("Method", ADD_USER_METHOD);
        ADD_USER_REQUEST.put("Parameter", ADD_USER_PARAMETER);
        ADD_USER_REQUEST.put("Return Object", ADD_USER_RETURN_OBJECT);
        ADD_USER_REQUEST.put("Description", ADD_USER_DESCRIPTION);
    }


    public final static String REMOVE_USER_MAPPING = "admin/removeUser";
    private final static String REMOVE_USER_METHOD = "GET";
    private final static List<Parameter> REMOVE_USER_PARAMETER = new ArrayList<>();
    private final static String REMOVE_USER_DESCRIPTION = "Delete a user from the MeganServer instance. This can not be revoked.";

    static {
        Parameter p = new Parameter("userName", true, "default");
        REMOVE_USER_PARAMETER.add(p);
    }

    private final static String REMOVE_USER_RETURN_OBJECT = "String";
    private final static Map<String, Object> REMOVE_USER_REQUEST = new HashMap<>();

    static {
        REMOVE_USER_REQUEST.put("Mapping Name", REMOVE_USER_MAPPING);
        REMOVE_USER_REQUEST.put("Method", REMOVE_USER_METHOD);
        REMOVE_USER_REQUEST.put("Parameter", REMOVE_USER_PARAMETER);
        REMOVE_USER_REQUEST.put("Return Object", REMOVE_USER_RETURN_OBJECT);
        REMOVE_USER_REQUEST.put("Description", REMOVE_USER_DESCRIPTION);
    }


    public final static String LIST_USERS_MAPPING = "admin/listUsers";
    private final static String LIST_USERS_METHOD = "GET";
    private final static List<Parameter> LIST_USERS_PARAMETER = new ArrayList<>();
    private final static String LIST_USERS_DESCRIPTION = "Retrieve a list of users";

    static {
    }

    private final static String LIST_USERS_RETURN_OBJECT = "String[]";
    private final static Map<String, Object> LIST_USERS_REQUEST = new HashMap<>();

    static {
        LIST_USERS_REQUEST.put("Mapping Name", LIST_USERS_MAPPING);
        LIST_USERS_REQUEST.put("Method", LIST_USERS_METHOD);
        LIST_USERS_REQUEST.put("Parameter", LIST_USERS_PARAMETER);
        LIST_USERS_REQUEST.put("Return Object", LIST_USERS_RETURN_OBJECT);
        LIST_USERS_REQUEST.put("Description", LIST_USERS_DESCRIPTION);
    }


    //USER
    public final static String GET_UID_MAPPING = "getFileUid";
    private final static String GET_UID_METHOD = "GET";
    private final static List<Parameter> GET_UID_PARAMETER = new ArrayList<>();
    private final static String GET_UID_DESCRIPTION = "Get the unique uid for a dataset.";

    static {
        Parameter p = new Parameter("fileId", true, "default");
        GET_UID_PARAMETER.add(p);
    }

    private final static String GET_UID_RETURN_OBJECT = "Long";
    private final static Map<String, Object> GET_UID_REQUEST = new HashMap<>();

    static {
        GET_UID_REQUEST.put("Mapping Name", GET_UID_MAPPING);
        GET_UID_REQUEST.put("Method", GET_UID_METHOD);
        GET_UID_REQUEST.put("Parameter", GET_UID_PARAMETER);
        GET_UID_REQUEST.put("Return Object", GET_UID_RETURN_OBJECT);
        GET_UID_REQUEST.put("Description", GET_UID_DESCRIPTION);
    }


    public final static String IS_READ_ONLY_MAPPING = "isReadOnly";
    private final static String IS_READ_ONLY_METHOD = "GET";
    private final static List<Parameter> IS_READ_ONLY_PARAMETER = new ArrayList<>();
    private final static String IS_READ_ONLY_DESCRIPTION = "Show permissions of the user on the dataset. Currently all datasets are read-only unless changed on the raw file level.";

    static {
        Parameter p = new Parameter("fileId", true, "default");
        IS_READ_ONLY_PARAMETER.add(p);
    }

    private final static String IS_READ_ONLY_RETURN_OBJECT = "Boolean";
    private final static Map<String, Object> IS_READ_ONLY_REQUEST = new HashMap<>();

    static {
        IS_READ_ONLY_REQUEST.put("Mapping Name", IS_READ_ONLY_MAPPING);
        IS_READ_ONLY_REQUEST.put("Method", IS_READ_ONLY_METHOD);
        IS_READ_ONLY_REQUEST.put("Parameter", IS_READ_ONLY_PARAMETER);
        IS_READ_ONLY_REQUEST.put("Return Object", IS_READ_ONLY_RETURN_OBJECT);
        IS_READ_ONLY_REQUEST.put("Description", IS_READ_ONLY_DESCRIPTION);
    }


    public final static String LIST_DATASETS_MAPPING = "listDatasets";
    private final static String LIST_DATASETS_METHOD = "GET";
    private final static List<Parameter> LIST_DATASETS_PARAMETER = new ArrayList<>();

    static {
        Parameter p = new Parameter("includeMetadata", false, "False");
        LIST_DATASETS_PARAMETER.add(p);
    }

    private final static String LIST_DATASETS_DESCRIPTION = "Retrieve a list of datasets that is known to the MeganServer instance.";

    private final static String LIST_DATASETS_RETURN_OBJECT = "RMADataset[]";
    private final static Map<String, Object> LIST_DATASETS_REQUEST = new HashMap<>();

    static {
        LIST_DATASETS_REQUEST.put("Mapping Name", LIST_DATASETS_MAPPING);
        LIST_DATASETS_REQUEST.put("Method", LIST_DATASETS_METHOD);
        LIST_DATASETS_REQUEST.put("Parameter", LIST_DATASETS_PARAMETER);
        LIST_DATASETS_REQUEST.put("Return Object", LIST_DATASETS_RETURN_OBJECT);
        LIST_DATASETS_REQUEST.put("Description", LIST_DATASETS_DESCRIPTION);
    }


    private final static String ABOUT_MAPPING = "About";
    private final static String ABOUT_METHOD = "GET";
    private final static List<Parameter> ABOUT_PARAMETER = new ArrayList<>();
    private final static String ABOUT_DESCRIPTION = "Get a brief About string including version number, build date and authors.";

    private final static String ABOUT_RETURN_OBJECT = "About";
    private final static Map<String, Object> ABOUT_REQUEST = new HashMap<>();

    static {
        ABOUT_REQUEST.put("Mapping Name", ABOUT_MAPPING);
        ABOUT_REQUEST.put("Method", ABOUT_METHOD);
        ABOUT_REQUEST.put("Parameter", ABOUT_PARAMETER);
        ABOUT_REQUEST.put("Return Object", ABOUT_RETURN_OBJECT);
        ABOUT_REQUEST.put("Description", ABOUT_DESCRIPTION);
    }


    public final static String GET_AUXILIARY_MAPPING = "getAuxiliary";
    private final static String GET_AUXILIARY_METHOD = "GET";
    private final static List<Parameter> GET_AUXILIARY_PARAMETER = new ArrayList<>();
    private final static String GET_AUXILIARY_DESCRIPTION = "Get auxiliary data for a single dataset.";

    static {
        Parameter p = new Parameter("fileId", true, "default");
        GET_AUXILIARY_PARAMETER.add(p);
    }

    private final static String GET_AUXILIARY_RETURN_OBJECT = "Map<String,String>";
    private final static Map<String, Object> GET_AUXILIARY_REQUEST = new HashMap<>();

    static {
        GET_AUXILIARY_REQUEST.put("Mapping Name", GET_AUXILIARY_MAPPING);
        GET_AUXILIARY_REQUEST.put("Method", GET_AUXILIARY_METHOD);
        GET_AUXILIARY_REQUEST.put("Parameter", GET_AUXILIARY_PARAMETER);
        GET_AUXILIARY_REQUEST.put("Return Object", GET_AUXILIARY_RETURN_OBJECT);
        GET_AUXILIARY_REQUEST.put("Description", GET_AUXILIARY_DESCRIPTION);
    }


    public final static String GET_ALL_CLASSIFICATION_NAMES_MAPPING = "getAllClassificationNames";
    private final static String GET_ALL_CLASSIFICATION_NAMES_METHOD = "GET";
    private final static List<Parameter> GET_ALL_CLASSIFICATION_NAMES_PARAMETER = new ArrayList<>();
    private final static String GET_ALL_CLASSIFICATION_NAMES_DESCRIPTION = "Retrieve a list of classifications that are avaliable in a dataset.";

    static {
        Parameter p = new Parameter("fileId", true, "default");
        GET_ALL_CLASSIFICATION_NAMES_PARAMETER.add(p);
    }

    private final static String GET_ALL_CLASSIFICATION_NAMES_RETURN_OBJECT = "String[]";
    private final static Map<String, Object> GET_ALL_CLASSIFICATION_NAMES_REQUEST = new HashMap<>();

    static {
        GET_ALL_CLASSIFICATION_NAMES_REQUEST.put("Mapping Name", GET_ALL_CLASSIFICATION_NAMES_MAPPING);
        GET_ALL_CLASSIFICATION_NAMES_REQUEST.put("Method", GET_ALL_CLASSIFICATION_NAMES_METHOD);
        GET_ALL_CLASSIFICATION_NAMES_REQUEST.put("Parameter", GET_ALL_CLASSIFICATION_NAMES_PARAMETER);
        GET_ALL_CLASSIFICATION_NAMES_REQUEST.put("Return Object", GET_ALL_CLASSIFICATION_NAMES_RETURN_OBJECT);
        GET_ALL_CLASSIFICATION_NAMES_REQUEST.put("Description", GET_ALL_CLASSIFICATION_NAMES_DESCRIPTION);
    }


    public final static String GET_CLASSIFICATIONBLOCK_MAPPING = "getClassificationBlock";
    private final static String GET_CLASSIFICATIONBLOCK_METHOD = "GET";
    private final static List<Parameter> GET_CLASSIFICATIONBLOCK_PARAMETER = new ArrayList<>();
    private final static String GET_CLASSIFICATIONBLOCK_DESCRIPTION = "Retrieve for a dataset and a classification a number of known taxa/genes together with abundances.";

    static {
        Parameter p = new Parameter("fileId", true, "default");
        Parameter p2 = new Parameter("classification", true, "default");
        GET_CLASSIFICATIONBLOCK_PARAMETER.add(p);
        GET_CLASSIFICATIONBLOCK_PARAMETER.add(p2);
    }

    private final static String GET_CLASSIFICATIONBLOCK_RETURN_OBJECT = "ClassificationBlockServer";
    private final static Map<String, Object> GET_CLASSIFICATIONBLOCK_REQUEST = new HashMap<>();

    static {
        GET_CLASSIFICATIONBLOCK_REQUEST.put("Mapping Name", GET_CLASSIFICATIONBLOCK_MAPPING);
        GET_CLASSIFICATIONBLOCK_REQUEST.put("Method", GET_CLASSIFICATIONBLOCK_METHOD);
        GET_CLASSIFICATIONBLOCK_REQUEST.put("Parameter", GET_CLASSIFICATIONBLOCK_PARAMETER);
        GET_CLASSIFICATIONBLOCK_REQUEST.put("Return Object", GET_CLASSIFICATIONBLOCK_RETURN_OBJECT);
        GET_CLASSIFICATIONBLOCK_REQUEST.put("Description", GET_CLASSIFICATIONBLOCK_DESCRIPTION);
    }


    public final static String GET_ALL_READS_ITERATOR_MAPPING = "getAllReadsIterator";
    private final static String GET_ALL_READS_ITERATOR_METHOD = "GET";
    private final static List<Parameter> GET_ALL_READS_ITERATOR_PARAMETER = new ArrayList<>();
    private final static String GET_ALL_READS_ITERATOR_DESCRIPTION = "Get a list of reads together with annotations from a datasets and filtered by parameters such as minScore, maxExpected, and dataselection.";

    static {
        Parameter p = new Parameter("fileId", true, "default");
        Parameter p2 = new Parameter("minScore", false, "0");
        Parameter p3 = new Parameter("maxExpected", false, "100000");
        Parameter p4 = new Parameter("dataselection", false, "useRead,useReadName,useReadHeader,useReadSequence,useMateUId,useReadLength,useReadComplexity,useReadNumberOfMatches,useMatchText,useMatchIgnore,useMatchBitScore,useMatchLength,useMatchTaxonId,useMatchSeedId,useMatchKeggId,useMatchCogId,useMatchExpected,useMatchRefSeq");
        GET_ALL_READS_ITERATOR_PARAMETER.add(p);
        GET_ALL_READS_ITERATOR_PARAMETER.add(p2);
        GET_ALL_READS_ITERATOR_PARAMETER.add(p3);
        GET_ALL_READS_ITERATOR_PARAMETER.add(p4);
    }

    private final static String GET_ALL_READS_ITERATOR_RETURN_OBJECT = "ReadBlockPage";
    private final static Map<String, Object> GET_ALL_READS_ITERATOR_REQUEST = new HashMap<>();

    static {
        GET_ALL_READS_ITERATOR_REQUEST.put("Mapping Name", GET_ALL_READS_ITERATOR_MAPPING);
        GET_ALL_READS_ITERATOR_REQUEST.put("Method", GET_ALL_READS_ITERATOR_METHOD);
        GET_ALL_READS_ITERATOR_REQUEST.put("Parameter", GET_ALL_READS_ITERATOR_PARAMETER);
        GET_ALL_READS_ITERATOR_REQUEST.put("Return Object", GET_ALL_READS_ITERATOR_RETURN_OBJECT);
        GET_ALL_READS_ITERATOR_REQUEST.put("Description", GET_ALL_READS_ITERATOR_DESCRIPTION);
    }


    public final static String GET_READS_ITERATOR_MAPPING = "getReadsIterator";
    private final static String GET_READS_ITERATOR_METHOD = "GET";
    private final static List<Parameter> GET_READS_ITERATOR_PARAMETER = new ArrayList<>();
    private final static String GET_READS_ITERATOR_DESCRIPTION = "Get a list of reads together with annotations from a dataset assigned to one classification and class identifier and filtered by parameters such as minScore, maxExpected, and dataselection.";

    static {
        Parameter p = new Parameter("fileId", true, "default");
        Parameter p5 = new Parameter("classification", true, "default");
        Parameter p6 = new Parameter("classId", true, "default");
        Parameter p2 = new Parameter("minScore", false, "0");
        Parameter p3 = new Parameter("maxExpected", false, "100000");
        Parameter p4 = new Parameter("dataselection", false, "useRead,useReadName,useReadHeader,useReadSequence,useMateUId,useReadLength,useReadComplexity,useReadNumberOfMatches,useMatchText,useMatchIgnore,useMatchBitScore,useMatchLength,useMatchTaxonId,useMatchSeedId,useMatchKeggId,useMatchCogId,useMatchExpected,useMatchRefSeq");
        GET_READS_ITERATOR_PARAMETER.add(p);
        GET_READS_ITERATOR_PARAMETER.add(p5);
        GET_READS_ITERATOR_PARAMETER.add(p6);
        GET_READS_ITERATOR_PARAMETER.add(p2);
        GET_READS_ITERATOR_PARAMETER.add(p3);
        GET_READS_ITERATOR_PARAMETER.add(p4);
    }

    private final static String GET_READS_ITERATOR_RETURN_OBJECT = "ReadBlockPage";
    private final static Map<String, Object> GET_READS_ITERATOR_REQUEST = new HashMap<>();

    static {
        GET_READS_ITERATOR_REQUEST.put("Mapping Name", GET_READS_ITERATOR_MAPPING);
        GET_READS_ITERATOR_REQUEST.put("Method", GET_READS_ITERATOR_METHOD);
        GET_READS_ITERATOR_REQUEST.put("Parameter", GET_READS_ITERATOR_PARAMETER);
        GET_READS_ITERATOR_REQUEST.put("Return Object", GET_READS_ITERATOR_RETURN_OBJECT);
        GET_READS_ITERATOR_REQUEST.put("Description", GET_READS_ITERATOR_DESCRIPTION);
    }


    public final static String GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_MAPPING = "getReadsForMultipleClassIds";
    private final static String GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_METHOD = "GET";
    private final static List<Parameter> GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_PARAMETER = new ArrayList<>();
    private final static String GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_DESCRIPTION = "Get a list of reads together with annotations from a dataset assigned to one classification and a number of class identifiers and filtered by parameters such as minScore, maxExpected, and dataselection.";

    static {
        Parameter p = new Parameter("fileId", true, "default");
        Parameter p5 = new Parameter("classification", true, "default");
        Parameter p6 = new Parameter("classIds", true, "default");
        Parameter p2 = new Parameter("minScore", false, "0");
        Parameter p3 = new Parameter("maxExpected", false, "100000");
        Parameter p4 = new Parameter("dataselection", false, "useRead,useReadName,useReadHeader,useReadSequence,useMateUId,useReadLength,useReadComplexity,useReadNumberOfMatches,useMatchText,useMatchIgnore,useMatchBitScore,useMatchLength,useMatchTaxonId,useMatchSeedId,useMatchKeggId,useMatchCogId,useMatchExpected,useMatchRefSeq");
        GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_PARAMETER.add(p);
        GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_PARAMETER.add(p5);
        GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_PARAMETER.add(p6);
        GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_PARAMETER.add(p2);
        GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_PARAMETER.add(p3);
        GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_PARAMETER.add(p4);
    }

    private final static String GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_RETURN_OBJECT = "ReadBlockPage";
    private final static Map<String, Object> GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_REQUEST = new HashMap<>();

    static {
        GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_REQUEST.put("Mapping Name", GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_MAPPING);
        GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_REQUEST.put("Method", GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_METHOD);
        GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_REQUEST.put("Parameter", GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_PARAMETER);
        GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_REQUEST.put("Return Object", GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_RETURN_OBJECT);
        GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_REQUEST.put("Description", GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_DESCRIPTION);
    }


    public final static String GET_CLASSIFICATION_SIZE_MAPPING = "getClassificationSize";
    private final static String GET_CLASSIFICATION_SIZE_METHOD = "GET";
    private final static List<Parameter> GET_CLASSIFICATION_SIZE_PARAMETER = new ArrayList<>();
    private final static String GET_CLASSIFICATION_SIZE_DESCRIPTION = "Retrieve the number of different genes/taxa from a classification for a single dataset.";


    static {
        Parameter p = new Parameter("fileId", true, "default");
        Parameter p2 = new Parameter("classification", true, "default");
        GET_CLASSIFICATION_SIZE_PARAMETER.add(p);
        GET_CLASSIFICATION_SIZE_PARAMETER.add(p2);
    }

    private final static String GET_CLASSIFICATION_SIZE_RETURN_OBJECT = "Integer";
    private final static Map<String, Object> GET_CLASSIFICATION_SIZE_REQUEST = new HashMap<>();

    static {
        GET_CLASSIFICATION_SIZE_REQUEST.put("Mapping Name", GET_CLASSIFICATION_SIZE_MAPPING);
        GET_CLASSIFICATION_SIZE_REQUEST.put("Method", GET_CLASSIFICATION_SIZE_METHOD);
        GET_CLASSIFICATION_SIZE_REQUEST.put("Parameter", GET_CLASSIFICATION_SIZE_PARAMETER);
        GET_CLASSIFICATION_SIZE_REQUEST.put("Return Object", GET_CLASSIFICATION_SIZE_RETURN_OBJECT);
        GET_CLASSIFICATION_SIZE_REQUEST.put("Description", GET_CLASSIFICATION_SIZE_DESCRIPTION);
    }


    public final static String GET_CLASS_SIZE_MAPPING = "getClassSize";
    private final static String GET_CLASS_SIZE_METHOD = "GET";
    private final static List<Parameter> GET_CLASS_SIZE_PARAMETER = new ArrayList<>();
    private final static String GET_CLASS_SIZE_DESCRIPTION = "Retrieve the number of reads assigned to one gene/taxa for one dataset.";

    static {
        Parameter p = new Parameter("fileId", true, "default");
        Parameter p2 = new Parameter("classification", true, "default");
        Parameter p3 = new Parameter("classId", true, "default");
        GET_CLASS_SIZE_PARAMETER.add(p);
        GET_CLASS_SIZE_PARAMETER.add(p2);
        GET_CLASS_SIZE_PARAMETER.add(p3);
    }

    private final static String GET_CLASS_SIZE_RETURN_OBJECT = "Integer";
    private final static Map<String, Object> GET_CLASS_SIZE_REQUEST = new HashMap<>();

    static {
        GET_CLASS_SIZE_REQUEST.put("Mapping Name", GET_CLASS_SIZE_MAPPING);
        GET_CLASS_SIZE_REQUEST.put("Method", GET_CLASS_SIZE_METHOD);
        GET_CLASS_SIZE_REQUEST.put("Parameter", GET_CLASS_SIZE_PARAMETER);
        GET_CLASS_SIZE_REQUEST.put("Return Object", GET_CLASS_SIZE_RETURN_OBJECT);
        GET_CLASS_SIZE_REQUEST.put("Description", GET_CLASS_SIZE_DESCRIPTION);
    }


    public final static String GET_FIND_ALL_READS_ITERATOR_MAPPING = "getFindAllReadsIterator";
    private final static String GET_FIND_ALL_READS_ITERATOR_METHOD = "GET";
    private final static List<Parameter> GET_FIND_ALL_READS_ITERATOR_PARAMETER = new ArrayList<>();
    private final static String GET_FIND_ALL_READS_ITERATOR_DESCRIPTION = "Retrieve reads of a datasets in which the string in the regular expression is found.";

    static {
        Parameter p = new Parameter("fileId", true, "default");
        Parameter p2 = new Parameter("regEx", true, "0");
        Parameter p4 = new Parameter("findSelection", false, "useMatchText,useReadHeader,useReadName,useReadSequence");
        GET_FIND_ALL_READS_ITERATOR_PARAMETER.add(p);
        GET_FIND_ALL_READS_ITERATOR_PARAMETER.add(p2);
        GET_FIND_ALL_READS_ITERATOR_PARAMETER.add(p4);
    }

    private final static String GET_FIND_ALL_READS_ITERATOR_RETURN_OBJECT = "ReadBlockPage";
    private final static Map<String, Object> GET_FIND_ALL_READS_ITERATOR_REQUEST = new HashMap<>();

    static {
        GET_FIND_ALL_READS_ITERATOR_REQUEST.put("Mapping Name", GET_FIND_ALL_READS_ITERATOR_MAPPING);
        GET_FIND_ALL_READS_ITERATOR_REQUEST.put("Method", GET_FIND_ALL_READS_ITERATOR_METHOD);
        GET_FIND_ALL_READS_ITERATOR_REQUEST.put("Parameter", GET_FIND_ALL_READS_ITERATOR_PARAMETER);
        GET_FIND_ALL_READS_ITERATOR_REQUEST.put("Return Object", GET_FIND_ALL_READS_ITERATOR_RETURN_OBJECT);
        GET_FIND_ALL_READS_ITERATOR_REQUEST.put("Description", GET_FIND_ALL_READS_ITERATOR_DESCRIPTION);
    }


    public final static String GET_NUMBER_OF_READS_MAPPING = "getNumberOfReads";
    private final static String GET_NUMBER_OF_READS_METHOD = "GET";
    private final static List<Parameter> GET_NUMBER_OF_READS_PARAMETER = new ArrayList<>();
    private final static String GET_NUMBER_OF_READS_DESCRIPTION = "Get number of reads for a datasets";

    static {
        Parameter p = new Parameter("fileId", true, "default");
        GET_NUMBER_OF_READS_PARAMETER.add(p);
    }

    private final static String GET_NUMBER_OF_READS_RETURN_OBJECT = "Integer";
    private final static Map<String, Object> GET_NUMBER_OF_READS_REQUEST = new HashMap<>();

    static {
        GET_NUMBER_OF_READS_REQUEST.put("Mapping Name", GET_NUMBER_OF_READS_MAPPING);
        GET_NUMBER_OF_READS_REQUEST.put("Method", GET_NUMBER_OF_READS_METHOD);
        GET_NUMBER_OF_READS_REQUEST.put("Parameter", GET_NUMBER_OF_READS_PARAMETER);
        GET_NUMBER_OF_READS_REQUEST.put("Return Object", GET_NUMBER_OF_READS_RETURN_OBJECT);
        GET_NUMBER_OF_READS_REQUEST.put("Description", GET_NUMBER_OF_READS_DESCRIPTION);
    }


    public final static String GET_NUMBER_OF_MATCHES_MAPPING = "getNumberOfMatches";
    private final static String GET_NUMBER_OF_MATCHES_METHOD = "GET";
    private final static List<Parameter> GET_NUMBER_OF_MATCHES_PARAMETER = new ArrayList<>();
    private final static String GET_NUMBER_OF_MATCHES_DESCRIPTION = "Get number of matches for a datasets";

    static {
        Parameter p = new Parameter("fileId", true, "default");
        GET_NUMBER_OF_MATCHES_PARAMETER.add(p);
    }

    private final static String GET_NUMBER_OF_MATCHES_RETURN_OBJECT = "Integer";
    private final static Map<String, Object> GET_NUMBER_OF_MATCHES_REQUEST = new HashMap<>();

    static {
        GET_NUMBER_OF_MATCHES_REQUEST.put("Mapping Name", GET_NUMBER_OF_MATCHES_MAPPING);
        GET_NUMBER_OF_MATCHES_REQUEST.put("Method", GET_NUMBER_OF_MATCHES_METHOD);
        GET_NUMBER_OF_MATCHES_REQUEST.put("Parameter", GET_NUMBER_OF_MATCHES_PARAMETER);
        GET_NUMBER_OF_MATCHES_REQUEST.put("Return Object", GET_NUMBER_OF_MATCHES_RETURN_OBJECT);
        GET_NUMBER_OF_MATCHES_REQUEST.put("Description", GET_NUMBER_OF_MATCHES_DESCRIPTION);
    }


    private final static String HELP_MAPPING = "help";
    private final static String HELP_METHOD = "GET";
    private final static List<Parameter> HELP_PARAMETER = new ArrayList<>();
    private final static String HELP_DESCRIPTION = "This helpful page";

    static {
    }

    private final static String HELP_RETURN_OBJECT = "Integer";
    private final static Map<String, Object> HELP_REQUEST = new HashMap<>();

    static {
        HELP_REQUEST.put("Mapping Name", HELP_MAPPING);
        HELP_REQUEST.put("Method", HELP_METHOD);
        HELP_REQUEST.put("Parameter", HELP_PARAMETER);
        HELP_REQUEST.put("Return Object", HELP_RETURN_OBJECT);
        HELP_REQUEST.put("Description", HELP_DESCRIPTION);
    }

    public static final String GET_READ_MAPPING = "getRead";
    private final static String GET_READ_METHOD = "GET";
    private final static List<Parameter> GET_READ_PARAMETER = new ArrayList<>();
    private final static String GET_READ_DESCRIPTION = "Get a single read.";

    static {
        Parameter p = new Parameter("fileId", true, "default");
        Parameter p5 = new Parameter("readUid", true, "0");
        Parameter p2 = new Parameter("minScore", false, "0");
        Parameter p3 = new Parameter("maxExpected", false, "100000");
        Parameter p4 = new Parameter("dataselection", false, "useRead,useReadName,useReadHeader,useReadSequence,useMateUId,useReadLength,useReadComplexity,useReadNumberOfMatches,useMatchText,useMatchIgnore,useMatchBitScore,useMatchLength,useMatchTaxonId,useMatchSeedId,useMatchKeggId,useMatchCogId,useMatchExpected,useMatchRefSeq");
        GET_READ_PARAMETER.add(p);
        GET_READ_PARAMETER.add(p5);
        GET_READ_PARAMETER.add(p2);
        GET_READ_PARAMETER.add(p3);
        GET_READ_PARAMETER.add(p4);
    }

    private final static String GET_READ_RETURN_OBJECT = "ReadBlockServer";
    private final static Map<String, Object> GET_READ_REQUEST = new HashMap<>();

    static {
        GET_READ_REQUEST.put("Mapping Name", GET_READ_MAPPING);
        GET_READ_REQUEST.put("Method", GET_READ_METHOD);
        GET_READ_REQUEST.put("Parameter", GET_READ_PARAMETER);
        GET_READ_REQUEST.put("Return Object", GET_READ_RETURN_OBJECT);
        GET_READ_REQUEST.put("Description", GET_READ_DESCRIPTION);
    }


    public static final String LOAD_READ_PAGE_MAPPING = "loadPagedReads";
    private final static String LOAD_READ_PAGE_METHOD = "GET";
    private final static List<Parameter> LOAD_READ_PAGE_PARAMETER = new ArrayList<>();
    private final static String LOAD_READ_PAGE_DESCRIPTION = "Load a cached read page. You get the key for this read page with one of the iterators.";

    static {
        Parameter p = new Parameter("pageId", true, "default");
        LOAD_READ_PAGE_PARAMETER.add(p);
    }

    private final static String LOAD_READ_PAGE_RETURN_OBJECT = "ReadBlockPage";
    private final static Map<String, Object> LOAD_READ_PAGE_REQUEST = new HashMap<>();

    static {
        LOAD_READ_PAGE_REQUEST.put("Mapping Name", LOAD_READ_PAGE_MAPPING);
        LOAD_READ_PAGE_REQUEST.put("Method", LOAD_READ_PAGE_METHOD);
        LOAD_READ_PAGE_REQUEST.put("Parameter", LOAD_READ_PAGE_PARAMETER);
        LOAD_READ_PAGE_REQUEST.put("Return Object", LOAD_READ_PAGE_RETURN_OBJECT);
        LOAD_READ_PAGE_REQUEST.put("Description", LOAD_READ_PAGE_DESCRIPTION);
    }

    public static final Map<String, Map<String, Object>> REQUESTS = new HashMap<>();

    static {
        REQUESTS.put(HELP_MAPPING, HELP_REQUEST);
        REQUESTS.put(GET_UID_MAPPING, GET_UID_REQUEST);
        REQUESTS.put(GET_NUMBER_OF_MATCHES_MAPPING, GET_NUMBER_OF_MATCHES_REQUEST);
        REQUESTS.put(GET_NUMBER_OF_READS_MAPPING, GET_NUMBER_OF_READS_REQUEST);
        REQUESTS.put(GET_FIND_ALL_READS_ITERATOR_MAPPING, GET_FIND_ALL_READS_ITERATOR_REQUEST);
        REQUESTS.put(GET_CLASS_SIZE_MAPPING, GET_CLASS_SIZE_REQUEST);
        REQUESTS.put(GET_CLASSIFICATION_SIZE_MAPPING, GET_CLASSIFICATION_SIZE_REQUEST);
        REQUESTS.put(GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_MAPPING, GET_READS_ITERATOR_FOR_MULTIPLE_CLASSIDS_REQUEST);

        REQUESTS.put(GET_READS_ITERATOR_MAPPING, GET_READS_ITERATOR_REQUEST);
        REQUESTS.put(GET_ALL_READS_ITERATOR_MAPPING, GET_ALL_READS_ITERATOR_REQUEST);
        REQUESTS.put(GET_CLASSIFICATIONBLOCK_MAPPING, GET_CLASSIFICATIONBLOCK_REQUEST);
        REQUESTS.put(GET_ALL_CLASSIFICATION_NAMES_MAPPING, GET_ALL_CLASSIFICATION_NAMES_REQUEST);

        REQUESTS.put(GET_AUXILIARY_MAPPING, GET_AUXILIARY_REQUEST);
        REQUESTS.put(LIST_DATASETS_MAPPING, LIST_DATASETS_REQUEST);
        REQUESTS.put(IS_READ_ONLY_MAPPING, IS_READ_ONLY_REQUEST);
        REQUESTS.put(GET_READ_MAPPING, GET_READ_REQUEST);
        REQUESTS.put(LOAD_READ_PAGE_MAPPING, LOAD_READ_PAGE_REQUEST);

        REQUESTS.put(GET_LOG_MAPPING, GET_LOG_REQUEST);
        REQUESTS.put(ADD_USER_MAPPING, ADD_USER_REQUEST);
        REQUESTS.put(REMOVE_USER_MAPPING, REMOVE_USER_REQUEST);
        REQUESTS.put(LIST_USERS_MAPPING, LIST_USERS_REQUEST);
        REQUESTS.put(UPDATE_DATASETS_MAPPING, UPDATE_DATASETS_REQUEST);
    }


}
