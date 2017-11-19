/*
 *  Copyright (C) 2017 Daniel H. Huson
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

package megan.tools;

import jloda.export.gifEncode.Gif89Encoder;
import jloda.util.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Extracts a color table from an image file
 * Created by huson on 1/29/16.
 */
public class ColorTableExtractor {
    /**
     * main
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     * @throws CanceledException
     */
    public static void main(String[] args) throws UsageException, IOException, CanceledException {
        if (args.length == 0 && System.getProperty("user.name").equals("huson"))
            //args = new String[]{"-i","/Users/huson/Desktop/color-tables/Many.png","-r","-t","30"};
            args = new String[]{"-i", "/Users/huson/Desktop/color-tables/CeMeT.png", "-t", "3"};

        try {
            long start = System.currentTimeMillis();
            (new ColorTableExtractor()).run(args);
            System.err.println("Time: " + ((System.currentTimeMillis() - start) / 1000) + "s");
            System.exit(0);
        } catch (Exception ex) {
            Basic.caught(ex);
            System.exit(1);
        }
    }

    /**
     * run
     *
     * @param args
     * @throws UsageException
     * @throws IOException
     * @throws CanceledException
     */
    public void run(String[] args) throws UsageException, IOException, CanceledException {
        final ArgsOptions options = new ArgsOptions(args, this, ProgramProperties.getProgramName(), "Extract colors from image file");
        options.setVersion(ProgramProperties.getProgramVersion());
        options.setLicense("Copyright (C) 2017 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.");
        options.setAuthors("Daniel H. Huson");

        final String inputFile = options.getOptionMandatory("-i", "in", "Input file", "");
        final String iconFile = options.getOption("-ic", "icon", "Output icon file", Basic.replaceFileSuffix(inputFile, "_16.gif"));
        final boolean noWhite = options.getOption("-nw", "noWhite", "No white color", true);
        final boolean noBlack = options.getOption("-nb", "noBlack", "No black color", true);
        final boolean randomize = options.getOption("-r", "random", "Report colors in random order", false);
        final int threshold = options.getOption("-t", "threshold", "Similarity threshold", 6);
        options.done();

        Collection<Triplet<Integer, Integer, Color>> colors = new TreeSet<>(new Comparator<Triplet<Integer, Integer, Color>>() {
            @Override
            public int compare(Triplet<Integer, Integer, Color> t1, Triplet<Integer, Integer, Color> t2) {
                if (t1.getFirst() < t2.getFirst())
                    return -1;
                else if (t1.getFirst() > t2.getFirst())
                    return 1;
                else if (t1.getSecond() < t2.getSecond())
                    return -1;
                else if (t1.getSecond() > t2.getSecond())
                    return 1;
                else
                    return 0;
            }
        });

        {
            final BufferedImage image = ImageIO.read(new File(inputFile));
            for (int i = 0; i < image.getWidth(); i++) {
                for (int j = 0; j < image.getHeight(); j++) {
                    Color color = new Color(image.getRGB(i + image.getMinX(), j + image.getMinY()));
                    colors.add(new Triplet<>(i, j, color));
                }
            }
        }

        System.err.println("Found: " + colors.size());

        Triplet<Integer, Integer, Color>[] array = colors.toArray(new Triplet[colors.size()]);
        colors.clear();
        BitSet dead = new BitSet();
        for (int i = 0; i < array.length; i++) {
            Triplet<Integer, Integer, Color> a = array[i];
            boolean ok = ((a.getThird().getRGB() & 0xffffff) != 0);
            for (int j = 0; ok && j < i; j++) {
                Triplet<Integer, Integer, Color> b = array[j];
                if (!dead.get(j) && similar(a.getThird(), b.getThird(), threshold)) {
                    ok = false;
                    break;
                }
            }
            if (ok && !(noBlack && similar(a.getThird(), Color.BLACK, threshold)) && !(noWhite && similar(a.getThird(), Color.WHITE, threshold)))
                colors.add(a);
            else
                dead.set(i);
        }

        System.err.println("Unique: " + colors.size());

        if (randomize) {
            Iterator<Triplet<Integer, Integer, Color>> it = Basic.randomize(colors.iterator(), 10);
            colors = new ArrayList<>(colors.size()); // must be list, not sorted set
            while (it.hasNext()) {
                colors.add(it.next());
            }
        }

        System.out.print(Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(inputFile), ";") + colors.size() + ";");
        for (Triplet<Integer, Integer, Color> color : colors) {
            System.out.print("0x" + Integer.toHexString(color.getThird().getRGB() & 0xffffff) + ";");
        }
        System.out.println();

        // make an icon:
        {
            final BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);

            int patchSize = 16 / (int) (1 + Math.sqrt(colors.size()));

            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++)
                    image.setRGB(x, y, Color.LIGHT_GRAY.getRGB());

            int row = 0;
            int col = 0;
            for (Triplet<Integer, Integer, Color> triplet : colors) {
                for (int y = 0; y < patchSize; y++) {

                    if (row + patchSize >= 16) {
                        row = 0;
                        col += patchSize;
                    }

                    for (int x = 0; x < patchSize; x++) {
                        if (row + x < 16 && col + y < 16)
                            image.setRGB(row + x, col + y, triplet.getThird().getRGB());
                    }
                }
                row += patchSize;
            }
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(iconFile))) {
                Gif89Encoder enc = new Gif89Encoder(image);
                enc.setTransparentIndex(-1);
                enc.encode(bos);
            }
        }
    }

    private static boolean similar(Color a, Color b, int threshold) {
        return Math.abs(a.getRed() - b.getRed()) < threshold && Math.abs(a.getGreen() - b.getGreen()) < threshold && Math.abs(a.getBlue() - b.getBlue()) < threshold;
    }
}
