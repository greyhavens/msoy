//
// $Id$

package com.threerings.msoy.person.tests;

import java.util.List;

import com.google.common.collect.Lists;

import org.junit.*;

import com.samskivert.util.StringUtil;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.gwt.AggregateFeedMessage.Style;
import com.threerings.msoy.person.gwt.AggregateFeedMessage;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Builder;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Icon;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Media;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Messages;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Plural;
import com.threerings.msoy.person.gwt.FeedItemGenerator;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageAggregator;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.gwt.FriendFeedMessage;
import com.threerings.msoy.person.gwt.GroupFeedMessage;
import com.threerings.msoy.person.gwt.SelfFeedMessage;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import static org.junit.Assert.*;

public class FeedItemGeneratorUnitTest
{
    @Test public void testLevelGainAggregation ()
    {
        long now = System.currentTimeMillis();
        List<FeedMessage> levelGains = Lists.newArrayList();
        for (int ii = 0; ii < 20; ++ii) {
            levelGains.add(newLevelGain(now, ii + 1));
        }
        int maxItems = FeedMessageAggregator.MAX_AGGREGATED_ITEMS;
        List<FeedMessage> aggregated = FeedMessageAggregator.aggregate(levelGains, false);
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
        genMessage(new FriendFeedMessage(FeedMessageType.FRIEND_ADDED_FRIEND,
            new MemberName("Member", 1), new String[]{"Friend", "2"}, 0));
    }

    @Test public void testNullGroupMedia ()
    {
        genMessage(new GroupFeedMessage(FeedMessageType.GROUP_ANNOUNCEMENT, null,
            new String[]{null, "Subject", "1234"}, 0));
    }

    @Test public void testNullRoomMedia ()
    {
        genMessage(new SelfFeedMessage(FeedMessageType.SELF_ROOM_COMMENT,
            new MemberName("Commentor", 1), new String[]{"1234", "Room Name"}, 0));
    }

    @Test public void testNullActionsMedia ()
    {
        FriendFeedMessage msgs[] = {
            new FriendFeedMessage(FeedMessageType.FRIEND_LISTED_ITEM,
                new MemberName("Member", 1), new String[]{"1st Item Name", "1", "1234"}, 0),
            new FriendFeedMessage(FeedMessageType.FRIEND_LISTED_ITEM,
                new MemberName("Member", 1), new String[]{"2nd Item Name", "2", "5678"}, 0)};
        genAggMessage(Style.ACTIONS, msgs);
    }

    @Test public void testNullActorsMedia ()
    {
        SelfFeedMessage msgs[] = {
            new SelfFeedMessage(FeedMessageType.SELF_ROOM_COMMENT,
                new MemberName("Commentor 1", 1), new String[]{"1234", "Room Name"}, 0),
            new SelfFeedMessage(FeedMessageType.SELF_ROOM_COMMENT,
                new MemberName("Commentor 2", 2), new String[]{"1234", "Room Name"}, 0)};
        genAggMessage(Style.ACTORS, msgs);
    }

    @Test public void testPlayedGameDuplicateRemoval ()
    {
        List<FeedMessage> msgs, aggregated;
        msgs = Lists.newArrayList();
        msgs.add(new FriendFeedMessage(FeedMessageType.FRIEND_PLAYED_GAME,
            new MemberName("M1", 1),new String[]{"", "2000", ""}, 0));
        msgs.add(new FriendFeedMessage(FeedMessageType.FRIEND_PLAYED_GAME,
            new MemberName("M1", 1),new String[]{"", "2000", ""}, 0));

        aggregated = FeedMessageAggregator.aggregate(msgs, false);
        assertEquals(1, aggregated.size());
        assertTrue(aggregated.get(0) == msgs.get(0));
        msgs = Lists.newArrayList();
        msgs.add(new FriendFeedMessage(FeedMessageType.FRIEND_PLAYED_GAME,
            new MemberName("M1", 1),new String[]{"", "2000", ""}, 0));
        msgs.add(new FriendFeedMessage(FeedMessageType.FRIEND_PLAYED_GAME,
            new MemberName("M1", 1),new String[]{"", "2000", ""}, 0));
        msgs.add(new FriendFeedMessage(FeedMessageType.FRIEND_PLAYED_GAME,
            new MemberName("M2", 2),new String[]{"", "2000", ""}, 0));

        aggregated = FeedMessageAggregator.aggregate(msgs, false);
        assertEquals(1, aggregated.size());
        assertTrue(aggregated.get(0) instanceof AggregateFeedMessage);
        AggregateFeedMessage aggMsg = (AggregateFeedMessage)aggregated.get(0);
        assertTrue(aggMsg.style == AggregateFeedMessage.Style.ACTORS);
        assertTrue(aggMsg.messages.size() == 2);
        assertTrue(aggMsg.messages.get(0) == msgs.get(0));
        assertTrue(aggMsg.messages.get(1) == msgs.get(2));
    }

    @Test public void testPlayedGameAggregation ()
    {
        List<FeedMessage> msgs = Lists.newArrayList();

        // 2 members play same game
        msgs.add(new FriendFeedMessage(FeedMessageType.FRIEND_PLAYED_GAME,
            new MemberName("M1", 1), new String[]{"", "2000", ""}, 0));
        msgs.add(new FriendFeedMessage(FeedMessageType.FRIEND_PLAYED_GAME,
            new MemberName("M2", 2), new String[]{"", "2000", ""}, 0));

        // same member plays 2 games
        msgs.add(new FriendFeedMessage(FeedMessageType.FRIEND_PLAYED_GAME,
            new MemberName("M3", 3), new String[]{"", "2001", ""}, 0));
        msgs.add(new FriendFeedMessage(FeedMessageType.FRIEND_PLAYED_GAME,
            new MemberName("M3", 3), new String[]{"", "2002", ""}, 0));

        List<FeedMessage> aggregated = FeedMessageAggregator.aggregate(msgs, false);
        assertEquals(2, aggregated.size());
        AggregateFeedMessage aggMsg;

        assertTrue(aggregated.get(0) instanceof AggregateFeedMessage);
        aggMsg = (AggregateFeedMessage)aggregated.get(0);
        assertTrue(aggMsg.style == AggregateFeedMessage.Style.ACTORS);
        assertTrue(aggMsg.messages.size() == 2);
        assertTrue(aggMsg.messages.get(0) == msgs.get(0));
        assertTrue(aggMsg.messages.get(1) == msgs.get(1));

        assertTrue(aggregated.get(1) instanceof AggregateFeedMessage);
        aggMsg = (AggregateFeedMessage)aggregated.get(1);
        assertTrue(aggMsg.style == AggregateFeedMessage.Style.ACTIONS);
        assertTrue(aggMsg.messages.size() == 2);
        assertTrue(aggMsg.messages.get(0) == msgs.get(2));
        assertTrue(aggMsg.messages.get(1) == msgs.get(3));
    }

    @Test public void testAggregateGroupDepletion ()
    {
        List<FeedMessage> msgs = Lists.newArrayList();

        // this data is a canonical version of a live user's feed that was casuing a crash in the
        // message aggregation code (fixed in msoy r17548)

        // TODO: pare down what is actually needed to check "isDepleted" code path
        msgs.add(playedGame(1, 1));
        msgs.add(playedGame(1, 1));
        msgs.add(playedGame(1, 2));
        msgs.add(playedGame(2, 3));
        msgs.add(playedGame(2, 3));
        msgs.add(playedGame(2, 3));
        msgs.add(playedGame(3, 4));
        msgs.add(playedGame(3, 4));
        msgs.add(playedGame(4, 5));
        msgs.add(playedGame(4, 5));
        msgs.add(playedGame(5, 4));
        msgs.add(playedGame(5, 4));
        msgs.add(playedGame(4, 4));
        msgs.add(playedGame(3, 6));
        msgs.add(playedGame(6, 3));
        msgs.add(playedGame(6, 7));
        msgs.add(playedGame(7, 8));
        msgs.add(playedGame(6, 1));
        msgs.add(playedGame(8, 3));
        msgs.add(playedGame(9, 9));
        msgs.add(playedGame(9, 1));
        msgs.add(playedGame(3, 1));
        msgs.add(playedGame(3, 1));
        msgs.add(playedGame(10, 5));
        msgs.add(playedGame(11, 10));
        msgs.add(playedGame(10, 11));
        msgs.add(playedGame(12, 5));
        msgs.add(playedGame(12, 5));
        msgs.add(playedGame(12, 5));
        msgs.add(playedGame(10, 9));
        msgs.add(playedGame(13, 3));
        msgs.add(playedGame(13, 3));
        msgs.add(playedGame(13, 3));
        msgs.add(playedGame(4, 12));
        msgs.add(playedGame(4, 12));
        msgs.add(playedGame(14, 5));
        msgs.add(playedGame(14, 5));
        msgs.add(playedGame(14, 5));
        msgs.add(playedGame(3, 5));
        msgs.add(playedGame(3, 5));
        msgs.add(playedGame(3, 8));
        msgs.add(playedGame(4, 13));
        msgs.add(playedGame(1, 5));
        msgs.add(playedGame(1, 5));
        msgs.add(playedGame(1, 8));
        msgs.add(playedGame(15, 5));
        msgs.add(playedGame(15, 3));
        msgs.add(playedGame(15, 3));
        msgs.add(playedGame(16, 14));
        msgs.add(playedGame(4, 15));
        msgs.add(playedGame(4, 15));
        msgs.add(playedGame(11, 16));
        msgs.add(playedGame(11, 16));
        msgs.add(playedGame(11, 16));
        msgs.add(playedGame(4, 15));
        msgs.add(playedGame(15, 5));
        msgs.add(playedGame(17, 2));
        msgs.add(playedGame(3, 5));
        msgs.add(playedGame(3, 5));
        msgs.add(playedGame(3, 5));
        msgs.add(playedGame(15, 5));
        msgs.add(playedGame(15, 5));
        msgs.add(playedGame(4, 15));
        msgs.add(playedGame(4, 15));
        msgs.add(playedGame(4, 16));
        msgs.add(playedGame(15, 3));
        msgs.add(playedGame(7, 17));
        msgs.add(playedGame(18, 18));
        msgs.add(playedGame(18, 18));
        msgs.add(playedGame(18, 18));
        msgs.add(playedGame(4, 15));
        msgs.add(playedGame(4, 15));
        msgs.add(playedGame(4, 18));
        msgs.add(playedGame(15, 3));
        msgs.add(playedGame(15, 14));
        msgs.add(playedGame(15, 19));
        msgs.add(playedGame(10, 17));
        msgs.add(playedGame(10, 17));
        msgs.add(playedGame(10, 17));
        msgs.add(playedGame(11, 16));
        msgs.add(playedGame(11, 16));
        msgs.add(playedGame(11, 16));
        msgs.add(playedGame(4, 5));
        msgs.add(playedGame(4, 5));
        msgs.add(playedGame(15, 20));
        msgs.add(playedGame(15, 20));
        msgs.add(playedGame(13, 18));
        msgs.add(playedGame(13, 18));
        msgs.add(playedGame(13, 8));
        msgs.add(playedGame(1, 1));
        msgs.add(playedGame(1, 1));
        msgs.add(playedGame(1, 21));
        msgs.add(playedGame(7, 17));
        msgs.add(playedGame(7, 9));
        msgs.add(playedGame(7, 9));
        msgs.add(playedGame(3, 9));
        msgs.add(playedGame(18, 22));
        msgs.add(playedGame(4, 23));
        msgs.add(playedGame(3, 17));
        msgs.add(playedGame(3, 17));
        msgs.add(playedGame(15, 14));
        msgs.add(playedGame(15, 14));
        msgs.add(playedGame(15, 14));
        msgs.add(playedGame(13, 24));
        msgs.add(playedGame(13, 25));
        msgs.add(playedGame(13, 25));
        msgs.add(playedGame(11, 16));
        msgs.add(playedGame(11, 16));
        msgs.add(playedGame(11, 16));
        msgs.add(playedGame(4, 26));
        msgs.add(playedGame(4, 26));
        msgs.add(playedGame(1, 27));
        msgs.add(playedGame(1, 27));
        msgs.add(playedGame(1, 27));
        msgs.add(playedGame(2, 19));
        msgs.add(playedGame(2, 19));
        msgs.add(playedGame(2, 19));
        msgs.add(playedGame(19, 17));
        msgs.add(playedGame(19, 17));
        msgs.add(playedGame(19, 17));
        msgs.add(playedGame(7, 15));
        msgs.add(playedGame(7, 15));
        msgs.add(playedGame(7, 7));
        msgs.add(playedGame(3, 17));
        msgs.add(playedGame(3, 17));
        msgs.add(playedGame(3, 17));
        msgs.add(playedGame(20, 27));
        msgs.add(playedGame(4, 28));
        msgs.add(playedGame(4, 28));
        msgs.add(playedGame(4, 28));
        msgs.add(playedGame(15, 19));
        msgs.add(playedGame(15, 19));
        msgs.add(playedGame(15, 19));
        msgs.add(playedGame(14, 19));
        msgs.add(playedGame(14, 19));
        msgs.add(playedGame(14, 19));
        msgs.add(playedGame(13, 19));
        msgs.add(playedGame(13, 19));
        msgs.add(playedGame(13, 19));
        msgs.add(playedGame(3, 19));
        msgs.add(playedGame(3, 19));
        msgs.add(playedGame(3, 19));
        msgs.add(playedGame(4, 5));
        msgs.add(playedGame(4, 5));
        msgs.add(playedGame(4, 5));
        msgs.add(playedGame(7, 20));
        msgs.add(playedGame(4, 19));
        msgs.add(playedGame(4, 19));
        msgs.add(playedGame(4, 19));
        msgs.add(playedGame(7, 29));
        msgs.add(playedGame(13, 19));
        msgs.add(playedGame(13, 19));
        msgs.add(playedGame(13, 19));
        msgs.add(playedGame(16, 14));
        msgs.add(playedGame(16, 14));
        msgs.add(playedGame(16, 14));
        msgs.add(playedGame(18, 3));
        msgs.add(playedGame(10, 19));
        msgs.add(playedGame(13, 19));
        msgs.add(playedGame(13, 19));
        msgs.add(playedGame(13, 19));

        // the test is that this aggregate does not throw an exception
        FeedMessageAggregator.aggregate(msgs, false);
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

    public FriendFeedMessage playedGame (int actorId, int gameId)
    {
        return new FriendFeedMessage(FeedMessageType.FRIEND_PLAYED_GAME,
                    new MemberName("M" + actorId, actorId), new String[]{"", "" + gameId, ""}, 0);
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

        public Icon createSubscribedIcon (String text) {
            calls.count();
            assertNotNull(text);
            return new StubItem(text);
        }

        public Icon createCommentedIcon (String text) {
            calls.count();
            assertNotNull(text);
            return new StubItem(text);
        }

        public Icon createPokedIcon (String text) {
            calls.count();
            assertNotNull(text);
            return new StubItem(text);
        }

        public Icon createFoundedGroupIcon (String text) {
            calls.count();
            assertNotNull(text);
            return new StubItem(text);
        }

        public Icon createListedItemIcon (String text) {
            calls.count();
            assertNotNull(text);
            return new StubItem(text);
        }

        public Icon createUpdatedRoomIcon (String text) {
            calls.count();
            assertNotNull(text);
            return new StubItem(text);
        }

        public Icon createLikedMusicIcon (String text) {
            calls.count();
            assertNotNull(text);
            return new StubItem(text);
        }

        public String createLink (String label, Pages page, Args args) {
            calls.count();
            assertNotNull(label);
            assertNotNull(page);
            assertNotNull(args);
            return simpleToString("link", "label", label, "page", page, "args", args);
        }

        public Media createMedia (MediaDesc md, Pages page, Args args) {
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

        public String action (FeedMessage message, String subject, String object, Plural plural) {
            calls.count();
            return simpleToString("action", "subject", subject, "type", message.type, "object", object,
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
        public StubItem (MediaDesc md, Pages page, Args args)
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
        return new FriendFeedMessage(FeedMessageType.FRIEND_GAINED_LEVEL,
            new MemberName("Test Friend " + memberId, memberId), new String[] {"10"}, posted);
    }

}

