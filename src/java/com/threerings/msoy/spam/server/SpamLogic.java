package com.threerings.msoy.spam.server;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.util.IntSet;
import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.person.gwt.FeedItemGenerator;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Builder;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Icon;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Media;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Messages;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Plural;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageAggregator;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.gwt.MyWhirledData.FeedCategory;
import com.threerings.msoy.person.server.FeedLogic;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.MailSender;
import com.threerings.msoy.server.util.MailSender.Parameters;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.presents.annotation.BlockingThread;

@Singleton @BlockingThread
public class SpamLogic
{
    public boolean sendFeedEmail (int memberId)
    {
        final MemberRecord mrec = _memberRepo.loadMember(memberId);
        IntSet friendIds = _memberRepo.loadFriendIds(mrec.memberId);

        List<FeedCategory> categories = _feedLogic.loadFeedCategories(
            mrec, friendIds, ITEMS_PER_CATEGORY, null);

        List<EmailFeedCategory> ecats = Lists.newArrayList();
        int total = 0;
        for (FeedCategory category : categories) {
            List<EmailFeedItem> eitems = Lists.transform(
                FeedMessageAggregator.aggregate(category.messages, false),
                new Function<FeedMessage, EmailFeedItem>() {;
                    public EmailFeedItem apply (FeedMessage fm) {
                        return new EmailFeedItem(mrec.memberId, fm);
                    }
                });
            if (eitems.isEmpty()) {
                continue;
            }
            EmailFeedCategory ecat = new EmailFeedCategory(
                FeedMessageType.Category.values()[category.category], eitems);
            total += eitems.size();
            ecats.add(ecat);
        }

        if (total == 0) {
            return false;
        }

        Parameters mailParams = new Parameters();
        mailParams.set("feed", ecats);
        _mailSender.sendTemplateEmail(mrec.accountName, ServerConfig.getFromAddress(),
            "feed", mailParams);
        return true;
    }

    protected static class EmailFeedCategory
    {
        public FeedMessageType.Category category;
        public List<EmailFeedItem> items;

        protected EmailFeedCategory (FeedMessageType.Category category, List<EmailFeedItem> items)
        {
            this.category = category;
            this.items = items;
        }
    }

    protected static class EmailFeedItem
    {
        protected EmailFeedItem (int memberId, FeedMessage message)
        {
            _memberId = memberId;
            _message = message;
        }

        public String getTextOnly ()
        {
            if (_content == null) {
                EmailItemBuilder builder = new EmailItemBuilder();
                FeedItemGenerator gen = new FeedItemGenerator(_memberId, true, builder, _messages);
                gen.addMessage(_message);
                _content = builder.buffer.toString();
            }
            return _content;
        }

        public String toString ()
        {
            FeedItemGenerator gen = new FeedItemGenerator(_memberId, true, null, _messages);
            gen.addMessage(_message);
            return null;
        }

        protected int _memberId;
        protected FeedMessage _message;
        protected String _content;
    }

    protected static class StringWrapper
        implements Media, Icon
    {
        public String str;

        public StringWrapper (String str)
        {
            this.str = str;
        }

        public String toString ()
        {
            return str;
        }
    }

    protected static class EmailItemBuilder
        implements Builder
    {
        public StringBuilder buffer = new StringBuilder();

        public void addIcon (Icon icon) {
            buffer.append(icon);
        }

        public void addMedia (Media media, String message) {
            buffer.append(message);
        }

        public void addMedia (Media[] media, String message) {
            buffer.append(message);
        }

        public void addText (String text) {
            buffer.append(text);
        }

        public Icon createGainedLevelIcon (String text) {
            return new StringWrapper(text);
        }

        public String createLink (String label, Pages page, String args) {
            return label;
        }

        public Media createMedia (MediaDesc md, Pages page, String args) {
            return new StringWrapper("");
        }
    }

    protected static final Messages _messages = new Messages () {
        public String action (FeedMessageType type, String subject, String object, Plural plural) {
            if (plural == Plural.SUBJECT) {
                subject = subject + " all";
            }
            String suffix = " thing";
            if (plural == Plural.OBJECT) {
                suffix += "ies";
            }
            return subject + " did " + type + " on " + object + suffix;
        }

        public String andCombine (String list, String item) {
            return list + " and " + item;
        }

        public String badgeName (int code, String levelName) {
            return BadgeType.getType(code).name() + " " + levelName;
        }

        public String briefLevelGain (String subject, String level) {
            return subject + ": " + level;
        }

        public String commaCombine (String list, String item) {
            return list + ", " + item;
        }

        public String describeItem (String typeName, String itemName) {
            return "the " + typeName + " " + itemName;
        }

        public String medal (String medal, String group) {
            return "the " + medal + " medal from " + group;
        }

        public String noGroupForMedal (String medalLink) {
            return "the " + medalLink + " medal";
        }

        public String typeName (String itemType) {
            return Item.getClassForType(Byte.parseByte(itemType)).getSimpleName();
        }

        public String unknownMember () {
            return "Someone";
        }

        public String you () {
            return "You";
        }
    };

    @Inject protected MemberRepository _memberRepo; 
    @Inject protected FeedLogic _feedLogic;
    @Inject protected MailSender _mailSender;

    protected static final int ITEMS_PER_CATEGORY = 50;
}
