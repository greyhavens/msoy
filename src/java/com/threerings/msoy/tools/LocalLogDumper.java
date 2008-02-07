//
// $Id$

package com.threerings.msoy.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.ObjectOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.threerings.panopticon.common.BaseEvent;
import com.threerings.panopticon.common.walken.EventSerializer;

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
                "out of the given source directories (default: \".*\").\n\n" +
                "If output_file is specified, results will not be pretty-printed to screen, " +
                "but dumped into a binary file as a collection of raw event bytes into a binary file.\n\n");
            System.exit(255);
        }

        // if needed, make the filename filter
        FileFilter ff = null;
        if (args.length >= 2) {
            final String filename_regex = args[1];
            ff = new FileFilter() {
                Pattern p = Pattern.compile(filename_regex);
                public boolean accept(File pathname) {
                    try {
                        return p.matcher(pathname.getCanonicalPath()).find();
                    } catch (IOException ioe) {
                        return false;
                    }
                }
            };
        }
        
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
    
    private static final Pattern timestampPattern = 
        Pattern.compile("(\\d\\d\\d\\d)\\p{Punct}(\\d\\d)\\p{Punct}(\\d\\d)");
    
    /**
     * Scans the absolute file path for a pattern of the form "YYYY,MM,DD" 
     * (using any punctuation as delimiters), and if successful, returns it 
     * as a millisecond timestamp value. 
     */
    protected static Long getTimestampFromFile (File file)
    {
        Matcher m = timestampPattern.matcher(file.getAbsolutePath());
        if (! m.find() || m.groupCount() != 3) {
            return null;
        }
        
        // it matches, let's parse it out
        int year = Integer.parseInt(m.group(1));
        int month = Integer.parseInt(m.group(2)) - 1; // nota bene: months are zero-indexed!
        int day = Integer.parseInt(m.group(3));
        
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, day);
        return new Long(c.getTimeInMillis());
    }

    /**
     * Recursively reads event files, fixing up timestamps if needed.  
     */
    protected static void recursiveProcess (File path, ObjectOutputStream out, FileFilter filter)
        throws Exception
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
        throws IOException, ClassNotFoundException
    {
        Long possibleTimestamp = getTimestampFromFile(file);
        
        DataInputStream din = new DataInputStream(
            new BufferedInputStream(new FileInputStream(file)));
        
        while (true) {
            Object o = null;
            
            try {
                byte[] data = new byte[din.readInt()];
                din.read(data);
                o = new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
            } catch (EOFException eofe) {
                break;
            }

            writeEvent(o, out, possibleTimestamp);

            _eventcount++;
        }

        _filecount++;
    }
    
    /** 
     * Processes a single Java-serialized event.
     */
    protected static void writeEvent (Object o, ObjectOutputStream out, Long possibleTimestamp)
        throws IOException
    {
        if (o instanceof BaseEvent) {
            // now try to fix up the event, if necessary
            BaseEvent event = (BaseEvent) o;
            if (event.timestamp == 0L && possibleTimestamp != null) {
                event.timestamp = possibleTimestamp;
            }
            
            if (out != null) {
                byte[] serialized = EventSerializer.toBytes(event);
                out.writeObject(serialized);
            } else {
                System.out.println(event);
            }
            
            return;
            
        } 

        if (o instanceof byte[]) { 
            byte[] bytes = (byte[]) o;
            if (out != null) {
                out.writeObject(bytes);
            } else {
                System.out.println("Serialized event: " + bytes);
            }
        
            return;
        } 

        throw new IOException("Failed to read unknown event object: " + o); 
    }
    
    protected static int _filecount = 0;
    protected static int _eventcount = 0;
}
