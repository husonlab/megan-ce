/*
 * DAAModifier.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package megan.daa.io;


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * modifies a DAA file
 * Daniel Huson, 8.2105
 */
public class DAAModifier {
    /**
     * remove all data added by MEGAN
     */
    public static void removeAllMEGANData(String fileName) throws IOException {
        final var header = new DAAHeader(fileName);
        header.load();

        var newFileSize = -1L;
        for (var i = 0; i < header.getBlockTypeRankArrayLength(); i++) {
            var type = header.getBlockType(i);
            if (type != BlockType.empty) {
                if (type.toString().startsWith("megan")) {
                    if (newFileSize == -1L)
                        newFileSize = header.getLocationOfBlockInFile(i);
                    header.setBlockTypeRank(i, BlockType.rank(BlockType.empty));
                    header.setBlockSize(i, 0L);
                }
            }

            if (newFileSize != -1) {
                try (RandomAccessFile raf = new RandomAccessFile(fileName, "rw")) {
                    raf.setLength(newFileSize);
                }
            }
            if (newFileSize != -1 || header.getReserved3() > 0) {
                header.setReserved3(0);
                header.save();
            }
        }
    }

    /**
     * remove all classification data added by MEGAN (leaves ref annotations)
     */
    public static void removeMEGANClassificationData(DAAHeader header) throws IOException {
        var hasMeganBlock = false;
        var meganStart = header.getHeaderSize();
        for (var i = 0; i < header.getBlockTypeRankArrayLength(); i++) {
            var type = header.getBlockType(i);
            if (type != BlockType.empty) {
                if (type.toString().startsWith("megan") && !type.equals(BlockType.megan_ref_annotations)) {
                    hasMeganBlock = true;
                    header.setBlockTypeRank(i, BlockType.rank(BlockType.empty));
                    header.setBlockSize(i, 0L);
                } else
                    meganStart += header.getBlockSize(i);
            }
        }

        if (hasMeganBlock) {
            header.save();
            try (RandomAccessFile raf = new RandomAccessFile(header.getFileName(), "rw")) {
                raf.setLength(meganStart);
            }
        }
    }

    /**
     * replace a block
     *
	 */
    public static void replaceBlock(DAAHeader header, BlockType blockType, byte[] bytes, int size) throws IOException {
        var index = header.getIndexForBlockType(blockType);

        {
            if (index != -1) {
                header.setBlockTypeRank(index, BlockType.rank(BlockType.empty));
                header.setBlockSize(index, 0);
                if (index >= header.getLastDefinedBlockIndex()) {
                    var newSize = header.getLocationOfBlockInFile(index);
                    if (newSize > 0) {
                        try (var raf = new RandomAccessFile(header.getFileName(), "rw")) {
                            raf.setLength(newSize);
                        }
                    }
                } else
                    throw new IOException("Can't replace block, not last");
            }
        }
        try (var outs = new BufferedOutputStream(new FileOutputStream(header.getFileName(), true))) { // append
            index = header.getFirstAvailableBlockIndex();
            header.setBlockTypeRank(index, BlockType.rank(blockType));
            header.setBlockSize(index, size);
            outs.write(bytes, 0, size);
        }
        header.save();
    }

    /**
     * add new blocks
     *
	 */
    public static void appendBlocks(DAAHeader header, BlockType[] types, byte[][] blocks, int[] sizes) throws IOException {
        try (var outs = new BufferedOutputStream(new FileOutputStream(header.getFileName(), true))) { // append to file...
            for (var i = 0; i < blocks.length; i++) {
                final var bytes = blocks[i];
                final var size = sizes[i];
                final var index = header.getFirstAvailableBlockIndex();
                header.setBlockTypeRank(index, BlockType.rank(types[i]));
                header.setBlockSize(index, size);
                outs.write(bytes, 0, size);
            }
        }
        header.save(); // overwrite header
    }

    /**
     * append new blocks
     *
	 */
    public static void appendBlocks(DAAHeader header, BlockType type, byte[][] blocks, int[] sizes) throws IOException {
        var types = new BlockType[blocks.length];
        Arrays.fill(types, type);
        appendBlocks(header, types, blocks, sizes);
    }
}
