// $Id: InvitesResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.TreeSet;

import org.apache.hadoop.io.WritableComparable;

import com.threerings.panopticon.aggregator.HadoopSerializationUtil;
import com.threerings.panopticon.aggregator.result.AggregatedResult;
import com.threerings.panopticon.common.event.EventData;

public class InvitesResult implements AggregatedResult<WritableComparable<?>, InvitesResult>
{
    public void combine (InvitesResult result)
    {
        this.invitesSent.addAll(result.invitesSent);
        this.invitesAccepted.addAll(result.invitesAccepted);
        this.invitesFollowed.addAll(result.invitesFollowed);
        this.inviteSenders.addAll(result.inviteSenders);
    }

    public boolean init (WritableComparable<?> key, EventData eventData)
    {
        // Add the invite ID to the appropriate sets
        String inviteId = (String)eventData.getData().get("inviteId");
        invitesSent.add(inviteId);
        inviteSenders.add((Integer)eventData.getData().get("inviterId"));
        if ((Boolean)eventData.getData().get("followed")) {
            invitesFollowed.add(inviteId);
        }
        if ((Boolean)eventData.getData().get("accepted")) {
            invitesAccepted.add(inviteId);
        }

        return true;
    }

    public boolean putData (Map<String, Object> result)
    {
        result.put("sent", invitesSent.size());
        result.put("followed", invitesFollowed.size());
        result.put("accepted", invitesAccepted.size());
        result.put("senders", inviteSenders.size());
        result.put("avgSentPerSender", (double)invitesSent.size() / (double)inviteSenders.size());

        return false;
    }

    @SuppressWarnings("unchecked")
    public void readFields (DataInput in)
        throws IOException
    {
        invitesSent = (TreeSet<String>)HadoopSerializationUtil.readObject(in);
        invitesFollowed = (TreeSet<String>)HadoopSerializationUtil.readObject(in);
        invitesAccepted = (TreeSet<String>)HadoopSerializationUtil.readObject(in);
        inviteSenders = (TreeSet<Integer>)HadoopSerializationUtil.readObject(in);
    }

    public void write (DataOutput out)
        throws IOException
    {
        HadoopSerializationUtil.writeObject(out, invitesSent);
        HadoopSerializationUtil.writeObject(out, invitesFollowed);
        HadoopSerializationUtil.writeObject(out, invitesAccepted);
        HadoopSerializationUtil.writeObject(out, inviteSenders);
    }

    private TreeSet<String> invitesSent = new TreeSet<String>();
    private TreeSet<String> invitesFollowed = new TreeSet<String>();
    private TreeSet<String> invitesAccepted = new TreeSet<String>();
    private TreeSet<Integer> inviteSenders = new TreeSet<Integer>();
}
