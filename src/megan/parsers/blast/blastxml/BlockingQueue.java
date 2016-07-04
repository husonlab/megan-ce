/*
 *  Copyright (C) 2016 Daniel H. Huson
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
package megan.parsers.blast.blastxml;

import jloda.util.Basic;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * a blocking queue of capacity 1000 and a done flag
 * Daniel Huson, 12.2011
 */
public class BlockingQueue<T> {
    private final ArrayBlockingQueue<T> queue;
    private boolean inputDone = false;

    public BlockingQueue() {
        queue = new ArrayBlockingQueue<>(1000);
    }

    /**
     * notify that we have finished adding input
     */
    public void setInputDone() {
        synchronized (this) {
            this.inputDone = true;
        }
    }

    /**
     * is output done?
     * @return true, if no more output available
     */
    public boolean isOutputDone() {
        synchronized (this) {
            return inputDone && queue.size() == 0;
        }
    }

    public void abort(T sentinel) {
        queue.add(sentinel);
    }

    /**
     * take the next object of the queue, block if queue is empty and input is not done
     * @return next object or null
     */
    public T take() {
        if (isOutputDone())
            return null;
        try {
            return queue.take();
        } catch (InterruptedException e) {
            Basic.caught(e);
            return null;
        }
    }

    /**
     * put the next object into the queue, or block, if queue is full
     * @param object
     */
    public void put(T object) throws InterruptedException {
        queue.put(object);
    }

    public int size() {
        return queue.size();
    }
}
