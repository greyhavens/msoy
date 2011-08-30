//
// $Id$

package com.threerings.msoy.item.remix.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import com.jswiff.SWFDocument;
import com.jswiff.SWFReader;
import com.jswiff.SWFWriter;
import com.jswiff.listeners.SWFDocumentReader;
import com.jswiff.swfrecords.AlphaBitmapData;
import com.jswiff.swfrecords.RGBA;
import com.jswiff.swfrecords.Rect;
import com.jswiff.swfrecords.tags.DefineBitsJPEG2;
import com.jswiff.swfrecords.tags.DefineBitsLossless2;
import com.jswiff.swfrecords.tags.DefinitionTag;
import com.jswiff.swfrecords.tags.Tag;
import com.jswiff.swfrecords.tags.TagConstants;
import com.jswiff.util.ImageUtilities;

public class UndumpSWF
{
    public static void main (String[] args)
        throws IOException
    {
        if (args.length != 3 && args.length != 5 ) {
            printUsageAndExit();
        }

        int width = -1;
        int height = -1;
        if (args.length > 3) {
            try {
                width = Integer.parseInt(args[3]);
                height = Integer.parseInt(args[4]);
                if (width < 0 || height < 0) {
                    printUsageAndExit();
                }

            } catch (NumberFormatException nfe) {
                printUsageAndExit();
            }
        }

        File sourceFile = new File(args[0]);
        File inputDir = new File(args[1]);
        File destFile = new File(args[2]);

        UndumpSWF undumper = new UndumpSWF();
        undumper.undump(sourceFile, inputDir, destFile, width, height);
        System.exit(0);
    }

    protected static void printUsageAndExit ()
    {
        System.out.println("Usage:    UndumpSWF <swfFile> <inputDir> <destSwf> [newWidth] [newHeight]");
        System.exit(1);
    }

    public void undump (
        File sourceFile, File inputDir, File destFile, int width, int height)
        throws IOException
    {
        SWFReader reader = new SWFReader(new FileInputStream(sourceFile));
        SWFDocumentReader docReader = new SWFDocumentReader();
        reader.addListener(docReader);
        reader.read();
        reader = null; // gc

        SWFDocument doc = docReader.getDocument();
        replaceImages(doc, inputDir);

        if (width != -1 && height != -1) {
            doc.setFrameSize(new Rect(0, 20L * width, 0, 20L * height));
        }
        SWFWriter writer = new SWFWriter(doc, new FileOutputStream(destFile));
        writer.write();
    }

    /**
     * Replace any images in the file with any corresponding images
     * in the input directory.
     */
    protected void replaceImages (SWFDocument doc, File inputDir)
        throws IOException
    {
        @SuppressWarnings("unchecked") List<Object> tags = doc.getTags();
        for (int ii=0; ii < tags.size(); ii++) {
            Object tagObj = tags.get(ii);
            if (tagObj instanceof DefinitionTag) {
                DefinitionTag tag = (DefinitionTag) tagObj;
                int id = tag.getCharacterId();

                switch (tag.getCode()) {
                case TagConstants.DEFINE_BITS_JPEG_2:
                case TagConstants.DEFINE_BITS_JPEG_3:
                case TagConstants.DEFINE_BITS_LOSSLESS:
                case TagConstants.DEFINE_BITS_LOSSLESS_2:
                    Tag newTag = createReplacement(id, inputDir);
                    if (newTag != null) {
                        // replace it in the list... should be kosher
                        tags.set(ii, newTag);
                    }
                    break;
                }
            }
        }
    }

    protected Tag createReplacement (int id, File inputDir)
        throws IOException
    {
        // look for a jpeg
        File f = new File(inputDir, String.valueOf(id) + ".jpg");
        if (f.exists()) {
            byte[] imageData = readRawImageData(f);
            return new DefineBitsJPEG2(id, imageData);
        }

        // look for a png
        f = new File(inputDir, String.valueOf(id) + ".png");
        if (f.exists()) {
            return createGIForPNG(id, f);
        }

        // look for a gif
        f = new File(inputDir, String.valueOf(id) + ".gif");
        if (f.exists()) {
            return createGIForPNG(id, f);
        }

        // nothing found
        return null;
    }

    protected Tag createGIForPNG (int id, File f)
        throws IOException
    {
        // TODO: write indexed images and images without alpha (Lossless1)
        // (The following code converts all images to RGBA)
        BufferedImage img = ImageIO.read(new FileInputStream(f));
        RGBA[] rawData = ImageUtilities.getRGBAArray(img);
        AlphaBitmapData bitmapData = new AlphaBitmapData(rawData);
        return new DefineBitsLossless2(id,
            DefineBitsLossless2.FORMAT_32_BIT_RGBA,
            img.getWidth(), img.getHeight(), bitmapData);
    }

    /**
     * Read a file into a byte[].
     */
    protected byte[] readRawImageData (File f)
        throws IOException
    {
        FileInputStream fin = new FileInputStream(f);

        int length = (int) f.length();
        byte[] data = new byte[length];
        int pos = 0;
        do {
            int read = fin.read(data, pos, length);
            if (read == -1) {
                System.err.println("Early file termination?");
                break;
            }
            pos += read;
            length -= read;
        } while (length > 0);

        return data;
    }
}
