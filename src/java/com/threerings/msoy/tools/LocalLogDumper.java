//
// $Id$

package com.threerings.msoy.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.ObjectOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.regex.Pattern;

import com.threerings.msoy.server.LocalEventLogger;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.net.otp.message.event.EventDataSerializer;
import com.threerings.panopticon.common.serialize.EncodingException;

/**
 * Dumps the events from a {@link LocalEventLogger}, either to screen,
 * or to a binary file that holds serialized byte arrays.
 */
public class LocalLogDumper
{
    public static void main (String[] args)
        throws Exception
    {
        if (args.length < 1 || args.length > 3) {
            System.err.println(
                "Usage:\n  LocalLogDumper <source> [<filter> [<output_file>]]\n" +
                "where <source> is an event file or a directory that contains event files, " +
                "and <filter> is an optional regular expression used to select event files " +
                "out of the given source directories (default: \"/events_\").\n\n" +
                "If output_file is specified, results will not be pretty-printed to screen, " +
                "but dumped into a binary file as a collection of raw event bytes into a binary file.\n\n");
            System.exit(255);
        }

        // if needed, make the filename filter
        final String filename_regex = (args.length >= 2) ? args[1] : "/events_";
        final FileFilter ff = new FileFilter() {
            Pattern p = Pattern.compile(filename_regex);
            public boolean accept(File pathname) {
                try {
                    return p.matcher(pathname.getCanonicalPath()).find();
                } catch (IOException ioe) {
                    return false;
                }
            }
        };

        // do it!

        ObjectOutputStream out = null;
        if (args.length >= 3) {
            File target = new File(args[2], "events.dat");
            out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(target)));
        }

        long start = System.currentTimeMillis();
        recursiveProcess(new File(args[0]), out, ff);
        double delta = (System.currentTimeMillis() - start) / 1000.0;

        if (out != null) {
            out.close();
        }

        System.err.println("Processed " + _eventcount + " events from " + _filecount +
            " files in " + delta + "s (" + _eventcount/delta + " events/s).");
    }

    /**
     * Recursively reads event files.
     */
    protected static void recursiveProcess (File path, ObjectOutputStream out, FileFilter filter)
        throws IOException
    {
        if (path.isFile()) {
            process(path, out);

        } else if (path.isDirectory()) {

            // we're a directory, go through our contents, examining all subdirectories,
            // and any files that match the pattern
            for (File file : path.listFiles()) {
                if (filter.accept(file) || file.isDirectory()) {
                    recursiveProcess(file, out, filter);
                }
            }
        }
    }

    /**
     * Reads and processes a single event file.
     */
    protected static void process (File file, ObjectOutputStream out)
        throws IOException
    {
        DataInputStream din = new DataInputStream(
            new BufferedInputStream(new FileInputStream(file)));

        while (true) {
            try {
                byte[] data = new byte[din.readInt()];
                din.read(data);
                ObjectInputStream payload = new ObjectInputStream(new ByteArrayInputStream(data));
                Object o = payload.readObject();

                if (o instanceof Integer) {
                    // optic version
                    if (! ((Integer)o).equals(LocalEventLogger.VERSION_ID)) {
                        throw new IllegalStateException("Unknown item version: " + o);
                    }

                    Object result = payload.readObject();
                    if (! (result instanceof byte[])) {
                        throw new IllegalStateException("Unexpected data in item version " + o);
                    }
                    output (out, (byte[]) result, (Integer) o);

                } else {
                    throw new IllegalStateException("Unexpected event object: " + o.getClass().getName());
                }

            } catch (EOFException eofe) {
                break;

            } catch (ClassNotFoundException ce) {
                throw new IOException("File contains old event definitions: " + file.getName() + "," +
                		"; original exception: " + ce);
            } catch (EncodingException ee) {
                throw new IOException("Unknown event found: " + ee);
            }

            _eventcount++;
        }

        _filecount++;
    }

    protected static void output (ObjectOutputStream out, byte[] opticEvent, int version)
        throws EncodingException, IOException
    {
        if (out != null) {
            out.writeObject(opticEvent);

        } else {
            ByteArrayInputStream in = new ByteArrayInputStream(opticEvent);
            EventData data = EventDataSerializer.deserialize(in);
            System.out.println("Serialized event (" + version + "): " + data);
        }

    }

    protected static int _filecount = 0;
    protected static int _eventcount = 0;
}
