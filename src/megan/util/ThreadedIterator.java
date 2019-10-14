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
package megan.util;

import jloda.util.Basic;
import jloda.util.ICloseableIterator;
import jloda.util.Single;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Use this to run an getLetterCodeIterator in a separate thread. Typical use is a getLetterCodeIterator that reads from a file.
 * Daniel Huson, 3.2012
 */
public class ThreadedIterator<T> implements ICloseableIterator<T> {
    private final Iterator<T> iterator;
    private final Future future;
    private final BlockingQueue<Object> queue;
    private static final Object sentinel = new Object();
    private boolean done = false;
    private Object next = null;
    private final boolean isClosable;
    private final Single<Boolean> closed = new Single<>(false); // need this to stop

    private static final boolean enable = true;

    /**
     * default constructor
     *
     * @param it
     */
    public ThreadedIterator(Iterator<T> it) {
        this(it, 10000);
    }

    /**
     * constructor
     *
     * @param it
     * @param bufferSize maximum number of read ahead by getLetterCodeIterator
     */
    public ThreadedIterator(Iterator<T> it, int bufferSize) {
        this.iterator = it;
        isClosable = (it instanceof ICloseableIterator);

        if (enable) {
            queue = new LinkedBlockingQueue<>(bufferSize);
            future = Executors.newSingleThreadExecutor().submit(new Runnable() {
                public void run() {
                    try {
                        while (iterator.hasNext()) {
                            T item;
                            try {
                                synchronized (closed) {
                                    if (closed.get())
                                        break;
                                    item = iterator.next();
                                }
                                if (item == null)
                                    break;
                                queue.put(item);
                            } catch (Exception ex) {
                                Basic.caught(ex);
                                break;
                            }
                        }
                        queue.put(sentinel);
                    } catch (InterruptedException e) {
                        // was interrupted while trying to put an element, erase the queue and then add the sentinel
                        future.cancel(true);
                        queue.clear();
                        queue.add(sentinel);
                    }
                }
            });
        } else {
            queue = null;
            future = null;
        }
    }

    /**
     * has next? Prefetches the next item
     *
     * @return true, if has next
     */
    public boolean hasNext() { // we get the next element when this is called.
        if (enable) {
            if (done)
                return false;
            if (next != null) // can't be sentinel
                return true;
            try {
                next = queue.take();
            } catch (InterruptedException e) {
                done = true;
                next = null;
                return false;
            }
            if (next == sentinel) {
                done = true;
                next = null;
                return false;
            } else
                return true;
        } else
            return iterator.hasNext();
    }

    /**
     * gets next element of getLetterCodeIterator
     *
     * @return true
     */
    public T next() {
        if (enable) {
            if (done) {
                return null;
            }
            if (next == null)
                hasNext(); // if next null, was already taken, so get next by calling hasNext
            if (next != null) {
                T result = (T) next;
                next = null;
                // System.err.println("result: "+result.toString()+" pos: "+position+" numberOfBytes: "+numberOfBytes);
                return result;
            } else {
                return null;
            }
        } else
            return iterator.next();
    }

    /**
     * shutdown thread used by getLetterCodeIterator
     */
    public void close() throws IOException {
        if (enable) {
            synchronized (closed) {
                closed.set(true);
                if (!future.isDone()) {
                    done = true;
                    future.cancel(true);
                }
                if (isClosable) {
                    ((ICloseableIterator) iterator).close();
                }
            }
        } else {
            if (isClosable) {
                ((ICloseableIterator) iterator).close();
            }
        }
    }

    /**
     * gets the maximum progress value
     *
     * @return maximum progress value
     */
    public long getMaximumProgress() {
        if (isClosable)
            return ((ICloseableIterator) iterator).getMaximumProgress();
        else
            return -1;
    }

    /**
     * gets the current progress value
     *
     * @return current progress value
     */
    public long getProgress() {
        if (isClosable)
            return ((ICloseableIterator) iterator).getProgress();
        else
            return -1;
    }

    /**
     * not implemented
     */
    public void remove() {
    }

    /**
     * gets the underlying getLetterCodeIterator
     *
     * @return getLetterCodeIterator
     */
    public Iterator<T> iterator() {
        return iterator;
    }


    /**
     * is this getLetterCodeIterator closable?
     *
     * @return true, if closable
     */
    public boolean isClosable() {
        return isClosable;
    }
}
