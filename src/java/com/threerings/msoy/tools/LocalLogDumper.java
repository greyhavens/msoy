//
// $Id$

package com.threerings.msoy.tools;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

/**
 * Dumps the events from a {@link LocalEventLogger}.
 */
public class LocalLogDumper
{
    public static void main (String[] args)
        throws Exception
    {
        if (args.length < 1) {
            System.err.println("Usage: LocalLogDumper log_file [log_file ...]");
            System.exit(255);
        }

        for (String filename : args) {
            DataInputStream din = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filename)));
            while (true) {
                try {
                    byte[] data = new byte[din.readInt()];
                    din.read(data);
                    System.err.println(
                        new ObjectInputStream(new ByteArrayInputStream(data)).readObject());
                } catch (EOFException eofe) {
                    break;
                }
            }
        }
    }
}
