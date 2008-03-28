//
// $Id$

package com.threerings.msoy.tools;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.threerings.panopticon.common.BaseEvent;
import com.threerings.panopticon.common.Event;
import com.threerings.panopticon.common.serialize.walken.EventSerializer;

/**
 * Log file converter for log formats used early in alpha test stage.
 * Turns all old-format logs to new hessian-serialized ones, backing up old ones.
 * 
 * Also, in cases when old-format events lacked correct timestamp info,
 * it fixes them up with timestamps inferred from the file path.
 */
public class AlphaReleaseLogConverter
{
    public static final String OLD_PREFIX = "old.";
    
    public static void main (String[] args)
        throws Exception
    {
        if (args.length < 1 || args.length > 2) {
            System.err.println(
                "Usage:\n  AlphaReleaseLogConverter <source> [<filter>]\n" +
                "where <source> is an event file or a directory that contains event files, " +
                "and <filter> is an optional regular expression used to select event files " +
                "out of the given source directories (default: \"/events_\").\n\n");
            System.exit(255);
        }

        final String filename_regex = (args.length == 2) ? args[1] : "/events_";
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
        long start = System.currentTimeMillis();
        recursiveProcess(new File(args[0]), ff);
        double delta = (System.currentTimeMillis() - start) / 1000.0;

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
    protected static void recursiveProcess (File path, FileFilter filter)
        throws Exception
    {
        if (path.isFile()) {
            process(path);

        } else if (path.isDirectory()) {

            // we're a directory, go through our contents, examining all subdirectories,
            // and any files that match the pattern
            for (File file : path.listFiles()) {
                if (filter.accept(file) || file.isDirectory()) { 
                    recursiveProcess(file, filter);
                }
            }
        }
    }
    
    /**
     * Reads and processes a single event file.
     */
    protected static void process (File file) 
        throws IOException, ClassNotFoundException
    {
        Long possibleTimestamp = getTimestampFromFile(file);
        
        DataInputStream din = new DataInputStream(
            new BufferedInputStream(new FileInputStream(file)));
        
        // read in the entire log file
        
        ArrayList<Object> contents = new ArrayList<Object>(); 
        boolean oldFormatDetected = false;
        
        while (true) {
            try {
                int size = din.readInt();
                byte[] data = new byte[size];
                din.read(data);
                Object o = new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
                
                contents.add(o);
                oldFormatDetected |= (o instanceof BaseEvent);
                
            } catch (EOFException eofe) {
                din.close();
                break;
            }
        } 
        
        // does this log file need converting?
        if (! oldFormatDetected) {
            return; // nothing to do!
        }
        
        // back up the file
        File backup = new File(file.getParent() + File.separator + OLD_PREFIX + file.getName());
        if (! file.renameTo(backup)) {
            throw new IOException("Failed to rename file: " + file);
        }
        
        // convert to new format
        File newfile = new File(file.getAbsolutePath());
        FileChannel out = new FileOutputStream(newfile, false).getChannel();
        
        for (Object o : contents) {
            if (o instanceof BaseEvent) {
                BaseEvent event = (BaseEvent) o;
                if (event.timestamp == 0L && possibleTimestamp != null) {
                    event.timestamp = possibleTimestamp;
                }
                writeEvent(event, out);
            } else if (o instanceof byte[]) {
                writeEvent((byte[]) o, out);
            } else {
                throw new IllegalStateException("Unexpected format!");
            }

            _eventcount++;
        }
        
        out.close();
        _filecount++;
    }
    
    protected static void writeEvent (Event event, FileChannel channel)
        throws IOException
    {
        writeEvent(EventSerializer.toBytes(event), channel);
    }
    
    protected static void writeEvent (byte[] hessianEvent, FileChannel channel)
        throws IOException
    {
        // serialize our event into a byte buffer
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(hessianEvent);
        oout.close();
        ByteBuffer data = ByteBuffer.wrap(bout.toByteArray());

        // create another buffer that contains the length of the serialized event
        bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(bout);
        dout.writeInt(data.capacity());
        ByteBuffer size = ByteBuffer.wrap(bout.toByteArray());

        // write both to the log file using the scatter/gather interface
        channel.write(new ByteBuffer[] { size, data });
    }
    
    protected static int _filecount = 0;
    protected static int _eventcount = 0;
}
