package com.threerings.msoy.bureau.client;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import com.google.common.collect.Sets;

import com.samskivert.util.StringUtil;

import com.threerings.util.ErrorDatabase;
import com.threerings.util.OOOFileAppender;

/**
 * Extends the usual appender to do summaries of the individual "channels" (prefixes) in a log file.
 */
public class BureauFileAppender extends OOOFileAppender
{
    @Override // from OOOFileAppender
    protected void summarizeLog (File target)
        throws IOException
    {
        long nowStamp = System.currentTimeMillis();
        ErrorDatabase errors = new ErrorDatabase();
        errors.readFrom(_database);
        
        StringBuffer summary = new StringBuffer();

        // First do the overall summary, printing full message text where dictated by error db.
        // These lines will be prefixed by the bureau id.
        BureauMergedFormat overallFormat = new BureauMergedFormat();
        summarizeLog(target, "Filtered messages (all bureaus)", overallFormat, errors, nowStamp, 
            summary);

        // Now do each bureau, suppressing full message text since it has already been printed.
        ErrorDatabase summaryOnly = new ErrorDatabase() {
            @Override public boolean shouldSummarize (long nowStamp, String message) {
                return true;
            }};
        for (String bureau : overallFormat.bureaus) {
            BureauFilteredFormat bformat = new BureauFilteredFormat(bureau);
            summarizeLog(target, "Filtered messages (" + bureau + ")", bformat, summaryOnly,
                nowStamp, summary);
        }

        // Prepend header
        if (summary.length() > 0) {
            StringBuffer header = new StringBuffer("Bureaus: ");
            header.append(StringUtil.join(overallFormat.bureaus.toArray()));
            header.append("\n\n");
            summary.insert(0, header);
        }
        
        // Send the email
        sendSummary(summary.toString());

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
            return true;
        }

        String _prefix;
    }
}
