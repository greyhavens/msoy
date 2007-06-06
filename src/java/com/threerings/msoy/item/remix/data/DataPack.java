//
// $Id$

package com.threerings.msoy.item.remix.data;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Map;

import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.samskivert.util.ResultListener;

/**
 *
 */
public class DataPack extends com.whirled.DataPack
{
    public DataPack (String url, final ResultListener<DataPack> listener)
    {
        super(url, new ResultListener<com.whirled.DataPack>() {
            public void requestCompleted (com.whirled.DataPack pack) {
                // cast to this subclass
                listener.requestCompleted((DataPack) pack);
            }

            public void requestFailed (Exception cause) {
                listener.requestFailed(cause);
            }
        });
    }

    public void addFile (String filename, byte[] data)
    {
        _files.put(filename, data);
    }

    public void write (String filename)
    {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            write(fos);
            fos.close();

        } catch (IOException ioe) {
            System.err.println("ioe: " + ioe);
            ioe.printStackTrace();
        }
    }

    protected void write (OutputStream out)
        throws IOException
    {
        ZipOutputStream zos = new ZipOutputStream(out);
        zos.setMethod(ZipOutputStream.STORED);
        CRC32 crc = new CRC32();

        for (Map.Entry<String,byte[]> file : _files.entrySet()) {
            byte[] data = file.getValue();
            ZipEntry entry = new ZipEntry(file.getKey());
            entry.setSize(data.length);
            crc.reset();
            crc.update(data);
            entry.setCrc(crc.getValue());
            zos.putNextEntry(entry);
            zos.write(data, 0, data.length);
            zos.closeEntry();
        }

        // write the metadata
        byte[] data = _metadata.toXML().getBytes("utf-8");
        ZipEntry entry = new ZipEntry("_data.xml");
        entry.setSize(data.length);
        crc.reset();
        crc.update(data);
        entry.setCrc(crc.getValue());
        zos.putNextEntry(entry);
        zos.write(data, 0, data.length);
        zos.closeEntry();

        zos.finish();
    }

    public static void main (String[] args)
    {
        new DataPack("http://tasman.sea.earth.threerings.net:8080/ClockPack.dpk",
            new ResultListener<DataPack>() {
                public void requestCompleted (DataPack pack)
                {
                    pack.write("/export/msoy/pages/ClockPack.jpk");
                }

                public void requestFailed (Exception cause)
                {
                    System.err.println("Oh noes: " + cause);
                }
            });
    }
}
