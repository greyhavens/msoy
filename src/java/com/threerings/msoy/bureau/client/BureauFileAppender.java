package com.threerings.msoy.bureau.client;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.samskivert.util.StringUtil;

import com.threerings.util.ErrorDatabase;
import com.threerings.util.OOOFileAppender;

/**
 * Extends the usual appender to do summaries of the individual "channels" (prefixes) in a log file.
 */
public class BureauFileAppender extends OOOFileAppender
{
    /**
     * Summarizes the log specified by the first command line argument and prints it to stdout
     * instead of sending via email.
     */
    public static void main (String[] args)
        throws IOException
    {
        BureauFileAppender appender = new BureauFileAppender();
        File target = new File(args[0]);
        StringBuffer summary = new StringBuffer();
        appender.summarizeLog(target, summary);
        System.out.print(summary);
    }
    
    @Override // from OOOFileAppender
    protected void summarizeLog (File target)
        throws IOException
    {
        StringBuffer summary = new StringBuffer();
        summarizeLog(target, summary);
        sendSummary(summary.toString());
    }
    
    protected void summarizeLog (File target, StringBuffer summary)
        throws IOException
    {
        long nowStamp = System.currentTimeMillis();

        // First do the overall summary with no full text, collecting bureau names as we go
        BureauMergedFormat overallFormat = new BureauMergedFormat();
        StringBuffer overallSummary = new StringBuffer();
        summarizeLog(
            target, "Filtered messages (all bureaus)", overallFormat, new ErrorDatabase() {
                @Override public boolean shouldSummarize (long nowStamp, String message) {
                    return true;
                }}, nowStamp, overallSummary);

        // Read the summary database
        ErrorDatabase errors = new ErrorDatabase();
        errors.readFrom(_database);

        // Now do each bureau into a separate buffer, inserting a marker between the full text
        // and the summary.
        String marker = "~!@#" + nowStamp + "~!@#";
        HashMap<String, StringBuffer> summaries = Maps.newHashMap();
        for (String bureau : overallFormat.bureaus) {
            BureauFilteredFormat bformat = new BureauFilteredFormat(bureau);
            StringBuffer bsummary = new StringBuffer();
            String header = marker + "Filtered messages (" + bureau + ")";
            summarizeLog(target, header, bformat, errors, nowStamp, bsummary);
            summaries.put(bureau, bsummary);
        }
        
        // Append all full text portions
        for (String bureau : overallFormat.bureaus) {
            StringBuffer bsummary = summaries.get(bureau);
            int mpos = bsummary.indexOf(marker);
            if (mpos <= 0) {
                continue;
            }
            summary.append(bsummary.substring(0, mpos));
        }

        // Append the overall summary
        if (overallSummary.length() > 0) {
            if (summary.length() > 0) {
                summary.append("\n");
            }
            summary.append(overallSummary);
        }
        
        // Append all summary portions
        for (String bureau : overallFormat.bureaus) {
            StringBuffer bsummary = summaries.get(bureau);
            int mpos = bsummary.indexOf(marker);
            if (mpos == -1) {
                continue;
            }
            if (summary.length() > 0) {
                summary.append("\n");
            }
            summary.append(bsummary.substring(mpos + marker.length()));
        }

        if (summary.length() > 0) {
            // prepend header showing all the bureaus summarized
            StringBuffer header = new StringBuffer("Bureaus: ");
            header.append(StringUtil.join(overallFormat.bureaus.toArray()));
            header.append("\n\n");
            summary.insert(0, header);
            
            // append footer showing how long the summary took to generate
            summary.append("\n(Generation time: ");
            summary.append(StringUtil.intervalToString(System.currentTimeMillis() - nowStamp));
            summary.append(")\n");
        }
        
        // Prune & write out message timestamps
        errors.pruneOldErrors(nowStamp);
        errors.writeTo(_database);
    }
    
    /**
     * Handles all the common bits of bureau log format.
     */
    protected static abstract class BureauFormat
        implements LineFormat
    {
        // from LineFormat
        public void setLine (String line)
        {
            _line = line;
        }
    
        // from LineFormat
        public boolean isStray ()
        {
            // Subclasses need to set up _pos to point to after the bureau prefix, e.g. "bureau| "
            _line = _line.substring(_pos);
    
            if  (!isProbablyStandardLogLine(_line)) {
                return true;
            }

            // Now search for end of level string
            int bidx = _line.indexOf(": ", LEVEL_OFFSET_IN_STANDARD_LOG_LINE);
            if (bidx == -1) {
                return true;
            }
    
            _level = _line.substring(LEVEL_OFFSET_IN_STANDARD_LOG_LINE, bidx);
            _pos = bidx;
            return false;
        }
    
        // from LineFormat
        public String extractLevel ()
        {
            return _level;
        }
    
        // from LineFormat
        public String extractMessageId ()
        {
            // advance past ": "
            _pos += 2;
    
            // strip off '[' and anything that follows
            int bidx = _line.indexOf('[', _pos);
            if (bidx != -1) {
                return _line.substring(_pos, bidx).trim();
            } else {
                return _line.substring(_pos);
            }
        }
    
        String _line;
        String _level;
        int _pos;
    }

    /**
     * Catalogs all log lines, regardless of bureau. Also builds up a set of all bureaus found in
     * the log.
     */
    protected static class BureauMergedFormat extends BureauFormat
    {
        /** Keep track of all bureaus we've seen. */
        public HashSet<String> bureaus =  Sets.newHashSet();
        
        // from LineFormat
        public boolean isImportant ()
        {
            _pos = _line.indexOf("| ");
            if (_pos == -1 || _pos >= 32) {
                return false;
            }
            String bureau = _line.substring(0, _pos);
            if (!bureaus.contains(bureau)) {
                bureaus.add(bureau);
            }
            _pos += 2;
            return true;
        }
    }

    /**
     * Extracts all the lines for only one bureau.
     */
    protected static class BureauFilteredFormat extends BureauFormat
    {
        public BureauFilteredFormat (String bureau)
        {
            _prefix = bureau + "| ";
        }
        
        // from LineFormat
        public boolean isImportant ()
        {
            if (!_line.startsWith(_prefix)) {
                return false;
            }
            
            _pos = _prefix.length();

            // Ignore stuff from user code, this will get relayed to the database logs
            if (_pos + 9 <= _line.length() && _line.substring(_pos, _pos + 9).equals(
                "UserCode-")) {
                return false;
            }

            return true;
        }

        String _prefix;
    }
}
