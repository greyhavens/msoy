//
// $Id$

package com.threerings.msoy.item.remix.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.jswiff.SWFDocument;
import com.jswiff.SWFReader;
import com.jswiff.listeners.SWFDocumentReader;
import com.jswiff.swfrecords.AlphaBitmapData;
import com.jswiff.swfrecords.BitmapData;
import com.jswiff.swfrecords.BitmapPixelData;
import com.jswiff.swfrecords.Pix15;
import com.jswiff.swfrecords.Pix24;
import com.jswiff.swfrecords.RGBA;
import com.jswiff.swfrecords.Rect;
import com.jswiff.swfrecords.ZlibBitmapData;
import com.jswiff.swfrecords.tags.DefineBitsJPEG2;
import com.jswiff.swfrecords.tags.DefineBitsJPEG3;
import com.jswiff.swfrecords.tags.DefineBitsLossless2;
import com.jswiff.swfrecords.tags.DefineBitsLossless;
import com.jswiff.swfrecords.tags.DefinitionTag;
import com.jswiff.swfrecords.tags.TagConstants;

public class DumpSWF
{
    public static void main (String[] args)
        throws IOException
    {
        if (args.length != 2) {
            printUsageAndExit();
        }

        File sourceFile = new File(args[0]);
        File outputDir = new File(args[1]);

        DumpSWF dumper = new DumpSWF();
        String size = dumper.dump(sourceFile, outputDir);
        System.out.println(size);
        System.exit(0);
    }

    protected static void printUsageAndExit ()
    {
        System.out.println("Usage:    DumpSWF <swfFile> <outputDir>");
        System.exit(1);
    }

    /**
     */
    public String dump (File sourceFile, File outputDir)
        throws IOException
    {
        SWFReader reader = new SWFReader(new FileInputStream(sourceFile));
        SWFDocumentReader docReader = new SWFDocumentReader();
        reader.addListener(docReader);
        reader.read();

        SWFDocument doc = docReader.getDocument();
        dumpMedia(doc, outputDir);

        Rect size = doc.getFrameSize();
        return "" + (size.getXMax() / SWFConstants.TWIPS_PER_PIXEL) + " " +
            (size.getYMax() / SWFConstants.TWIPS_PER_PIXEL);
    }

    protected void dumpMedia (SWFDocument doc, File outputDir)
        throws IOException
    {
        for (Object tagObj : doc.getTags()) {
            if (tagObj instanceof DefinitionTag) {
                DefinitionTag tag = (DefinitionTag) tagObj;
                int id = tag.getCharacterId();

                switch (tag.getCode()) {
                case TagConstants.DEFINE_BITS_JPEG_2:
                    saveJPEG(((DefineBitsJPEG2) tag).getJpegData(), outputDir, id);
                    break;

                case TagConstants.DEFINE_BITS_JPEG_3:
                    saveJPEG(((DefineBitsJPEG3) tag).getJpegData(), outputDir, id);
                    break;

                case TagConstants.DEFINE_BITS_LOSSLESS:
                    DefineBitsLossless png = (DefineBitsLossless) tag;
                    savePNG(png.getZlibBitmapData(), png.getWidth(), png.getHeight(),
                        outputDir, id);
                    break;

                case TagConstants.DEFINE_BITS_LOSSLESS_2:
                    DefineBitsLossless2 png2 = (DefineBitsLossless2) tag;
                    savePNG(png2.getZlibBitmapData(), png2.getWidth(), png2.getHeight(),
                        outputDir, id);
                    break;

                case TagConstants.SOUND_STREAM_HEAD_2:
                    // mp3 not yet supported
                    break;
                }
            }
        }
    }

    protected void savePNG (ZlibBitmapData data, int width, int height, File outputDir, int id)
        throws IOException
    {
        BufferedImage img;
        if (data instanceof AlphaBitmapData) {
            // TODO: output 8 bit colormapped as well (with alpha)
            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            RGBA[] rawData = ((AlphaBitmapData) data).getBitmapPixelData();
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    RGBA pixel = rawData[index++];
                    int pix = (pixel.getAlpha() << 24) |
                        (pixel.getRed() << 16) | (pixel.getGreen() << 8) |
                        (pixel.getBlue());
                    img.setRGB(x, y, pix);
                }
            }

        } else if (data instanceof BitmapData) {
            BitmapPixelData[] rawData = ((BitmapData) data).getBitmapPixelData();
            if (rawData[0] instanceof Pix15) {
                img = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB);
                int index = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        Pix15 pixel = (Pix15) rawData[index++];
                        int pix = (pixel.getRed() << 10) | (pixel.getGreen() << 5) |
                            (pixel.getBlue());
                        img.setRGB(x, y, pix);
                    }
                }

            } else if (rawData[0] instanceof Pix24) {
                img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                int index = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        Pix24 pixel = (Pix24) rawData[index++];
                        int pix = (pixel.getRed() << 16) | (pixel.getGreen() << 8) |
                            (pixel.getBlue());
                        img.setRGB(x, y, pix);
                    }
                }

            } else {
                throw new IllegalArgumentException(
                    "Unknown pixel format: " + rawData[0].getClass());
            }

        } else {
            throw new IllegalArgumentException("Unknown png data format: " + data.getClass());
        }

        File outFile = new File(outputDir, "" + id + ".png");

        ImageIO.write(img, "png", outFile);
    }

    protected void saveJPEG (byte[] data, File outputDir, int id)
        throws IOException
    {
        data = truncateJPEGHeader(data);

        File outFile = new File(outputDir, "" + id + ".jpg");

        if (true) {
            FileOutputStream fout = new FileOutputStream(outFile);
            fout.write(data);
            fout.close();

//        } else {
//  TODO: I guess this path will ensure the data is really a jpg.
//
//            BufferedImage image = ImageUtilities.loadImage(
//                new ByteArrayInputStream(data));
//            // shouldn't ImageUtilities throw this exception? Sheesh.
//            if (image == null) {
//                throw new IllegalArgumentException("Garbled JPEG  data!");
//            }
//
//            ImageUtilities.saveImageAsJPEG(image, new FileOutputStream(outFile));
        }
    }

    protected byte[] truncateJPEGHeader (byte[] data)
    {
        byte[] HEADER = SWFConstants.JPEG_HEADER;
        // most, but not all JPEG tags contain this header
        if ((data.length < HEADER.length) || (data[0] != HEADER[0]) ||
                (data[1] != HEADER[1]) || (data[2] != HEADER[2]) ||
                (data[3] != HEADER[3])) {
            return data;
        }
        byte[] truncatedData = new byte[data.length - HEADER.length];
        System.arraycopy(data, HEADER.length, truncatedData, 0, truncatedData.length);
        return truncatedData;
    }
}
