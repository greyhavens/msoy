//
// $Id$

package com.threerings.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import org.apache.log4j.FileAppender;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ErrorCode;

import com.samskivert.net.MailUtil;

/**
 * Handles rolling over log4j log files and performing some additional processing. In addition to
 * rolling files over at midnight, the rolled files have their warning messages summarized and
 * mailed to a configured email address. Further, previously rolled over log files older than a
 * configured number of days will be deleted. Configuration is as follows:
 *
 * <pre>
 * log4j.appender.oooappender = com.threerings.util.OOOFileAppender
 * log4j.appender.oooappender.layout = org.apache.log4j.PatternLayout
 * log4j.appender.oooappender.layout.ConversionPattern = %d %p %C{1}: %m%n
 * log4j.appender.oooappender.File = log/somelogfile.log
 * log4j.appender.oooappender.DatePattern = '.'yyyy-MM-dd
 * log4j.appender.oooappender.DatabaseFile = run/log-errors.db
 * log4j.appender.oooappender.Email = project-dev@threerings.net
 * log4j.appender.oooappender.PruneDays = 7
 * </pre>
 *
 * <p> Note that the '%d %p' format prefix is required for the log summarization functionality to
 * work properly.
 *
 * <p> Some of this code is duplicated from DailyRollingFileAppender because the implementor of
 * that class was apparently unconcerned about making their code extensible and made all members
 * either private or package protected. They even managed to ignore the fine example set by
 * FileAppender which they extended and customized. Alas.
 */
public class OOOFileAppender extends FileAppender
{
    /**
     * Sets the <b>DatePattern</b> option.
     *
     * @param pattern a string in the same format as expected by {@link SimpleDateFormat}. This
     * options determines the rollover schedule.
     */
    public void setDatePattern (String pattern)
    {
        _datePattern = pattern;
    }

    /**
     * Returns the value of the <b>DatePattern</b> option.
     */
    public String getDatePattern ()
    {
        return _datePattern;
    }

    /**
     * Sets <b>DatabaseFile</b> option.
     */
    public void setDatabaseFile (String database)
    {
        _database = database;
    }

    /**
     * Returns the value of the <b>DatabaseFile</b> option.
     */
    public String getDatabaseFile ()
    {
        return _database;
    }

    /**
     * Sets <b>Email</b> option.
     */
    public void setEmail (String email)
    {
        _email = email;
    }

    /**
     * Returns the value of the <b>Email</b> option.
     */
    public String getEmail ()
    {
        return _email;
    }

    /**
     * Sets the <b>PruneDays</b> option.
     */
    public void setPruneDays (String days)
    {
        _pruneDays = Integer.valueOf(days);
    }

    /**
     * Returns the value of the <b>PruneDays</b> option.
     */
    public String getPruneDays ()
    {
        return String.valueOf(_pruneDays);
    }

    @Override
    public void activateOptions()
    {
        super.activateOptions();

        // make sure we were provided with configuration values
        if (_datePattern == null || this.fileName == null) {
            LogLog.error("Either File or DatePattern options are not set for appender " +
                         "[" + name +"].");
            return;
        }

        // determine our rollover periodicity
        _now.setTime(System.currentTimeMillis());
        _sdf = new SimpleDateFormat(_datePattern);
        int type = computeCheckPeriod();
        printPeriodicity(type);
        _rc.setType(type);

        // determine the name of the next rollover file
        File file = new File(this.fileName);
        _scheduledFilename = this.fileName + _sdf.format(new Date(file.lastModified()));

        // if we have log summary configuration, verify that it's copacetic
        if (_database != null) {
            File dbfile = new File(_database);
            try {
                dbfile.createNewFile(); // this will NOOP if the file already exists
            } catch (IOException ioe) {
                LogLog.error("Unable to create database file [" + _database + "].", ioe);
            }
            if (!dbfile.canWrite()) {
                LogLog.error("Unable to write to database file [" + _database + "].");
            }
        }

        if (_email != null && !MailUtil.isValidAddress(_email)) {
            LogLog.error("Summary email address appears invalid [" + _email + "].");
        }
    }

    @Override
    protected void subAppend (LoggingEvent event)
    {
        // before actually logging, check whether it is time to do a rollover, if so, schedule the
        // next rollover time and then rollover
        long n = event.getTimeStamp();
        if (n >= _nextCheck) {
            _now.setTime(n);
            _nextCheck = _rc.getNextCheckMillis(_now);
            try {
                rollOver();
            } catch (IOException ioe) {
                LogLog.error("rollOver() failed.", ioe);
            }
        }

        super.subAppend(event);
    }

    /**
     * Computes the roll over period by looping over the periods, starting with the shortest, and
     * stopping when the r0 is different from from r1, where r0 is the epoch formatted according
     * the datePattern (supplied by the user) and r1 is the epoch+nextMillis(i) formatted according
     * to datePattern. All date formatting is done in GMT and not local format because the test
     * logic is based on comparisons relative to 1970-01-01 00:00:00 GMT (the epoch).
     */
    protected int computeCheckPeriod ()
    {
        RollingCalendar rollingCalendar = new RollingCalendar(GMT_TIMEZONE, Locale.ENGLISH);
        Date epoch = new Date(0); // set date to 1970-01-01 00:00:00 GMT
        if (_datePattern != null) {
            for (int ii = TOP_OF_MINUTE; ii <= TOP_OF_MONTH; ii++) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(_datePattern);
                simpleDateFormat.setTimeZone(GMT_TIMEZONE); // do all date formatting in GMT
                String r0 = simpleDateFormat.format(epoch);
                rollingCalendar.setType(ii);
                Date next = rollingCalendar.getNextCheckDate(epoch);
                String r1 =  simpleDateFormat.format(next);
                // System.out.println("Type = " + ii + ", r0 = " + r0 + ", r1 = " + r1);
                if (r0 != null && r1 != null && !r0.equals(r1)) {
                    return ii;
                }
            }
        }
        return TOP_OF_TROUBLE; // Deliberately head for trouble...
    }

    /**
     * Rolls the current file over to a new file.
     */
    protected void rollOver () throws IOException
    {
        // compute filename, but only if datePattern is specified
        if (_datePattern == null) {
            errorHandler.error("Missing DatePattern option in rollOver().");
            return;
        }

        String datedFilename = this.fileName + _sdf.format(_now);
        // are we are still within the bounds of the current interval?
        if (_scheduledFilename.equals(datedFilename)) {
            return; // rollover will occur once the next interval is reached
        }

        // close current file, and rename it to datedFilename
        this.closeFile();

        // TODO: this is wack, we should rename the existing file to a .N file
        File target = new File(_scheduledFilename);
        if (target.exists()) {
            target.delete();
        }

        File file = new File(this.fileName);
        if (file.renameTo(target)) {
            LogLog.debug(this.fileName + " -> " + _scheduledFilename);
        } else {
            LogLog.error("Failed to rename " + this.fileName + " to " + _scheduledFilename + ".");
        }

        try {
            // this will also close the file; this is OK since multiple close operations are safe
            setFile(this.fileName, false, this.bufferedIO, this.bufferSize);
        } catch (IOException e) {
            errorHandler.error("setFile(" + this.fileName + ", false) call failed.");
        }
        _scheduledFilename = datedFilename;

        // if we are configured to generate and email an error summary, do so
        if (_email != null) {
            try {
                summarizeLog(target);
            } catch (IOException e) {
                errorHandler.error("Failed to summarize " + target, e, ErrorCode.GENERIC_FAILURE);
            }
        }

        // if we are configured to prune old log files, do so
        if (_pruneDays > 0) {
            try {
                pruneLogs(new File(fileName), System.currentTimeMillis(), _pruneDays);
            } catch (IOException e) {
                errorHandler.error("Failed to prune old logs", e, ErrorCode.GENERIC_FAILURE);
            }
        }
    }

    /**
     * Scans the supplied standard format log file for warnings and errors, summarizes them if a
     * summary database is configured and sends the report to the configured log summary address.
     */
    protected void summarizeLog (File target)
        throws IOException
    {
        StringBuilder sumbuf = new StringBuilder();
        summarizeLog(target, STANDARD, sumbuf);
        sendSummary(sumbuf.toString());
    }

    /**
     * Scans the supplied log file for warnings and errors, summarizes them if a summary database
     * is configured and sends the report to the configured log summary address.
     * @param target the file containing the log lines to summarize
     * @param format definition of the expected stlye of lines
     * @param sumbuf buffer to append summary information to
     */
    protected void summarizeLog (File target, LineFormat format, StringBuilder sumbuf)
        throws IOException
    {
        // read in our error database from the specified file
        ErrorDatabase errors = new ErrorDatabase();
        errors.readFrom(_database);

        // we need the current time to do our message filtering and pruning
        long nowStamp = System.currentTimeMillis();

        summarizeLog(target, "Filtered messages", format, errors, nowStamp, sumbuf);

        // finally prune old errors from our database and write it back out
        errors.pruneOldErrors(nowStamp);
        errors.writeTo(_database);
    }

    /**
     * Scans the supplied log file for warnings and errors, summarizes them if a summary database
     * is configured and sends the report to the configured log summary address.
     * @param target the file containing the log lines to summarize
     * @param format definition of the expected stlye of lines
     * @param errors database to use for detecting summaries
     * @param nowStamp time stamp to use when detecting summaries
     * @param sumbuf buffer to append summary information to
     */
    protected void summarizeLog (
        File target, String label, LineFormat format, ErrorDatabase errors, long nowStamp,
        StringBuilder sumbuf)
        throws IOException
    {
        // this will contain our filtered error counts
        Multiset<String> counts = HashMultiset.create();

        // now grind through the log file looking for errors and warnings; log lines are parsed
        // according to the LineFormat instance
        boolean areDisplaying = false;
        int strayLineCount = 0;
        BufferedReader reader = new BufferedReader(new FileReader(target));
        for (String line; (line = reader.readLine()) != null; ) {
            format.setLine(line);

            // Skip lines that are not important for this summary
            if (!format.isImportant()) {
                continue;
            }

            try {
                // If the line is unformatted, it is considered 'stray' and belongs with the most
                // recent formatted line.
                if (format.isStray()) {
                    if (areDisplaying) {
                        // Limit the number of stray lines, there's generally no need to put large
                        // amounts of log data into an email
                        if (strayLineCount >= 100) {
                            areDisplaying = false;
                            sumbuf.append("... (stray lines truncated)\n");
                        } else {
                            sumbuf.append(line).append("\n");
                            ++strayLineCount;
                        }
                    }
                    continue;
                }

                // the stray lines from the most recent message are finito, stop appending them
                areDisplaying = false;

                // skip anything less than WARN
                String level = format.extractLevel();
                if (!level.equals("WARN") && !level.equals("ERROR")) {
                    areDisplaying = false;
                    continue;
                }

                // obtain our message and its "id" which is the message minus unique info
                String msgid = format.extractMessageId();

                // now determine whether we display this message in its entirety
                if (!errors.shouldSummarize(nowStamp, msgid)) {
                    areDisplaying = true;
                    strayLineCount = 0;
                    sumbuf.append(line).append("\n");
                }

                // note that this error message occurred (we count all messages, even filtered)
                counts.add(msgid);

            } catch (Exception e) {
                LogLog.warn("Summarizer choked on '" + line + "'.", e);
            }
        }

        // generate a list of filtered messages sorted by occurance count
        List<Multiset.Entry<String>> sorted = Lists.newArrayList(counts.entrySet());
        Collections.sort(sorted, new Comparator<Multiset.Entry<String>>() {
                public int compare (Multiset.Entry<String> e1, Multiset.Entry<String> e2) {
                    return ComparisonChain.start()
                        .compare(e2.getCount(), e1.getCount()) // higher first
                        .compare(e1.getElement(), e2.getElement())
                        .result();
                }
            });

        // append the filtered messages to our summary buffer
        if (sorted.size() > 0) {
            if (sumbuf.length() > 0) {
                sumbuf.append("\n");
            }
            sumbuf.append(label).append(":\n");
            for (Multiset.Entry<String> entry : sorted) {
                sumbuf.append(String.format("%3d %s\n", entry.getCount(), entry.getElement()));
            }
        }
    }

    /**
     * Sends an email summary to our configured email address.
     */
    protected void sendSummary (String summary)
        throws IOException
    {
        // if after all that fun and adventure, we have anything to report, do so
        if (summary.length() > 0) {
            String subject =
                System.getProperty("hostname", "unknown") + " " + this.fileName + " summary";
            try {
                MailUtil.deliverMail(_email, _email, subject, summary);

            } catch (IOException ioe) {
                // Write the summary to stdout so it may be viewed manually
                java.io.PrintStream out = System.out;
                String date = (new SimpleDateFormat()).format(new Date());
                out.println("-- Mail failure @ " + date + "--");
                out.println("Subject: " + subject);
                out.println("Recipient: " + _email);
                out.println();
                out.println(summary);
                out.println("-- End of summary --");
                throw ioe;
            }
        }
    }

    /**
     * Prunes all files in the same directory as the specified file that start with the name
     * of the file and were last modified more than <code>pruneDays</code> ago.
     */
    protected void pruneLogs (final File logFile, long now, int pruneDays)
        throws IOException
    {
        if (logFile == null) {
            return;
        }
        long cutoff = now - pruneDays * 24*60*60*1000L;
        File dir = logFile.getParentFile();

        // iterate over all of the files that start with our logfile name
        for (File file : dir.listFiles(new FilenameFilter() {
            public boolean accept (File dir, String name) {
                return name.startsWith(logFile.getName());
            }
        })) {
            // if they are older than our cutoff, delete them
            long mod = file.lastModified();
            if (mod == 0) {
                errorHandler.error("Unable to get mtime " + file);
            } else if (mod < cutoff && !file.delete()) {
                errorHandler.error("Unable to delete " + file);
            }
        }
    }

    protected void printPeriodicity (int type)
    {
        switch(type) {
        case TOP_OF_MINUTE:
            LogLog.debug("Appender [" + name + "] to be rolled every minute.");
            break;
        case TOP_OF_HOUR:
            LogLog.debug("Appender [" + name + "] to be rolled on top of every hour.");
            break;
        case HALF_DAY:
            LogLog.debug("Appender [" +name + "] to be rolled at midday and midnight.");
            break;
        case TOP_OF_DAY:
            LogLog.debug("Appender [" + name + "] to be rolled at midnight.");
            break;
        case TOP_OF_WEEK:
            LogLog.debug("Appender [" + name + "] to be rolled at start of week.");
            break;
        case TOP_OF_MONTH:
            LogLog.debug("Appender [" + name + "] to be rolled at start of every month.");
            break;
        default:
            LogLog.warn("Unknown periodicity for appender [" + name + "].");
            break;
        }
    }

    /**
     * Checks whether a given line looks like a standard log line (ISO date stamp followed by a
     * category (like %d %p in log4j's <code>PatternLayout</code>) using a few key characters and
     * the length.
     * @see #LEVEL_OFFSET_IN_STANDARD_LOG_LINE
     */
    protected static boolean isProbablyStandardLogLine (String line)
    {
        // We're looking for lines starting with time stamps like 2008-09-24 17:04:26,047
        // Just do punctuation checks for speed.
        return line.length() >= 30 && line.charAt(4) == '-' && line.charAt(7) == '-' &&
            line.charAt(13) == ':' && line.charAt(16) == ':' && line.charAt(19) == ',';
    }

    /**
     * A helper class that given a periodicity type and the current time, it computes the start of
     * the next interval.
     */
    protected static class RollingCalendar extends GregorianCalendar
    {
        public RollingCalendar () {
            super();
        }

        public RollingCalendar (TimeZone tz, Locale locale) {
            super(tz, locale);
        }

        public void setType (int type) {
            _type = type;
        }

        public long getNextCheckMillis (Date now) {
            return getNextCheckDate(now).getTime();
        }

        public Date getNextCheckDate (Date now) {
            setTime(now);

            switch (_type) {
            case TOP_OF_MINUTE:
                set(Calendar.SECOND, 0);
                set(Calendar.MILLISECOND, 0);
                add(Calendar.MINUTE, 1);
                break;

            case TOP_OF_HOUR:
                set(Calendar.MINUTE, 0);
                set(Calendar.SECOND, 0);
                set(Calendar.MILLISECOND, 0);
                add(Calendar.HOUR_OF_DAY, 1);
                break;

            case HALF_DAY:
                set(Calendar.MINUTE, 0);
                set(Calendar.SECOND, 0);
                set(Calendar.MILLISECOND, 0);
                if (get(Calendar.HOUR_OF_DAY) < 12) {
                    set(Calendar.HOUR_OF_DAY, 12);
                } else {
                    set(Calendar.HOUR_OF_DAY, 0);
                    add(Calendar.DAY_OF_MONTH, 1);
                }
                break;

            case TOP_OF_DAY:
                set(Calendar.HOUR_OF_DAY, 0);
                set(Calendar.MINUTE, 0);
                set(Calendar.SECOND, 0);
                set(Calendar.MILLISECOND, 0);
                add(Calendar.DATE, 1);
                break;

            case TOP_OF_WEEK:
                set(Calendar.DAY_OF_WEEK, getFirstDayOfWeek());
                set(Calendar.HOUR_OF_DAY, 0);
                set(Calendar.MINUTE, 0);
                set(Calendar.SECOND, 0);
                set(Calendar.MILLISECOND, 0);
                add(Calendar.WEEK_OF_YEAR, 1);
                break;

            case TOP_OF_MONTH:
                set(Calendar.DATE, 1);
                set(Calendar.HOUR_OF_DAY, 0);
                set(Calendar.MINUTE, 0);
                set(Calendar.SECOND, 0);
                set(Calendar.MILLISECOND, 0);
                add(Calendar.MONTH, 1);
                break;

            default:
                throw new IllegalStateException("Unknown periodicity type.");
            }

            return getTime();
        }

        protected int _type = TOP_OF_TROUBLE;
    }

    /**
     * Defines how lines are interpreted during log summarization.
     */
    protected static interface LineFormat
    {
        /**
         * Sets the current line for summarization.
         */
        void setLine (String line);

        /**
         * Returns true if the current line should be processed in the summary.
         */
        boolean isImportant ();

        /**
         * Returns true if the current line is a continuation of a previous line, such as a line
         * from a stack trace or other unknown output.
         */
        boolean isStray ();

        /**
         * Extracts and returns the level of the given line such as WARN or INFO. This may only be
         * called on important, non-stray lines.
         */
        String extractLevel ();

        /**
         * Extracts and returns the message id of the given line (excluding the date stamp and
         * specific situational data). This may only be called after <code>extractLevel</code>.
         */
        String extractMessageId ();
    }

    /** The standard way of handling log lines. */
    protected static class StandardLineFormat implements LineFormat
    {
        public void setLine (String line) {
            _line = line;
            _level = null;
            _bidx = -1;
        }

        public boolean isImportant () {
            // Always parse the whole log.
            return true;
        }

        public boolean isStray () {
            return !isProbablyStandardLogLine(_line);
        }

        public String extractLevel () {
            // Up to 5 characters after the date stamp.
            _level = _line.substring(
                LEVEL_OFFSET_IN_STANDARD_LOG_LINE, LEVEL_OFFSET_IN_STANDARD_LOG_LINE + 5).trim();
            return _level;
        }

        public String extractMessageId () {
            // The unique part is from there up the opening bracket, if present
            int offset = LEVEL_OFFSET_IN_STANDARD_LOG_LINE + _level.length() + 1;
            // strip off '[' and anything that follows
            _bidx = _line.indexOf("[", offset);
            if (_bidx == -1) {
                _bidx = _line.length();
            }
            return _line.substring(offset, _bidx).trim();
        }

        /** The current line being parsed. */
        protected String _line;

        /** The severity of the current line, set by extractLevel. */
        protected String _level;

        /** The index of the opening square bracket in the line, set by extractMessageId. */
        protected int _bidx;
    }

    protected static final LineFormat STANDARD = new StandardLineFormat();

    /** The date pattern. The default pattern is "'.'yyyy-MM-dd" meaning daily rollover. */
    protected String _datePattern = "'.'yyyy-MM-dd";

    /** The path to the database file. */
    protected String _database;

    /** The email address to which to mail log summaries. */
    protected String _email;

    /** Any log files older than this number of days will be pruned (zero means no pruning). */
    protected int _pruneDays;

    /** The log file will be renamed to the value of the scheduledFilename variable when the next
     * interval is entered. For example, if the rollover period is one hour, the log file will be
     * renamed to the value of "scheduledFilename" at the beginning of the next hour. The precise
     * time when a rollover occurs depends on logging activity. */
    protected String _scheduledFilename;

    /** The next time we estimate a rollover should occur. */
    protected long _nextCheck;

    protected Date _now = new Date();
    protected SimpleDateFormat _sdf;
    protected RollingCalendar _rc = new RollingCalendar();

    // used only in computeCheckPeriod() method
    protected static final TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("GMT");

    // the code assumes that the following constants are in a increasing sequence
    protected static final int TOP_OF_TROUBLE=-1;
    protected static final int TOP_OF_MINUTE = 0;
    protected static final int TOP_OF_HOUR   = 1;
    protected static final int HALF_DAY      = 2;
    protected static final int TOP_OF_DAY    = 3;
    protected static final int TOP_OF_WEEK   = 4;
    protected static final int TOP_OF_MONTH  = 5;

    /** Position of the level specifier in a standard log line, just after the ISO date stamp. */
    protected static final int LEVEL_OFFSET_IN_STANDARD_LOG_LINE = 24;
}
