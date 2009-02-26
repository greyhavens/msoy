// $Id: ForumPostResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.TreeSet;

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.HadoopSerializationUtil;
import com.threerings.panopticon.reporter.aggregator.result.AggregatedResult;

/**
 * Custom result for forum (aka discussion) message posting events.
 *
 * @author matt
 */
public class ForumPostResult implements AggregatedResult<ForumPostResult>
{
    /**
     * Combines the given result with this one as part of the reduce step.
     *
     * @param result
     *            the result to add to this one.
     */
    public void combine (ForumPostResult result)
    {
        posts += result.posts;
        newThreads += result.newThreads;
        uniqueThreads.addAll(result.uniqueThreads);
        uniqueMembers.addAll(result.uniqueMembers);
    }

    /**
     * Adds the given event data to this result.
     */
    public boolean init (EventData eventData)
    {
        // increment the message post count
        posts++;

        Integer postNumber = (Integer) eventData.getData().get("postNumber");

        // if this was the first posting to the thread, increment the new thread
        // counter
        if (postNumber.intValue() == 1) {
            newThreads++;
        }

        // add the member ID to the set of unique posters
        Integer memberId = (Integer) eventData.getData().get("memberId");
        uniqueMembers.add(memberId);

        // add thread to the set of unique, active discussion threads
        Integer threadId = (Integer) eventData.getData().get("threadId");
        uniqueThreads.add(threadId);

        // return true indicating that the data was valid
        return true;
    }

    /**
     * Puts the values of this result's fields into the given result map.
     */
    public boolean putData (Map<String, Object> result)
    {
        result.put("posts", Integer.valueOf(posts));
        result.put("newThreads", Integer.valueOf(newThreads));
        result.put("uniqueThreads", Integer.valueOf(uniqueThreads.size()));
        result.put("uniqueMembers", Integer.valueOf(uniqueMembers.size()));

        // This aggregator only generates one result event. Returning false lets
        // the caller know that we are done generating results.
        return false;
    }

    /**
     * Required by hadoop for serialization.
     */
    @SuppressWarnings("unchecked")
    public void readFields (DataInput in) throws IOException
    {
        posts = in.readInt();
        newThreads = in.readInt();
        uniqueMembers = (TreeSet<Integer>)HadoopSerializationUtil.readObject(in);
        uniqueThreads = (TreeSet<Integer>)HadoopSerializationUtil.readObject(in);
    }

    /**
     * Required by hadoop for serialization.
     */
    public void write (DataOutput out) throws IOException
    {
        out.writeInt(posts);
        out.writeInt(newThreads);
        HadoopSerializationUtil.writeObject(out, uniqueMembers);
        HadoopSerializationUtil.writeObject(out, uniqueThreads);
    }

    /**
     * The total number of message posts.
     */
    private int posts;

    /**
     * Used to count the number of new discussion threads created.
     */
    private int newThreads;

    /**
     * Used to count the number of unique members who posted a message.
     */
    private TreeSet<Integer> uniqueMembers = new TreeSet<Integer>();

    /**
     * Used to count the number of unique discussion threads that had at least
     * one posting.
     */
    private TreeSet<Integer> uniqueThreads = new TreeSet<Integer>();

}
