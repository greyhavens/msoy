// $Id: CountABTestResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;

import com.google.common.base.Preconditions;

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.HadoopSerializationUtil;
import com.threerings.panopticon.reporter.aggregator.result.PropertiesAggregatedResult;

/**
 * Counts up A/B test actions for all players participating in some chosen set of tests.
 * Also brings in data from the AccountCreated table, and outputs it as conversion.
 *
 * Required config options:
 * <ul>
 *      <li><b>testRegex</b>: Regular expression to match the tests to be processed.</li>
 *      <li><b>actionRegex</b>: Regular expression to match actions to be aggregated.</li>
 * </ul>
 *
 * @author Robert Zubek <robert@threerings.net>
 */
public class CountABTestResult implements PropertiesAggregatedResult<CountABTestResult>
{

    public void configure (Configuration config)
    {
        testRegex = config.getString("testRegex");
        actionRegex = config.getString("actionRegex");
    }

    public void combine (final CountABTestResult aggregate)
    {
        final CountABTestResult other = aggregate;

        // merge group sets and action sets
        this.actions.addAll(other.actions);
        this.groups.addAll(other.groups);
        if (other.vector.length() > 0) {
            this.vector = other.vector;
        }
    }

    public boolean init (final EventData eventData)
    {
        Preconditions.checkState(testRegex != null && actionRegex != null);

        String name = eventData.getEventName().getShortName();

        if (CLIENT_ACTION_TABLE.equals(name)) {
            // this table stores individual client actions, with or without tests
            return processClientAction(eventData, actionRegex);

        } else if (TEST_ACTION_TABLE.equals(name)) {
            // this table stores test actions, and test group assignments
            return processTestAssignment(eventData, testRegex, actionRegex);

        } else if (BEHAVIOR_TABLE.equals(name)) {
            return processBehavior(eventData);
        }

        throw new RuntimeException("CountABTestResult encountered unknown event: " + name);
    }

    private boolean processBehavior (final EventData eventData)
    {
        if (((Number) eventData.getData().get("conv")).intValue() > 0) {
            this.actions.add(CONVERTED_ACTION);
        }
        if (((Number) eventData.getData().get("ret")).intValue() > 0) {
            this.actions.add(RETAINED_ACTION);
        }

        return true;
    }

    private boolean processClientAction (final EventData eventData, final String actionRegex)
    {
        // get the client action name
        final Object o = eventData.getData().get("actionName");
        if (o == null) {
            return false;
        }

        final String action = o.toString();

        if (!Pattern.matches(actionRegex, action)) {
            return false;
        }

        this.actions.add(action);
        return true;
    }

    private boolean processTestAssignment (
        final EventData eventData, final String testRegex, final String actionRegex)
    {
        final Map<String, Object> data = eventData.getData();

        // see if this test is one of those we want
        final String test = (String) data.get("testName");
        if (test == null) {
            return false;
        }

        if (!Pattern.matches(testRegex, test)) {
            return false; // skip this one
        }

        // pull out the action, if it's not a test assignment, process it elsewhere
        final Object action = data.get("actionName");
        if (!"ABTestGroupAssigned".equals(action)) {
            return processClientAction(eventData, actionRegex);
        }

        // get the group
        final Object group = data.get("testGroup");
        if (group == null) {
            return false;
        }

        // convert to a single field, and store it
        String testAndGroup = String.format("%s - %s", test, group);
        this.groups.add(testAndGroup);
        this.actions.add(ASSIGNED_ACTION);

        return true;
    }

    public boolean putData (final Map<String, Object> data)
    {
        if (outputState == null) {
            this.outputState = new OutputState();
        }

        if (! outputState.hasNext()) {
            return false;
        }

        // get the next datum
        outputState.advance();

        data.put("_action", outputState.currentAction);

        // get constituents
        final String[] elts = outputState.currentGroup.split(" - ");
        final String test = elts[0];
        final String group = elts[1];

        data.put("ab_test", test);
        data.put("ab_test_grp", group);
        data.put("ab_vector", vector);

        data.put("count", 1);

        return outputState.hasNext();
    }

    public void readFields (final DataInput in)
        throws IOException
    {
        this.vector = in.readUTF();

        @SuppressWarnings("unchecked")
        Set<String> otherActions = (Set<String>) HadoopSerializationUtil.readObject(in);
        @SuppressWarnings("unchecked")
        Set<String> otherGroups = (Set<String>) HadoopSerializationUtil.readObject(in);

        this.actions.clear();
        this.actions.addAll(otherActions);

        this.groups.clear();
        this.groups.addAll(otherGroups);
    }

    public void write (final DataOutput out)
        throws IOException
    {
        out.writeUTF(this.vector);
        HadoopSerializationUtil.writeObject(out, this.actions);
        HadoopSerializationUtil.writeObject(out, this.groups);
    }

    @Override
    public String toString()
    {
        return String.format("[CountABTestResult - actions: %s - groups: %s]",
                             this.actions, this.groups);
    }

    /**
     * Maintains state between putData invocations.
     * It's a way produce multiple output values from a single reduce step.
     */
    private class OutputState {
        public OutputState () {
            this.groupIter = groups.iterator();
        }

        public boolean hasNext () {
            return groupIter.hasNext() ||
                   (actionIter != null && actionIter.hasNext());
        }

        public void advance () {
            // if the action iterator ran out of elements, advance to the next test group
            while (currentGroup == null ||
                   (!actionIter.hasNext() && groupIter.hasNext()))
            {
                currentGroup = groupIter.next();
                actionIter = actions.iterator();
            }
            // now get the next action (assuming we're in valid state)
            currentAction = (actionIter.hasNext() ? actionIter.next() : null);
        }

        public String getCurrentAction () {
            return currentAction;
        }

        public String getCurrentGroup () {
            return currentGroup;
        }

        public Iterator<String> groupIter;
        public String currentGroup;
        public Iterator<String> actionIter;
        public String currentAction;
    }


    // names of various tables to be processed
    private final static String CLIENT_ACTION_TABLE = "ClientAction";
    private final static String TEST_ACTION_TABLE = "TestAction";
    private final static String BEHAVIOR_TABLE = "AllGuestBehavior";

    /** Special action added to the result set if the player converted. */
    private final static String CONVERTED_ACTION = "_converted";

    /** Special action added to the result set if the player converted. */
    private final static String RETAINED_ACTION = "_retained";

    /** Special action added to the result set for each player assigned to a group. */
    private final static String ASSIGNED_ACTION = "_assigned";

    /** Affiliate info for this tracking number. */
    private String vector = "";

    /** Set of all test/group assignments for this tracking number. */
    private final Set<String> groups = new HashSet<String>();

    /** Set of all testable actions performed by this tracking number. */
    private final Set<String> actions = new HashSet<String>();

    private OutputState outputState = null;

    private String testRegex, actionRegex;
}
