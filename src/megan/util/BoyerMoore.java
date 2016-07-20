/*
 * Copyright (C) 2002-2015 Robert Sedgewick and Kevin Wayne.
 */
package megan.util;
/******************************************************************************
 * Compilation:  javac BoyerMoore.java
 * Execution:    java BoyerMoore pattern text
 * Dependencies: StdOut.java
 * <p>
 * Reads in two strings, the pattern and the input text, and
 * searches for the pattern in the input text using the
 * bad-character rule part of the Boyer-Moore algorithm.
 * (does not implement the strong good suffix rule)
 * <p>
 * % java BoyerMoore abracadabra abacadabrabracabracadabrabrabracad
 * text:    abacadabrabracabracadabrabrabracad
 * pattern:               abracadabra
 * <p>
 * % java BoyerMoore rab abacadabrabracabracadabrabrabracad
 * text:    abacadabrabracabracadabrabrabracad
 * pattern:         rab
 * <p>
 * % java BoyerMoore bcara abacadabrabracabracadabrabrabracad
 * text:    abacadabrabracabracadabrabrabracad
 * pattern:                                   bcara
 * <p>
 * % java BoyerMoore rabrabracad abacadabrabracabracadabrabrabracad
 * text:    abacadabrabracabracadabrabrabracad
 * pattern:                        rabrabracad
 * <p>
 * % java BoyerMoore abacad abacadabrabracabracadabrabrabracad
 * text:    abacadabrabracabracadabrabrabracad
 * pattern: abacad
 ******************************************************************************/

import java.util.Iterator;

/**
 *  The <tt>BoyerMoore</tt> class finds the first occurrence of a pattern string
 *  in a text string.
 *  <p>
 *  This implementation uses the Boyer-Moore algorithm (with the bad-character
 *  rule, but not the strong good suffix rule).
 *  <p>
 *  For additional documentation,
 *  see <a href="http://algs4.cs.princeton.edu/53substring">Section 5.3</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 */
public class BoyerMoore {
    private final int R;     // the radix
    private final int[] right;     // the bad-character skip array

    private byte[] pattern;  // store the pattern as a byte array
    private String pat;      // or as a string

    /**
     * Preprocesses the pattern string.
     *
     * @param pat the pattern string
     */
    public BoyerMoore(String pat) {
        this.R = 256;
        this.pat = pat;

        // position of rightmost occurrence of c in the pattern
        right = new int[R];
        for (int c = 0; c < R; c++)
            right[c] = -1;
        for (int j = 0; j < pat.length(); j++)
            right[pat.charAt(j)] = j;
    }

    /**
     * Preprocesses the pattern string.
     *
     * @param pattern the pattern string
     * @param R the alphabet size
     */
    public BoyerMoore(byte[] pattern, int offset, int length, int R) {
        this.R = R;
        this.pattern = new byte[length];
        System.arraycopy(pattern, offset, this.pattern, 0, length);

        // position of rightmost occurrence of c in the pattern
        right = new int[R];
        for (int c = 0; c < R; c++)
            right[c] = -1;
        for (int j = 0; j < pattern.length; j++)
            right[pattern[j]] = j;
    }


    /**
     * Returns the index of the first occurrence of the pattern string
     * in the text string.
     *
     * @param  txt the text string
     * @return the index of the first occurrence of the pattern string
     *         in the text string; -1 if no such match
     */
    public int search(String txt) {
        int M = pat.length();
        int N = txt.length();
        int skip;
        for (int i = 0; i <= N - M; i += skip) {
            skip = 0;
            for (int j = M - 1; j >= 0; j--) {
                if (pat.charAt(j) != txt.charAt(i + j)) {
                    skip = Math.max(1, j - right[txt.charAt(i + j)]);
                    break;
                }
            }
            if (skip == 0) return i;    // found
        }
        return N;                       // not found
    }

    /**
     * Returns the index of the first occurrrence of the pattern string
     * in the text string.
     *
     * @param text the text string
     * @return the index of the first occurrence of the pattern string
     * in the text string; N if no such match
     */
    public int search(byte[] text) {
        int M = pattern.length;
        int N = text.length;
        int skip;
        for (int i = 0; i <= N - M; i += skip) {
            skip = 0;
            for (int j = M - 1; j >= 0; j--) {
                if (pattern[j] != text[i + j]) {
                    skip = Math.max(1, j - right[text[i + j]]);
                    break;
                }
            }
            if (skip == 0) return i;    // found
        }
        return N;                       // not found
    }

    /**
     * Returns the index of the first occurrrence of the pattern string
     * in the text string.
     *
     * @param text  the text string
     * @param start starting positions
     * @return the index of the first occurrence of the pattern string
     * in the text string; N if no such match
     */
    public int search(byte[] text, int start) {
        int M = pattern.length;
        int N = text.length;
        int skip;
        for (int i = start; i <= N - M; i += skip) {
            skip = 0;
            for (int j = M - 1; j >= 0; j--) {
                if (pattern[j] != text[i + j]) {
                    skip = Math.max(1, j - right[text[i + j]]);
                    break;
                }
            }
            if (skip == 0) return i;    // found
        }
        return N;                       // not found
    }

    /**
     * get an interator over all occurrences
     *
     * @param text
     * @return iterator
     */
    public Iterator<Integer> iterator(final byte[] text) {
        return new Iterator<Integer>() {
            private int next;

            {
                next = search(text, 0);
            }

            @Override
            public boolean hasNext() {
                return next < text.length;
            }

            @Override
            public Integer next() {
                int result = next;
                if (result < text.length)
                    next = search(text, result + 1);
                return result;
            }
        };
    }

}
