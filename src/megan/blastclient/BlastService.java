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

package megan.blastclient;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import jloda.util.Pair;

import java.util.Collection;
import java.util.Collections;

/**
 * blast service
 * Created by huson on 1/18/17.
 */
public class BlastService extends Service<String> {
    private Collection<Pair<String, String>> queries;
    private RemoteBlastClient.BlastProgram program;
    private String database;

    @Override
    protected Task<String> createTask() {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                final RemoteBlastClient remoteBlastClient = new RemoteBlastClient();
                remoteBlastClient.setDatabase(getDatabase());
                remoteBlastClient.setProgram(getProgram());

                updateMessage("Contacting NCBI...");
                remoteBlastClient.startRemoteSearch(getQueries());

                long estimatedTime = 2000 * remoteBlastClient.getEstimatedTime(); // double...
                final long startTime = System.currentTimeMillis();
                updateProgress(0, estimatedTime);
                updateMessage("Request id: " + remoteBlastClient.getRequestId() + "\nEstimated time: " + (estimatedTime / 1100) + "s\nSearching...\n");

                RemoteBlastClient.Status status = null;
                do {
                    if (status != null) {
                        Thread.sleep(5000);
                    }
                    status = remoteBlastClient.getRemoteStatus();
                    final long time = System.currentTimeMillis() - startTime;
                    while (0.9 * time > estimatedTime)
                        estimatedTime *= 1.2;
                    updateProgress(time, estimatedTime);
                    if (isCancelled()) {
                        break;
                    }
                }
                while (status == RemoteBlastClient.Status.searching);
                updateMessage("Search completed");

                final StringBuilder buf = new StringBuilder();
                switch (status) {
                    case hitsFound:
                        for (String line : remoteBlastClient.getRemoteAlignments()) {
                            buf.append(line).append("\n");
                        }
                        break;
                    case noHitsFound:
                        updateMessage("No hits found");
                        break;
                    case failed:
                        updateMessage("Failed");
                        System.err.println("Remote BLAST failed. Lookup details for RID=" + remoteBlastClient.getRequestId() + " on NCBI BLAST website https://blast.ncbi.nlm.nih.gov");
                        break;
                    default:
                        updateMessage("Status: " + status);
                }
                return buf.toString();
            }
        };
    }

    private Collection<Pair<String, String>> getQueries() {
        return queries;
    }

    public void setQueries(Collection<Pair<String, String>> queries) {
        this.queries = queries;
    }

    public void setQuery(String name, String sequence) {
        setQueries(Collections.singletonList(new Pair<>(name, sequence)));
    }

    private RemoteBlastClient.BlastProgram getProgram() {
        return program;
    }

    public void setProgram(RemoteBlastClient.BlastProgram program) {
        this.program = program;
    }

    private String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}
