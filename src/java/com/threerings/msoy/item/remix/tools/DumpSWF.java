package com.threerings.msoy.item.remix.tools;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.jswiff.SWFDocument;
import com.jswiff.SWFReader;
import com.jswiff.listeners.SWFDocumentReader;
import com.jswiff.listeners.SWFListener;
import com.jswiff.swfrecords.Rect;
import com.jswiff.swfrecords.tags.DefinitionTag;
import com.jswiff.swfrecords.tags.DefineBitsJPEG2;
import com.jswiff.swfrecords.tags.DefineBitsJPEG3;
import com.jswiff.swfrecords.tags.Tag;
import com.jswiff.swfrecords.tags.TagConstants;
import com.jswiff.util.ImageUtilities;

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
        dumpImages(doc, outputDir);

        Rect size = doc.getFrameSize();
        return "" + (size.getXMax() / 20) + " " + (size.getYMax() / 20);
    }

    protected void dumpImages (SWFDocument doc, File outputDir)
        throws IOException
    {
        for (Object tagObj : doc.getTags()) {
            if (tagObj instanceof DefinitionTag) {
                DefinitionTag tag = (DefinitionTag) tagObj;
                int id = tag.getCharacterId();

                // TODO: more than Jpgs
                switch (tag.getCode()) {
                case TagConstants.DEFINE_BITS_JPEG_2:
                    saveJPEG(((DefineBitsJPEG2) tag).getJpegData(), outputDir, id);
                    break;

                case TagConstants.DEFINE_BITS_JPEG_3:
                    saveJPEG(((DefineBitsJPEG3) tag).getJpegData(), outputDir, id);
                    break;
                }
            }
        }
    }

    protected void saveJPEG (byte[] data, File outputDir, int id)
        throws IOException
    {
        data = truncateHeader(data);

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

    protected byte[] truncateHeader (byte[] data)
    {
        // most, but not all JPEG tags contain this header
        if ((data.length < 4) || (data[0] != HEADER[0]) ||
                (data[1] != HEADER[1]) || (data[2] != HEADER[2]) ||
                (data[3] != HEADER[3])) {
            return data;
        }
        byte[] truncatedData = new byte[data.length - 4];
        System.arraycopy(data, 4, truncatedData, 0, truncatedData.length);
        return truncatedData;
    }

    /** Magic blunders. */
    protected static final byte[] HEADER = new byte[] {
        (byte) 0xff, (byte) 0xd9, (byte) 0xff, (byte) 0xd8
    };

}
