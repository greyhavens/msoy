// $Id: GameEmbedVisitorsResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.result.AggregatedResult;

/**
 * Joins together one ReferralCreated event, zero or more ClientAction events, and zero or one
 * AccountCreated event by the common "tracker" key. Output data for trackers who have been
 * referred via a game embed only:
 *
 * timestamp - when the referral occured
 *
 * conv - 1 if visitor registered, otherwise 0
 *
 * click - 1 if visitor clicked through the main site, otherwise 0
 *
 * @author Sarah Collins <sarah@threerings.net>
 */
public class GameEmbedVisitorsResult implements AggregatedResult<GameEmbedVisitorsResult>
{
    public boolean init (final EventData eventData)
    {
        final Object trackerObject = eventData.getData().get("tracker");
        if (trackerObject != null) {
            tracker = (String)trackerObject;
        } else {
            final Object trackerObject2 = eventData.getData().get("visitorId");
            if (trackerObject2 != null) {
                tracker = (String)trackerObject2;
            }
        }

        String name = eventData.getEventName().getShortName();
        if (CLIENT_ACTION_TABLE.equals(name)) {
            final Object actionObject = eventData.getData().get("actionName");
            if (actionObject == null) {
                return false;
            }
            final String action = actionObject.toString();

            if (action.equals(EMBED_LANDING_ACTION)) {
                embedLanding = true;
            } else {
                for (String clickAction : CLICKTHROUGH_ACTIONS) {
                    if (action.equals(clickAction)) {
                        clickedThrough = true;
                        return true;
                    }
                }
                // ignore all other ClientEvents
                return false;
            }

        } else if (CONVERSION_TABLE.equals(name)) {
            converted = true;

        } else if (VISITOR_INFO_TABLE.equals(name)) {
            final Object timestampObject = eventData.getData().get("timestamp");
            if (timestampObject != null) {
                timestamp = (Date)timestampObject;
            }

        } else if (VECTOR_TABLE.equals(name)) {
            final Object vectorObject = eventData.getData().get("vector");
            if (vectorObject != null && !("".equals(vectorObject))) {
                vector = vectorObject.toString();
            }
        } else {
            throw new RuntimeException("GameEmbedVisitorsResult encountered unknown event: "
                + name);
        }

        return true;
    }

    public void combine (final GameEmbedVisitorsResult other)
    {
        // System.out.println("combine tracker is " + tracker + ", other is " + other.tracker);
        // merge all events that happened at least once
        if (other.vector.length() > 0) {
            vector = other.vector;
        }
        if (other.embedLanding) {
            embedLanding = true;
        }
        if (other.converted) {
            converted = true;
        }
        if (other.clickedThrough) {
            clickedThrough = true;
        }
        if (other.timestamp != null) {
            timestamp = other.timestamp;
        }

        if (other.tracker != "") {
            tracker = other.tracker;
        }
    }

    public boolean putData (final Map<String, Object> data)
    {
        // only include data from visitors who came through embedded games.
        if (vector == null || embedLanding == false) {
            return false;
        }

        if (!GAMES_VECTOR_PATTERN.matcher(vector).find() &&
            !AVRG_VECTOR_PATTERN.matcher(vector).find()) {
            return false;
        }

        // If there's no timestamp, no referral was created, and we need to ignore this record.
        if (timestamp == null) {
            return false;
        }

        // record the referral time, and "1" count for the actions that occured
        data.put(TIMESTAMP_OUTPUT, timestamp);
        data.put(CONVERTED_OUTPUT, converted ? 1 : 0);
        data.put(CLICKTHROUGH_OUTPUT, clickedThrough ? 1 : 0);
        return false;
    }

    public void readFields (final DataInput in)
        throws IOException
    {
        long timestampLong = in.readLong();
        if (timestampLong != 0) {
            timestamp = new Date(timestampLong);
        }
        vector = in.readUTF();
        embedLanding = in.readBoolean();
        converted = in.readBoolean();
        clickedThrough = in.readBoolean();
        tracker = in.readUTF();
    }

    public void write (final DataOutput out)
        throws IOException
    {
        out.writeLong(timestamp == null ? 0 : timestamp.getTime());
        out.writeUTF(vector);
        out.writeBoolean(embedLanding);
        out.writeBoolean(converted);
        out.writeBoolean(clickedThrough);
        out.writeUTF(tracker);
    }

    @Override
    public String toString()
    {
        return String.format("[GameEmbedVisitorsResult - vector: " + vector + "embedLanding: "
            + embedLanding + "converted: " + converted + " clickedThrough: " + clickedThrough);
    }

    // names of various tables to be processed
    private final static String CLIENT_ACTION_TABLE = "ClientAction";
    private final static String CONVERSION_TABLE = "AccountCreated";
    private final static String VISITOR_INFO_TABLE = "VisitorInfoCreated";
    private final static String VECTOR_TABLE = "VectorAssociated";

    /** The types of ClientAction that denotes moving from an embed to the main whirled site. */
    private final static String[] CLICKTHROUGH_ACTIONS = { "flashFullVersionClicked",
        "flashCreateAccount", "flashViewGameInstructions", "flashViewGameComments",
        "flashViewGames" };

    /** The type of ClientAction that denotes hitting an embedded flash client. */
    private final static String EMBED_LANDING_ACTION = "embeddedLogon";

    /** The referral vector that represents landing on a game */
    private final static Pattern GAMES_VECTOR_PATTERN = Pattern.compile("games\\.\\d+");
    private final static Pattern AVRG_VECTOR_PATTERN = Pattern.compile("world\\.g\\d+");

    // names of the fields to output
    private final static String TIMESTAMP_OUTPUT = "timestamp";
    private final static String CONVERTED_OUTPUT = "conv";
    private final static String CLICKTHROUGH_OUTPUT = "click";

    /** Timestamp when referral first occured */
    private Date timestamp;

    /** Landing vector for this tracking number. We only record visitors with a "games" vector */
    private String vector = "";

    /** Whether the visitor loaded an embedded client. We only record visitors who do */
    private boolean embedLanding = false;

    /** Whether the visitor registered a new account */
    private boolean converted = false;

    /** Whether the visitor continued to the main site */
    private boolean clickedThrough = false;

    private String tracker = "";
}
