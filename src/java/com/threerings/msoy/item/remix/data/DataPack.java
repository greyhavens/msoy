//
// $Id$

package com.threerings.msoy.item.remix.data;

import java.io.File;
import java.io.FileInputStream;
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
    public DataPack ()
    {
    }

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

    /**
     * Add a new file to this DataPack.
     */
    public void addFile (String filename, String name, FileType type, boolean optional)
        throws IOException
    {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[fis.available()]; // the whole file should be available
        fis.read(data);
        addFile(file.getName(), data, name, type, optional);
    }

    /**
     * Add a data parameter.
     */
    public void addData (String name, DataType type, String value, boolean optional)
    {
        DataEntry entry = new DataEntry();
        entry.name = name;
        entry.type = type;
        entry.value = value;
        entry.optional = optional;

        _metadata.datas.put(name, entry);
    }

    /**
     * Add a new file to this DataPack.
     */
    public void addFile (String filename, byte[] data, String name, FileType type, boolean optional)
    {
        _files.put(filename, data);

        FileEntry entry = new FileEntry();
        entry.name = name;
        entry.type = type;
        entry.value = filename;
        entry.optional = optional;

        _metadata.files.put(name, entry);
    }

    /**
     * Write this datapack out to the specified filename.
     */
    public void writeTo (String filename)
        throws IOException
    {
        FileOutputStream fos = new FileOutputStream(filename);
        writeTo(fos);
        fos.close();
    }

    /**
     * Write the DataPack to the specified stream.
     */
    protected void writeTo (OutputStream out)
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
                    try {
                        //pack.addFile("/home/ray/media/mp3/tarzan and jane - Tarzan & Jane.mp3",
                        //    "music", FileType.BLOB, true);
                        pack.writeTo("/export/msoy/pages/ClockPack.jpk");

                    } catch (IOException ioe) {
                        System.err.println("ioe: " + ioe);
                        ioe.printStackTrace();
                    }
                }

                public void requestFailed (Exception cause)
                {
                    System.err.println("Oh noes: " + cause);
                }
            });
    }
}
