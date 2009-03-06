//
// $Id$

package com.threerings.msoy.spam.server;

import java.sql.Date;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.net.MailUtil;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.CountHashMap;
import com.samskivert.util.IntSet;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberMailUtil;

import com.threerings.msoy.person.gwt.FeedItemGenerator;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Builder;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Icon;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Media;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Messages;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Plural;
import com.threerings.msoy.person.gwt.FeedMessageType.Category;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageAggregator;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.gwt.MyWhirledData.FeedCategory;
import com.threerings.msoy.person.server.FeedLogic;

import com.threerings.msoy.server.CronLogic;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.BatchInvoker;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.MailSender;
import com.threerings.msoy.server.util.MailSender.Parameters;

import com.threerings.msoy.spam.server.persist.SpamRecord;
import com.threerings.msoy.spam.server.persist.SpamRepository;

import com.threerings.msoy.web.gwt.MarkupBuilder;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SharedMediaUtil;
import com.threerings.msoy.web.gwt.SharedMediaUtil.Dimensions;

import static com.threerings.msoy.Log.log;

/**
 * Handles activities relating to sending users unsolicited email. On the less cynical side it
 * could be called MarketingLogic.
 */
@Singleton @BlockingThread
public class SpamLogic
{
    /**
     * News feed category for the purposes of filling in a velocity template. NOTE: this class must
     * be public so velocity can reflect its methods and members.
     */
    public class EmailFeedCategory
        implements Comparable<EmailFeedCategory>
    {
        /** The category. */
        public Category category;

        /** The items. */
        public List<EmailFeedItem> items;

        /**
         * Gets the name of this feed category. NOTE: this is called using reflection by velocity.
         */
        public String getCategoryName ()
        {
            return _dmsgs.get("feedCategory" + category.ordinal());
        }

        // from Comparable
        public int compareTo (EmailFeedCategory o)
        {
            int thisIdx = ArrayUtil.indexOf(CATEGORIES, category);
            int thatIdx = ArrayUtil.indexOf(CATEGORIES, o.category);
            return (thisIdx == -1 ? 100 : thisIdx) - (thatIdx == -1 ? 100 : thatIdx);
        }

        protected EmailFeedCategory (Category category, List<EmailFeedItem> items)
        {
            this.category = category;
            this.items = items;
        }
    }

    /**
     * News feed item for the purposes of filling in a velocity template. NOTE: this class must be
     * public so velocity can reflect its methods and members.
     */
    public class EmailFeedItem
    {
        /**
         * Gets the plain text representation of this feed item. NOTE: this is called using
         * reflection by velocity.
         */
        public String getPlainText ()
        {
            initContent();
            return _content;
        }

        /**
         * Gets the html text representation of this feed item. NOTE: this is called using
         * reflection by velocity.
         */
        public String getHTMLText ()
        {
            initContent();
            return _htmlContent;
        }

        protected EmailFeedItem (Generator generators[], int memberId, FeedMessage message)
        {
            _generators = generators;
            _memberId = memberId;
            _message = message;
        }

        protected void initContent ()
        {
            if (_content == null) {
                _generators[0].addMessage(_message);
                _generators[1].addMessage(_message);
                _content = builder(0).item;
                _htmlContent = builder(1).item;
            }
        }

        protected EmailItemBuilder builder (int ii)
        {
            return ((EmailItemBuilder)_generators[ii].getBuilder());
        }

        protected Generator _generators[];
        protected int _memberId;
        protected FeedMessage _message;
        protected String _content;
        protected String _htmlContent;
    }

    /**
     * Creates new spam logic.
     */
    @Inject public SpamLogic (ServerMessages messages)
    {
        _pmsgs = messages.getBundle("feed.PersonMessages");
        _dmsgs = messages.getBundle("feed.DynamicMessages");
    }

    /**
     * Initializes our spam jobs.
     */
    public void init ()
    {
        Runnable job = new Runnable () {
            public void run () {
                sendFeedEmails();
            }

            public String toString () {
                return "News feed emailer";
            }
        };

        // TODO: dev is not special, I just want to check this on our old alpha DB snapshot
        if (DeploymentConfig.devDeployment) {
            _batchInvoker.postRunnable(job);

        } else {
            // Run nightly around 1am
            _cronLogic.scheduleAt(1, job);
        }
    }

    /**
     * Loads up all candidate users for getting their feeds mailed to them, does various bits
     * of pruning and sends emails to a random subset of qualifying users.
     */
    public void sendFeedEmails ()
    {
        log.info("Starting feed mailing");
        long now = System.currentTimeMillis();

        // find everyone who is lapsed, shuffled
        List<Integer> lapsedIds = findRetentionCandidates(new Date(now - LAPSED_CUTOFF));
        log.info("Found lapsed members", "size", lapsedIds.size());

        // do the sending and record results
        int totalSent = 0;
        Date secondEmailCutoff = new Date(now - SECOND_EMAIL_CUTOFF);
        CountHashMap<Result> stats = new CountHashMap<Result>();
        for (Integer memberId : lapsedIds) {
            Result result = sendFeedEmail(memberId, secondEmailCutoff);
            if (DeploymentConfig.devDeployment) {
                log.info("Feed email result (not sent)", "member", memberId, "result", result);
            }
            stats.incrementCount(result, 1);
            if (result.success && ++totalSent >= SEND_LIMIT) {
                break;
            }
        }

        // log results, we could use keys here but seeing e.g. OTHER = 0 might be comforting
        List<Object> statLog = Lists.newArrayList();
        for (Result r : Result.values()) {
            statLog.add(r);
            statLog.add(stats.getCount(r));
        }
        log.info("Finished feed mailing", statLog);
    }

    /**
     * For testing the feed email content, just sends a message to the given member id with no
     * checking.
     * TODO: remove
     */
    public boolean testFeedEmail (int memberId)
    {
        Result result = sendFeedEmail(_memberRepo.loadMember(memberId), Result.SENT_LAPSED, false);
        log.info("Sent test feed email", "result", result);
        return result.success;
    }

    /**
     * Returns a writable shuffled list of member ids whose last login meets the criteria for a
     * retention message.
     */
    protected List<Integer> findRetentionCandidates (Date cutoff)
    {
        List<Integer> lapsedIds = Lists.newArrayList(
            _memberRepo.findRetentionCandidates(new Date(0), cutoff));
        Collections.shuffle(lapsedIds);
        return lapsedIds;
    }

    /**
     * Checks all relevant spam history and tries to send the feed to the given member id. The
     * cutoff date is passed in for consistency since the feed mailer job could take a long time to
     * run.
     */
    protected Result sendFeedEmail (int memberId, Date secondEmailCutoff)
    {
        Result result = Result.OTHER;
        try {
            result = trySendFeedEmail(memberId, secondEmailCutoff);

        } catch (Exception e) {
            log.warning("Failed to send feed", "memberId", e);
        }
        return result;
    }

    /**
     * Non-exception-aware version of the above.
     */
    protected Result trySendFeedEmail (int memberId, Date secondEmailCutoff)
    {
        SpamRecord spamRec = _spamRepo.loadSpamRecord(memberId);
        Date last = spamRec == null ? null : spamRec.lastRetentionEmailSent;

        if (last != null && last.after(secondEmailCutoff)) {
            // spammed recently, skip
            return Result.TOO_RECENTLY_SPAMMED;
        }

        // load the member
        MemberRecord mrec = _memberRepo.loadMember(memberId);
        if (mrec == null) {
            log.warning("Member deleted during feed mailing?", "memberId", memberId);
            return Result.MEMBER_DELETED;
        }

        // skip placeholder addresses
        if (MemberMailUtil.isPlaceholderAddress(mrec.accountName)) {
            return Result.PLACEHOLDER_ADDRESS;
        }

        // skip invalid addresses
        if (!MailUtil.isValidAddress(mrec.accountName)) {
            return Result.INVALID_ADDRESS;
        }

        // oh look, they've logged in! maybe the email(s) worked. clear counter
        boolean persuaded = last != null && mrec.lastSession.after(last);
        if (persuaded) {
            spamRec.retentionEmailCountSinceLastLogin = 0;
            // fall through, we'll send a mail and save the record below

        } else if (spamRec != null && spamRec.retentionEmailCountSinceLastLogin >= 2) {
            // they are never coming back... oh well, there are plenty of other fish in the sea
            return Result.LOST_CAUSE;
        }

        // sending the email could take a while so update the spam record here to reduce window
        // where other peers may conflict with us. NOTE: we do plan to run this job on multiple
        // servers some day
        _spamRepo.noteRetentionEmailSending(memberId, spamRec);

        // choose a successful result based on previous attempts
        Result result = Result.SENT_DORMANT;
        if (persuaded) {
            result = Result.SENT_PERSUADED;
        } else if (spamRec == null || spamRec.retentionEmailCount == 0) {
            result = Result.SENT_LAPSED;
        }

        // now send the email, overwriting result in case of not enough friends etc
        // TODO: algorithm for resurrecting these failures (e.g. if they failed due to no feed
        // activity, maybe their friends have since been persuaded and created some activity)
        result = sendFeedEmail(mrec, result, true);

        // NOTE: this is sort of redundant but increases the integrity of the spam record and
        // reduces chance of a user getting two emails when we are 1M strong
        _spamRepo.noteRetentionEmailResult(memberId, result.code);

        // log an event for successes. the result is the lapse status
        if (result.success) {
            _eventLog.retentionMailSent(mrec.memberId, mrec.visitorId, result.name());
        }

        return result;
    }

    /**
     * Does the heavy lifting of sending a feed email. If successful, returns the given result,
     * otherwise returns an unsuccessful result.
     * @param realDeal testing flag; if false, we make sure the mail is sent regardless
     */
    protected Result sendFeedEmail (
        final MemberRecord mrec, Result successResult, boolean realDeal)
    {
        int memberId = mrec.memberId;

        // load up friends, bail if not enough
        IntSet friendIds = _memberRepo.loadFriendIds(mrec.memberId);
        if (realDeal && friendIds.size() < MIN_FRIEND_COUNT) {
            return Result.NOT_ENOUGH_FRIENDS;
        }

        // load the feed by categories
        List<FeedCategory> categories = _feedLogic.loadFeedCategories(
            mrec, friendIds, ITEMS_PER_CATEGORY, null);

        // count up all messages
        int count = 0;
        for (FeedCategory cat : categories) {
            count += cat.messages.length;
        }
        if (realDeal && count < MIN_ITEM_COUNT) {
            return Result.NOT_ENOUGH_NEWS;
        }

        // prepare the generators!
        final Generator generators[] = {
            new Generator(memberId, new PlainTextBuilder(), _messages),
            new Generator(memberId, new HTMLBuilder(), _messages)};

        // convert to our wrapped categories and items
        List<EmailFeedCategory> ecats = Lists.newArrayList();
        for (FeedCategory category : categories) {
            List<EmailFeedItem> eitems = Lists.transform(
                FeedMessageAggregator.aggregate(category.messages, false),
                new Function<FeedMessage, EmailFeedItem>() {;
                    public EmailFeedItem apply (FeedMessage fm) {
                        return new EmailFeedItem(generators, mrec.memberId, fm);
                    }
                });
            if (eitems.isEmpty()) {
                continue;
            }
            EmailFeedCategory ecat = new EmailFeedCategory(
                Category.values()[category.category], eitems);
            ecats.add(ecat);
        }

        // Sort to CATEGORIES order
        Collections.sort(ecats);

        // don't send email to alpha registrants
        if (realDeal && DeploymentConfig.devDeployment) {
            return successResult;
        }

        // fire off the email, the template will take care of looping over categories and items
        // TODO: it would be great if we could somehow get the final result of actually sending the
        // mail. A lot of users have emails like 123@myass.com and we are currently counting them
        // as sent. I also like my pie at 30,000 feet please.
        Parameters params = new Parameters();
        params.set("feed", ecats);
        params.set("server_url", DeploymentConfig.serverURL);
        params.set("name", mrec.name);
        params.set("member_id", mrec.memberId);
        _mailSender.sendTemplateEmail(realDeal ? MailSender.By.COMPUTER : MailSender.By.HUMAN,
            mrec.accountName, ServerConfig.getFromAddress(), MAIL_TEMPLATE, params);
        return successResult;
    }

    /**
     * Implements the generator components, just wraps a string.
     */
    protected static class StringWrapper
        implements Media, Icon
    {
        /** The text of the thing we wrap. */
        public String text;

        /**
         * Creates a new wrapper.
         */
        public StringWrapper (String text)
        {
            this.text = text;
        }

        // from Object
        public String toString ()
        {
            return text;
        }
    }

    /**
     * Base item builder for email that just records the text of the added feed item.
     */
    protected static abstract class EmailItemBuilder
        implements Builder
    {
        /**
         * The final item text just added (we only keep it long enough for the feed item to grab it.
         */
        public String item;

        // from Builder
        public void addIcon (Icon icon) {
            item = icon.toString();
        }

        // from Builder
        public void addMedia (Media media, String message) {
            _temp.setLength(0);
            _temp.append(media).append(message);
            item = _temp.toString();
        }

        // from Builder
        public void addMedia (Media[] medias, String message) {
            _temp.setLength(0);
            for (Media media : medias) {
                _temp.append(media);
            }
            _temp.append(message);
            item = _temp.toString();
        }

        // from Builder
        public void addText (String text) {
            item = text;
        }

        StringBuilder _temp = new StringBuilder();
    }

    /**
     * Implementation of an feed item builder that keeps a string buffer of the item so far.
     */
    protected static class HTMLBuilder extends EmailItemBuilder
    {
        // from Builder
        public Icon createGainedLevelIcon (String text) {
            return new StringWrapper(_html.reset().open("img",
                "src", "images/whirled/friend_gained_level.png",
                "style", imgStyle(new Dimensions("30px", "20px"))).close().append(text).finish());
        }

        // from Builder
        public String createLink (String label, Pages page, String args) {
            return _html.reset().open("a", "href", link(page, args),"style", A_STYLE).append(label)
                .finish();
        }

        // from Builder
        public Media createMedia (MediaDesc md, Pages page, String args) {
            // start with the anchor
            _html.reset().open("a", "href", link(page, args), "style", A_STYLE);

            if (!md.isImage()) {
                // don't bother with other media types, just use some fakey text
                // TODO: should we worry about this? I don't think I've ever seen any non-image
                // media in my feed before...
                return new StringWrapper(_html.append("[X]").finish());
            }
            int size = MediaDesc.HALF_THUMBNAIL_SIZE;
            if (page == Pages.WORLD && args.startsWith("s")) {
                // snapshots are unconstrained at a set size; fake a width constraint for
                // TINY_SIZE.
                md.constraint = MediaDesc.HORIZONTALLY_CONSTRAINED;
                size = MediaDesc.SNAPSHOT_TINY_SIZE;
            }
            int width = MediaDesc.getWidth(size);
            int height = MediaDesc.getHeight(size);
            Dimensions dim = SharedMediaUtil.resolveImageSize(md, width, height);
            if (dim == null) {
                dim = new Dimensions(width + "px", height + "px");
            }
            return new StringWrapper(_html.open("img", "src", md.getMediaPath(),
                "style", imgStyle(dim)).finish());
        }

        protected static String link (Pages page, String args)
        {
            return Pages.makeLink(page, args).substring(1);
        }

        protected static String imgStyle (Dimensions dim)
        {
            return IMG_STYLE + " width: " + dim.width + "; height: " + dim.height + ";";
        }

        /** The buffer we append to for this item's html. */
        protected MarkupBuilder _html = new MarkupBuilder();
    }

    protected static class PlainTextBuilder extends EmailItemBuilder
    {
        // from Builder
        public Icon createGainedLevelIcon (String text) {
            return new StringWrapper(text);
        }

        // from Builder
        public String createLink (String label, Pages page, String args) {
            return label;
        }

        // from Builder
        public Media createMedia (MediaDesc md, Pages page, String args) {
            return new StringWrapper("");
        }
    }

    protected class Generator extends FeedItemGenerator
    {
        public Generator (int memberId, Builder builder, Messages messages)
        {
            super(memberId, true, builder, messages);
        }

        /**
         * Returns the builder we are using to generate the feed item.
         */
        public Builder getBuilder ()
        {
            return _builder;
        }
    }

    /**
     * The result of attempting to send a feed email.
     */
    protected enum Result
    {
        // successful results
        SENT_LAPSED(1, true), SENT_PERSUADED(2, true), SENT_DORMANT(3, true),

        // failed results
        TOO_RECENTLY_SPAMMED(4), NOT_ENOUGH_FRIENDS(5), NOT_ENOUGH_NEWS(6), MEMBER_DELETED(7),
        PLACEHOLDER_ADDRESS(8), INVALID_ADDRESS(9), LOST_CAUSE(10), OTHER(11);

        /** Persisted value for results. */
        public int code;

        /** Whether the results indicates a successfully sent message. */
        public boolean success;

        Result (int code)
        {
            this(code, false);
        }

        Result (int code, boolean success)
        {
            this.code = code;
            this.success = success;
        }
    }

    /** Messages instance that delegates to the bundles in our parent class. */
    protected Messages _messages = new Messages () {
        // from Messages
        public String typeName (String itemType) {
            return _dmsgs.get("itemType" + itemType);
        }

        // from Messages
        public String you () {
            return _pmsgs.get("feedProfileMemberYou");
        }

        // from Messages
        public String describeItem (String typeName, String itemName) {
            return _pmsgs.get("descCombine", typeName, itemName);
        }

        // from Messages
        public String badgeName (int code, String levelName) {
            String hexCode = Integer.toHexString(code);
            return _dmsgs.get("badge_" + hexCode, levelName);
        }

        // from Messages
        public String noGroupForMedal (String medalLink) {
            return _pmsgs.get("medalNoGroup", medalLink);
        }

        // from Messages
        public String medal (String medal, String group) {
            return _pmsgs.get("medal", medal, group);
        }

        // from Messages
        public String unknownMember () {
            return _pmsgs.get("feedProfileMemberUnknown");
        }

        // from Messages
        public String action (
            FeedMessageType type, String subject, String object, Plural plural) {
            switch (type) {
            case GLOBAL_ANNOUNCEMENT:
                return _pmsgs.get("globalAnnouncement", object);

            case FRIEND_ADDED_FRIEND:
                return _pmsgs.get("friendAddedFriend", subject, object);

            case FRIEND_UPDATED_ROOM:
                switch (plural) {
                case NONE:
                    return _pmsgs.get("friendUpdatedRoom", subject, object);
                case SUBJECT:
                    return _pmsgs.get("friendsUpdatedRoom", subject);
                case OBJECT:
                    return _pmsgs.get("friendUpdatedRooms", subject, object);
                }
                break;

            case FRIEND_WON_TROPHY:
                return plural == Plural.OBJECT ?
                    _pmsgs.get("friendWonTrophies", subject, object) :
                    _pmsgs.get("friendWonTrophy", subject, object);

            case FRIEND_LISTED_ITEM:
                return _pmsgs.get("friendListedItem", subject, object);

            case FRIEND_GAINED_LEVEL:
                return plural == Plural.SUBJECT ?
                    _pmsgs.get("friendsGainedLevel", subject) :
                    _pmsgs.get("friendGainedLevel", subject, object);

            case FRIEND_WON_BADGE:
                return plural == Plural.OBJECT ?
                    _pmsgs.get("friendWonBadges", subject, object) :
                    _pmsgs.get("friendWonBadge", subject, object);

            case FRIEND_WON_MEDAL:
                return _pmsgs.get("friendWonMedal", subject, object);

            case GROUP_ANNOUNCEMENT:
                return _pmsgs.get("groupAnnouncement", subject, object);

            case GROUP_UPDATED_ROOM:
                return _pmsgs.get("friendUpdatedRoom", subject, object);

            case SELF_ROOM_COMMENT:
                return _pmsgs.get("selfRoomComment", subject, object);

            case SELF_ITEM_COMMENT:
                return _pmsgs.get("selfItemComment", subject, object);

            case SELF_FORUM_REPLY:
                return _pmsgs.get("selfForumReply", subject, object);
            }
            return "Unknown message type: " + subject + " did something to " + object + ".";
        }

        // from Messages
        public String andCombine (String list, String item) {
            return _pmsgs.get("andCombine", list, item);
        }

        // from Messages
        public String briefLevelGain (String subject, String level) {
            return _pmsgs.get("colonCombine", subject, level);
        }

        // from Messages
        public String commaCombine (String list, String item) {
            return _pmsgs.get("commaCombine", list, item);
        }
    };

    // Message bundles
    // TODO: proper localization
    protected MessageBundle _pmsgs;
    protected MessageBundle _dmsgs;

    @Inject protected MemberRepository _memberRepo; 
    @Inject protected FeedLogic _feedLogic;
    @Inject protected MailSender _mailSender;
    @Inject protected SpamRepository _spamRepo;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected CronLogic _cronLogic;
    @Inject protected @BatchInvoker Invoker _batchInvoker;

    protected static final int ITEMS_PER_CATEGORY = 50;
    protected static final String MAIL_TEMPLATE = "feed";
    protected static final int LAPSED_CUTOFF = 3 * 24*60*60*1000;
    protected static final int SECOND_EMAIL_CUTOFF = 7 * 24*60*60*1000;
    protected static final int MIN_FRIEND_COUNT = 1;
    protected static final int MIN_ITEM_COUNT = 5;
    protected static final int SEND_LIMIT = DeploymentConfig.devDeployment ? 100 : 1000;

    protected static final String IMG_STYLE = "border: 0px; padding: 2px; margin: 2px; " +
        "vertical-align: middle;";
    protected static final String A_STYLE = "text-decoration: none;";

    /** We want these categories first. */
    protected static final Category[] CATEGORIES = {Category.ANNOUNCEMENTS, Category.LISTED_ITEMS, 
        Category.ROOMS, Category.TROPHIES, Category.FRIENDINGS};
}
