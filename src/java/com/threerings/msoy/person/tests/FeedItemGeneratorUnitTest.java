//
// $Id$

package com.threerings.msoy.person.tests;

import java.util.ArrayList;
import java.util.List;

import org.junit.*;

import com.google.common.collect.Lists;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.person.gwt.AggregateFeedMessage;
import com.threerings.msoy.person.gwt.FeedItemGenerator;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageAggregator;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.gwt.FriendFeedMessage;
import com.threerings.msoy.person.gwt.GroupFeedMessage;
import com.threerings.msoy.person.gwt.SelfFeedMessage;
import com.threerings.msoy.person.gwt.AggregateFeedMessage.Style;
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

    @Test public void testNullFriendMedia ()
    {
        FriendFeedMessage ffm = new FriendFeedMessage();
        ffm.type = FeedMessageType.FRIEND_ADDED_FRIEND;
        ffm.friend = new MemberName("Member", 1);
        ffm.data = new String[]{"Friend", "2"};
        genMessage(ffm);
    }

    @Test public void testNullGroupMedia ()
    {
        GroupFeedMessage gfm = new GroupFeedMessage();
        gfm.type = FeedMessageType.GROUP_ANNOUNCEMENT;
        gfm.data = new String[]{null, "Subject", "1234"};
        genMessage(gfm);
    }

    @Test public void testNullRoomMedia ()
    {
        SelfFeedMessage sfm = new SelfFeedMessage();
        sfm.type = FeedMessageType.SELF_ROOM_COMMENT;
        sfm.actor = new MemberName("Commentor", 1);
        sfm.data = new String[]{"1234", "Room Name"};
        genMessage(sfm);
    }

    @Test public void testNullActionsMedia ()
    {
        FriendFeedMessage msgs[] = {new FriendFeedMessage(), new FriendFeedMessage()};
        msgs[0].type = msgs[1].type = FeedMessageType.FRIEND_LISTED_ITEM;
        msgs[0].friend = msgs[1].friend = new MemberName("Member", 1);
        msgs[0].data = new String[]{"1st Item Name", "1", "1234"};
        msgs[1].data = new String[]{"2nd Item Name", "2", "5678"};
        genAggMessage(Style.ACTIONS, msgs);
    }

    @Test public void testNullActorsMedia ()
    {
        SelfFeedMessage msgs[] = {new SelfFeedMessage(), new SelfFeedMessage()};
        msgs[0].type = msgs[1].type = FeedMessageType.SELF_ROOM_COMMENT;
        msgs[0].actor = new MemberName("Commentor 1", 1);
        msgs[1].actor = new MemberName("Commentor 2", 2);
        msgs[0].data = new String[]{"1234", "Room Name"};
        msgs[1].data = new String[]{"1234", "Room Name"};
        genAggMessage(Style.ACTORS, msgs);
    }

    public void genMessage (FeedMessage msg)
    {
        new StubGenerator().addMessage(msg);
    }

    public void genAggMessage (Style style, FeedMessage ...msgs)
    {
        List<FeedMessage> list = Lists.newArrayList();
        for (FeedMessage msg : msgs) {
            list.add(msg);
        }
        new StubGenerator().addMessage(new AggregateFeedMessage(style, msgs[0].type, 0L, list));
    }

    public static class StubGenerator
    {
        public StubMessages messages;
        public StubBuilder builder;
        public FeedItemGenerator generator;

        public StubGenerator ()
        {
            this(1, true, false);
        }

        public StubGenerator (int memberId, boolean usePronouns, boolean echo)
        {
            builder = new StubBuilder(echo);
            messages = new StubMessages();
            generator = new FeedItemGenerator(1, true, builder, messages);
        }

        public void addMessage (FeedMessage msg)
        {
            generator.addMessage(msg);
        }
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
            assertNotNull(icon);
            echo(icon);
        }

        public void addMedia (Media media, String message) {
            calls.count();
            assertNotNull(media);
            echo(media + ", message: " + message);
        }

        public void addMedia (Media[] media, String message) {
            calls.count();
            assertNotNull(media);
            assertTrue(media.length > 0);
            echo(StringUtil.toString(media) + ", message: " + message);
        }

        public void addText (String text) {
            calls.count();
            assertNotNull(text);
            echo(text);
        }

        public Icon createGainedLevelIcon (String text) {
            calls.count();
            assertNotNull(text);
            return new StubItem(text);
        }

        public String createLink (String label, Pages page, String args) {
            calls.count();
            assertNotNull(label);
            assertNotNull(page);
            assertNotNull(args);
            return simpleToString("link", "label", label, "page", page, "args", args);
        }

        public Media createMedia (MediaDesc md, Pages page, String args) {
            calls.count();
            assertNotNull(md);
            assertNotNull(page);
            assertNotNull(args);
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
