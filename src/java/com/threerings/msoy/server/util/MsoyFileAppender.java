//
// $Id$

package com.threerings.msoy.server.util;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.samskivert.util.Comparators;
import com.threerings.util.OOOFileAppender;

/**
 * Extends the file appended to parse additional msoy information.
 */
public class MsoyFileAppender extends OOOFileAppender
{
    /**
     * Run the msoy file appender on a named target file for testing.
     */
    public static void main (String args[])
        throws IOException
    {
        MsoyFileAppender appender = new MsoyFileAppender();
        File target = new File(args[0]);
        StringBuilder sumbuf = appender.summarizeLogToBuffer(target);
        System.out.print(sumbuf);
    }

    @Override // from OOOFileAppender
    protected void summarizeLog (File target)
        throws IOException
    {
        StringBuilder sumbuf = summarizeLogToBuffer(target);
        sendSummary(sumbuf.toString());
    }

    protected StringBuilder summarizeLogToBuffer (File target)
        throws IOException
    {
        StringBuilder sumbuf = new StringBuilder();
        MsoyLineFormat format = new MsoyLineFormat();
        summarizeLog(target, format, sumbuf);
        format.summarizeLongUnits(sumbuf);
        return sumbuf;
    }

    protected static class UnitInfo
    {
        public String name;
        public int minTime = Integer.MAX_VALUE;
        public int maxTime;
        public double average;
        public int count;
        public TreeSet<String> instances = Sets.newTreeSet();

        public UnitInfo (String name) {
            this.name = name;
        }

        public void aggregate (String instance, int time) {
            if (time < minTime) {
                minTime = time;
            }
            if (time > maxTime) {
                maxTime = time;
            }
            average += (time - average) / (++count);
            instances.add(instance);
        }

        public String describe (int maxLen) {
            maxLen -= 5; // for ", ..."
            StringBuilder desc = new StringBuilder(name);
            if (desc.length() < maxLen) {
                desc.append(" - ");
                int ii = 0;
                for (String inst : instances) {
                    desc.append(ii++ > 0 ? ", " : "");
                    if (desc.length() + inst.length() > maxLen) {
                        desc.append("...");
                        break;
                    }
                    desc.append(inst);
                }
            }
            return desc.toString();
        }
    }

    protected static class MsoyLineFormat extends StandardLineFormat
    {
        @Override // from StandardLineFormat
        public String extractMessageId () {
            String id = super.extractMessageId();
            if (_bidx > -1 && ("Invoker: Long invoker unit".equals(id) ||
                "Invoker: Really long invoker unit".equals(id))) {
                String unit = _line.substring(_bidx + 1);
                String key = unit;
                String instance = "";
                int time = 0;
                Matcher m = UNIT_PATTERN.matcher(unit);
                if (m.lookingAt()) {
                    instance = m.group(1);
                    key = m.group(2);
                    time = Integer.parseInt(m.group(3));
                }
                UnitInfo uinf = _longUnits.get(key);
                if (uinf == null) {
                    _longUnits.put(key, uinf = new UnitInfo(key));
                }
                uinf.aggregate(instance, time);
            }
            return id;
        }

        public void summarizeLongUnits (StringBuilder sumbuf)
        {
            if (_longUnits.size() == 0) {
                return;
            }

            // sort by number of occurrences
            TreeSet<UnitInfo> sorted = new TreeSet<UnitInfo>(
                new Comparator<UnitInfo>() {
                    public int compare (UnitInfo uinfo1, UnitInfo uinfo2) {
                        int rv = Comparators.compare(uinfo2.count, uinfo1.count);
                        return (rv != 0) ? rv : uinfo1.name.compareTo(uinfo2.name);
                    }
                });
            sorted.addAll(_longUnits.values());

            // append the label
            if (sumbuf.length() > 0) {
                sumbuf.append("\n");
            }
            sumbuf.append("Long invoker units:\n");

            // append the table header
            final int descLen = 70;
            String descFmt = "%-" + descLen + "." + descLen + "s";
            String hfmt = "   %5.5s %5.5s %5.5s %5.5s " + descFmt + "\n";
            sumbuf.append(String.format(hfmt, "Count", "Min", "Max", "Avg", "Description"));
            String sep = "----------------------------------------------------------------------";
            sumbuf.append(String.format(hfmt, sep, sep, sep, sep, sep));

            // append the stats and description columns for each unit
            String rfmt = "   %5d %5s %5s %5s " + descFmt + "\n";
            for (UnitInfo ui : sorted) {
                sumbuf.append(String.format(
                    rfmt, ui.count, ui.minTime, ui.maxTime, (int)ui.average, ui.describe(descLen)));
            }
        }

        protected HashMap<String, UnitInfo> _longUnits = Maps.newHashMap();

        /**
         * Pattern for parsing long units log strings, e.g:
         * <pre>
         * unit=resolveScene(9) (class com.threerings.whirled.server.SceneRegistry$1), time=1507ms
         * </pre>
         */
        protected static final Pattern UNIT_PATTERN = Pattern.compile(
            "unit=(.+) \\(class .*\\.([^.]+)\\), time=(\\d+)ms");
    }
}
