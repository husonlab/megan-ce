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
package rusch.megan5client.Tests;


import jloda.util.Single;
import megan.data.*;
import rusch.megan5client.RMADataset;
import rusch.megan5client.connector.Megan5ServerConnector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Connector test Class. Last test performed like this:
 * <p/>
 * Instance of Connector took (in ms):288
 * Loading dataset info from server (Datasets 784)(in ms): 534
 * Loading dataset UID, and isReadonly)(in ms): 16
 * Loading readblocks(Allreadblockit) (number of readblocks 2496)(in ms): 9012
 * Loading classblock)(in ms): 25
 * Loading readblocks readblockit (number of blocks 104)(in ms): 659
 * Loading Multiple class (Number of reads 131)(in ms): 664
 * Accessing multiple readblocks one by one (number of accessed blocks 131)(in ms): 2893
 * Get classnames(in ms): 14
 * Get classSize (in ms): 17
 * Searching in remote file for "Bacteria"(number of blocks 2469)(in ms): 8486
 * Get number of matches/reads(in ms):34
 * Get Auxblock (in ms):20
 *
 * @author Hans-Joachim Ruscheweyh
 * 11:33:17 AM - Oct 28, 2014
 */
public class TestRun {


    public static void main(String[] args) throws IOException {
        String url = "http://meganserver2.informatik.uni-tuebingen.de/MeganServer/";
        //String url = "http://localhost:8080/Megan5ServerNew/";
        String user = "Hans";
        String password = "Ruscheweyh";
        long time = System.currentTimeMillis();
        Megan5ServerConnector connector = new Megan5ServerConnector(url, user, password);
        long now = System.currentTimeMillis();
        System.out.println("Instance of Connector took (in ms):" + (now - time));
        time = System.currentTimeMillis();
        RMADataset[] datasets = connector.getAvailiableDatasets();
        now = System.currentTimeMillis();
        System.out.println(String.format("Loading dataset info from server (Datasets %s)(in ms): %s", datasets.length, (now - time)));
        connector.setFile(String.valueOf(datasets[datasets.length - 1].getDatasetUid()));
        System.out.println(datasets[datasets.length - 1].getDatasetName());
        time = System.currentTimeMillis();

        long uid = connector.getUId();
        boolean readOnly = connector.isReadOnly();
        now = System.currentTimeMillis();
        System.out.println(String.format("Loading dataset UID, and isReadonly)(in ms): %s", (now - time)));

        time = System.currentTimeMillis();
        try (IReadBlockIterator it = connector.getAllReadsIterator(500, 0, true, false)) {
            int pos = 0;
            while (it.hasNext()) {
                IReadBlock block = it.next();
                pos++;
                if (pos % 5000 == 0) {
                    long t = System.currentTimeMillis() - time;
                    double rpms = (double) pos / ((double) t / 1000);
                    System.out.println(String.format("%s Reads in %s. This means %s reads per second.", pos, t, rpms));
                }
                for (IMatchBlock mblock : block.getMatchBlocks()) {
                    //System.out.println(mblock);
                }
            }
            now = System.currentTimeMillis();
            System.out.println(String.format("Loading readblocks(Allreadblockit) (number of readblocks %s)(in ms): %s", pos, (now - time)));
        }


        time = System.currentTimeMillis();
        IClassificationBlock classblock = connector.getClassificationBlock("Taxonomy");
        now = System.currentTimeMillis();
        System.out.println(String.format("Loading classblock)(in ms): %s", (now - time)));

        {
            time = System.currentTimeMillis();
            try (IReadBlockIterator it = connector.getReadsIterator("Taxonomy", 1735, 200, 0, true, false)) {
                int pos = 0;
                while (it.hasNext()) {
                    IReadBlock block = it.next();
                    pos++;
                    for (IMatchBlock mblock : block.getMatchBlocks()) {
                        //System.out.println(mblock);
                    }
                }
                now = System.currentTimeMillis();
                System.out.println(String.format("Loading readblocks readblockit (number of blocks %s)(in ms): %s", pos, (now - time)));
            }
        }
        List<Long> readUids = new ArrayList<>();
        {
            time = System.currentTimeMillis();
            List<Integer> classIds = new ArrayList<>();
            classIds.add(1735);
            classIds.add(-1);
            try (IReadBlockIterator it = connector.getReadsIteratorForListOfClassIds("Taxonomy", classIds, 200, 0, true, false)) {
                int pos = 0;
                while (it.hasNext()) {
                    IReadBlock block = it.next();
                    readUids.add(block.getUId());
                    pos++;
                    for (IMatchBlock mblock : block.getMatchBlocks()) {
                        //System.out.println(mblock);
                    }
                }
                now = System.currentTimeMillis();
                System.out.println(String.format("Loading Multiple class (Number of reads %s)(in ms): %s", pos, (now - time)));
            }

        }
        {
            time = System.currentTimeMillis();
            IReadBlockGetter getter = connector.getReadBlockGetter(200, 0, true, false);
            int pos = 0;
            for (Long readuid : readUids) {
                getter.getReadBlock(readuid);
                pos++;
            }
            now = System.currentTimeMillis();
            System.out.println(String.format("Accessing multiple readblocks one by one (number of accessed blocks %s)(in ms): %s", pos, (now - time)));

        }

        {
            time = System.currentTimeMillis();
            String[] classNames = connector.getAllClassificationNames();
            now = System.currentTimeMillis();
            System.out.println(String.format("Get classnames(in ms): %s", (now - time)));
        }

        {
            time = System.currentTimeMillis();
            int classSize = connector.getClassSize("Taxonomy", 2);
            now = System.currentTimeMillis();
            System.out.println(String.format("Get classSize (in ms): %s", (now - time)));

        }
        {
            FindSelection findsel = new FindSelection();
            findsel.useMatchText = true;
            time = System.currentTimeMillis();
            try (IReadBlockIterator it = connector.getFindAllReadsIterator("Bacteria", findsel, new Single<>(false))) {
                int pos = 0;
                while (it.hasNext()) {
                    IReadBlock block = it.next();
                    pos++;
                }
                now = System.currentTimeMillis();
                System.out.println(String.format("Searching in remote file for \"Bacteria\"(number of blocks %s)(in ms): %s", pos, (now - time)));
            }
        }

        {
            time = System.currentTimeMillis();
            connector.getNumberOfReads();
            connector.getNumberOfMatches();
            now = System.currentTimeMillis();
            System.out.println(String.format("Get number of matches/reads(in ms):%s", (now - time)));
        }

        {
            time = System.currentTimeMillis();
            Map<String, byte[]> aux = connector.getAuxiliaryData();
            byte[] userstate = aux.get("USER_STATE");
            now = System.currentTimeMillis();
            System.out.println(String.format("Get Auxblock (in ms):%s", (now - time)));
        }
    }


}
