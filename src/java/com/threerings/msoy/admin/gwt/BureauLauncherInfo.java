//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.io.Serializable;

import com.threerings.io.Streamable;

/**
 * Information about a bureau launcher.
 */
public class BureauLauncherInfo
    implements Streamable, Serializable
{
    /**
     * Information about a bureau.
     */
    public static class BureauInfo
        implements Streamable, Serializable
    {
        /** The id of the bureau. */
        public String bureauId;

        /** The time the bureau last launched, 0 if never. */
        public long launchTime;

        /** The time the bureau last exited, 0 if never. */
        public long shutdownTime;

        /** NUmber of bytes used in log. */
        public int logSpaceUsed;

        /** NUmber of bytes left in log allowance. */
        public int logSpaceRemaining;

        /** Generic message. */
        public String message;

        public String toString ()
        {
            return "BureauInfo(bureauId=" + bureauId + ", launchTime=" + launchTime +
                ", shutdownTime=" + shutdownTime + ", logSpaceUsed=" + logSpaceUsed +
                ", logSpaceRemaining=" + logSpaceRemaining + ")";
        }
    }

    /** Name of the machine running the launcher. */
    public String hostname;

    /** The past and present bureaus. */
    public BureauInfo[] bureaus;

    /** The servers the launcher is connected to. */
    public String[] connections;

    /** Changes each time this data is updated. */
    public int version;

    public String toString ()
    {
        StringBuffer buff = new StringBuffer();
        buff.append("BureauLauncherInfo(");
        buff.append("hostname=").append(hostname);
        buff.append(", bureaus=(");
        for (int ii = 0; ii < bureaus.length; ++ii) {
            if (ii > 0) {
                buff.append(", ");
            }
            buff.append(bureaus[ii].toString());
        }
        buff.append("), connections=(");
        for (int ii = 0; ii < connections.length; ++ii) {
            if (ii > 0) {
                buff.append(", ");
            }
            buff.append(connections[ii].toString());
        }
        buff.append("), version=").append(version);
        buff.append(")");
        return buff.toString();
    }
}
