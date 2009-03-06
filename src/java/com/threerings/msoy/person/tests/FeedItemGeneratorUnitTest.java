//
// $Id$

package com.threerings.msoy.person.tests;

import java.util.ArrayList;
import java.util.List;

import org.junit.*;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.person.gwt.AggregateFeedMessage;
import com.threerings.msoy.person.gwt.FeedItemGenerator;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageAggregator;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.gwt.FriendFeedMessage;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Builder;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Icon;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Media;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Messages;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Plural;

import com.threerings.msoy.web.gwt.Pages;

import static org.junit.Assert.*; 

public class FeedItemGeneratorUnitTest
{
    @Test public void testLevelGainAggregation ()
    {
        long now = System.currentTimeMillis();
        List<FriendFeedMessage> levelGains = new ArrayList<FriendFeedMessage>();
        for (int ii = 0; ii < 20; ++ii) {
            levelGains.add(newLevelGain(now, ii + 1));
        }
        int maxItems = FeedMessageAggregator.MAX_AGGREGATED_ITEMS;
        List<FeedMessage> aggregated = FeedMessageAggregator.aggregate(
            levelGains.toArray(new FeedMessage[levelGains.size()]), false);
        assertEquals(1, aggregated.size());
        assertTrue(aggregated.get(0) instanceof AggregateFeedMessage);
        assertEquals(maxItems, ((AggregateFeedMessage)aggregated.get(0)).messages.size());

        StubBuilder builder = new StubBuilder(false);
        StubMessages messages = new StubMessages();
        FeedItemGenerator gen = new FeedItemGenerator(1, true, builder, messages);
        for (FeedMessage fm : aggregated) {
            gen.addMessage(fm);
        }

        //builder.calls.dump(System.out, "builder.calls", builder.getClass());
        //messages.calls.dump(System.out, "messages.calls", messages.getClass());

        assertEquals(builder.calls.getCount("createGainedLevelIcon"), 1);
        assertEquals(builder.calls.getCount("createLink"), maxItems - 1);
        assertEquals(messages.calls.getCount("briefLevelGain"), maxItems);
        assertEquals(messages.calls.getCount("action"), 1);
        assertEquals(messages.calls.getCount("commaCombine"), 3);
        assertEquals(messages.calls.getCount("andCombine"), 1);
    }

    public static class StubBuilder
        implements Builder
    {
        public MethodCounter calls = new MethodCounter();
        public boolean echo;

        public StubBuilder (boolean echo) {
            this.echo = echo;
        }

        public void addIcon (Icon icon) {
            calls.count();
            echo(icon);
        }

        public void addMedia (Media media, String message) {
            calls.count();
            echo(media + ", message: " + message);
        }

        public void addMedia (Media[] media, String message) {
            calls.count();
            echo(StringUtil.toString(media) + ", message: " + message);
        }

        public void addText (String text) {
            calls.count();
            echo(text);
        }

        public Icon createGainedLevelIcon (String text) {
            calls.count();
            return new StubItem(text);
        }

        public String createLink (String label, Pages page, String args) {
            calls.count();
            return simpleToString("link", "label", label, "page", page, "args", args);
        }

        public Media createMedia (MediaDesc md, Pages page, String args) {
            calls.count();
            return new StubItem(md, page, args);
        }

        protected void echo (Object t)
        {
            if (echo) {
                System.out.println(t);
            }
        }
    }

    public static String simpleToString (String name, Object... nvPairs)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" (");
        for (int ii = 0; ii < nvPairs.length; ii += 2) {
            if (ii > 0) {
                sb.append(", ");
            }
            sb.append(nvPairs[ii]).append(": ").append(nvPairs[ii+1]);
        }
        sb.append(")");
        return sb.toString();
    }

    public static class StubMessages
        implements Messages
    {
        public MethodCounter calls = new MethodCounter();

        public String action (FeedMessageType type, String subject, String object, Plural plural) {
            calls.count();
            return simpleToString("action", "subject", subject, "type", type, "object", object,
                "plural", plural);
        }

        public String andCombine (String list, String item) {
            calls.count();
            return simpleToString("and", "list", list, "item", item);
        }

        public String badgeName (int code, String levelName) {
            calls.count();
            return simpleToString("badge", "code", code, "levelName", levelName);
        }

        public String briefLevelGain (String subject, String level) {
            calls.count();
            return simpleToString("levelGain", "subject", subject, "level", level);
        }

        public String commaCombine (String list, String item) {
            calls.count();
            return simpleToString("comma", "list", list, "item", item);
        }

        public String describeItem (String typeName, String itemName) {
            calls.count();
            return simpleToString("item", "type", typeName, "name", itemName);
        }

        public String medal (String medal, String group) {
            calls.count();
            return simpleToString("medal", "name", medal, "group", group);
        }

        public String noGroupForMedal (String medalLink) {
            calls.count();
            return simpleToString("medal", "name", medalLink);
        }

        public String typeName (String itemType) {
            calls.count();
            return simpleToString("itemtype", "type", itemType);
        }

        public String unknownMember () {
            calls.count();
            return "unknownMember";
        }

        public String you () {
            calls.count();
            return "you";
        }
    }

    public static class StubItem
        implements Icon, Media
    {
        public StubItem (MediaDesc md, Pages page, String args)
        {
            _text = simpleToString("media", "desc", md, "page", page, "args", args);
        }

        public StubItem (String text)
        {
            _text = simpleToString("icon", "text", text);
        }

        public String toString ()
        {
            return _text;
        }

        protected String _text;
    }

    protected static FriendFeedMessage newLevelGain (long posted, int memberId)
    {
        FriendFeedMessage ffm = new FriendFeedMessage();
        ffm.friend = new MemberName("Test Friend " + memberId, memberId);
        ffm.posted = posted;
        ffm.type = FeedMessageType.FRIEND_GAINED_LEVEL;
        ffm.data = new String[] {"10"};
        return ffm;
    }

}
