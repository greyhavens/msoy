//
// $Id$

package com.threerings.msoy.spam.server;

import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.net.MailUtil;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.Calendars;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.CountHashMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import com.threerings.util.MessageBundle;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;

import com.threerings.cron.server.CronLogic;
import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.fora.server.ForumLogic;
import com.threerings.msoy.group.server.GroupLogic;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.item.server.CatalogLogic;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.FavoritesRepository;
import com.threerings.msoy.item.server.persist.LauncherRecord;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Builder;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Icon;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Media;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Messages;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Plural;
import com.threerings.msoy.person.gwt.FeedItemGenerator;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageAggregator;
import com.threerings.msoy.person.gwt.FeedMessageType.Category;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.gwt.MyWhirledData.FeedCategory;
import com.threerings.msoy.person.server.FeedLogic;
import com.threerings.msoy.server.MediaDescFactory;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.BatchInvoker;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.MailSender.Parameters;
import com.threerings.msoy.server.util.MailSender;
import com.threerings.msoy.spam.server.persist.SpamRecord;
import com.threerings.msoy.spam.server.persist.SpamRepository;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.MarkupBuilder;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SharedMediaUtil.Dimensions;
import com.threerings.msoy.web.gwt.SharedMediaUtil;

import static com.threerings.msoy.Log.log;

/**
 * Handles activities relating to sending users unsolicited email. On the less cynical side it
 * could be called MarketingLogic.
 */
@Singleton @BlockingThread
public class SpamLogic
{
    /**
     * Retention bucket a user can get assigned to.
     */
    public enum Bucket
    {
        /** User has some friend or group activity. */
        HAS_PERSONAL_EVENTS("activeFriends", "nameBusyFriends", "friendFeedAndNewThings"),

        /** Has no friend or group activity, but has at least 1 friend. */
        HAS_INACTIVE_FRIENDS("inactiveFriends", "nameNewThings", "newsFeedAndNewThings"),

        /** Has no friends and hence no personal events. */
        HAS_NO_FRIENDS("noFriends", "nameNewThings", "newsFeedAndNewThings");

        /** Name of the bucket. Used for logging and presenting results. */
        public final String name;

        /** Choices of subject line for retention mailings. NOTE: the values here are for logging;
         * they are translated to full subject lines by the velocity template feed.tmpl. */
        public final String[] subjectLines;

        Bucket (String name, String... subjectLines)
        {
            this.name = name;
            this.subjectLines = subjectLines;
        }
    }

    /**
     * News feed category for the purposes of filling in a velocity template. NOTE: all public
     * methods and members are referenced from the <code>feed.tmpl</code> template using
     * reflection.
     */
    public class EmailFeedCategory
        implements Comparable<EmailFeedCategory>
    {
        /** The category. */
        public Category category;

        /** The items. */
        public List<EmailFeedItem> items;

        /**
         * Gets the name of this feed category.
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
     * News feed item for the purposes of filling in a velocity template. NOTE: all public methods
     * and members are referenced from the <code>feed.tmpl</code> template using reflection.
     */
    public class EmailFeedItem
    {
        /**
         * Gets the plain text representation of this feed item.
         */
        public String getPlainText ()
        {
            initContent();
            return _content;
        }

        /**
         * Gets the html text representation of this feed item.
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
     * Wrapper for a forum thread for use by velocity. NOTE: all public methods and members are
     * referenced from the <code>feed.tmpl</code> template using reflection.
     */
    public class EmailForumThread
    {
        /**
         * Gets the subject of the thread.
         */
        public String getSubject ()
        {
            return _thread.subject;
        }

        /**
         * Gets the URL leading to the most recent post in the thread.
         */
        public String getMostRecentURL ()
        {
            return Pages.GROUPS.makeURL(_thread.getMostRecentPostArgs());
        }

        /**
         * Gets the URL leading to the first unread post in the thread.
         */
        public String getFirstUnreadURL ()
        {
            return Pages.GROUPS.makeURL(_thread.getFirstUnreadPostArgs());
        }

        /**
         * Gets the URL leading to the thread's group.
         */
        public String getGroupURL ()
        {
            // TODO: remove check when group deletion is fixed
            if (_thread.group == null) {
                return Pages.GROUPS.makeURL();
            }
            return Pages.GROUPS.makeURL(String.valueOf(_thread.group.getGroupId()));
        }

        /**
         * Gets the name the thread's group.
         */
        public String getGroupName ()
        {
            // TODO: remove check when group deletion is fixed
            if (_thread.group == null) {
                log.warning("Null group in retention mailing", "threadId", _thread.threadId);
                return "?";
            }
            return _thread.group.toString();
        }

        protected EmailForumThread (ForumThread thread)
        {
            _thread = thread;
        }

        protected ForumThread _thread;
    }

    /**
     * Item listing for the purposes of filling in a velocity template. NOTE: all public methods
     * and members are referenced from the <code>feed.tmpl</code> template using reflection.
     */
    public class EmailListing
    {
        /**
         * Returns the game id associated with this listing, if any.
         */
        public int getGameId()
        {
            return _gameId;
        }

        /**
         * Returns the url of this listing's thumbnail image.
         */
        public String getThumbnail ()
        {
            return _listing.thumbMedia.getMediaPath();
        }

        /**
         * Returns the name of this listing.
         */
        public String getName ()
        {
            return _listing.name;
        }

        /**
         * Returns the catalog id of this listing.
         */
        public int getCatalogId ()
        {
            return _listing.catalogId;
        }

        protected EmailListing (ListingCard listing)
        {
            _listing = listing;
            if (listing.itemType == MsoyItemType.LAUNCHER) {
                try{
                    _gameId = ((LauncherRecord)_itemLogic.requireListing(
                        MsoyItemType.LAUNCHER, listing.catalogId, true).item).gameId;
                } catch (ServiceException e) {
                    log.warning("Could not load game id for new & hot game",
                                "catalogId", listing.catalogId);
                }
            }
        }

        protected ListingCard _listing;
        protected int _gameId;
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
        // run nightly at 1am
        _cronLogic.scheduleAt(1, "SpamLogic retention mailings", new Runnable() {
            public void run () {
                sendRetentionEmails();
            }
            public String toString () {
                return "News feed emailer";
            }
        });
    }

    /**
     * Loads up all candidate users for getting a retention email, does various bits of pruning and
     * sends emails to a random subset of qualifying users.
     */
    public void sendRetentionEmails ()
    {
        log.info("Starting retention mailing");
        long now = System.currentTimeMillis();

        // find everyone who is lapsed, shuffled
        List<Integer> lapsedIds = findRetentionCandidates(new Date(now - LAPSED_CUTOFF));
        log.info("Found lapsed members", "size", lapsedIds.size());

        // load the filler, data from this is included in all mailings
        NewStuff filler = loadFiller();

        // do the sending and record results
        int totalSent = 0;
        Date secondEmailCutoff = new Date(now - SECOND_EMAIL_CUTOFF);
        CountHashMap<Status> stats = new CountHashMap<Status>();
        for (Integer memberId : lapsedIds) {
            Status result = sendRetentionEmail(memberId, secondEmailCutoff, filler);
            if (DeploymentConfig.devDeployment) {
                log.info("Retention email result (not sent)", "member", memberId, "result", result);
            }
            stats.incrementCount(result, 1);
            if (result.success && ++totalSent >= SEND_LIMIT) {
                break;
            }
        }

        // log results, we could use keys here but seeing e.g. OTHER = 0 might be comforting
        List<Object> statLog = Lists.newArrayList();
        for (Status r : Status.values()) {
            statLog.add(r);
            statLog.add(stats.getCount(r));
        }
        log.info("Finished retention mailing", statLog.toArray(new Object[statLog.size()]));
    }

    /**
     * For testing the retention email content, just sends a message to the given member id with no
     * checking. If non-null, send to the provided address instead of the member's address.
     * TODO: remove
     */
    public boolean testRetentionEmail (int memberId, String address)
    {
        MailContent result = sendRetentionEmail(
            _memberRepo.loadMember(memberId), address, loadFiller(), false);
        log.info("Sent test retention email", "memberId", memberId, "address", address,
            "result", result);
        return true;
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
     * Checks all relevant spam history and tries to send a retention email to the given member id.
     * The cutoff date is passed in for consistency since the feed mailer job could take a long time
     * to run.
     */
    protected Status sendRetentionEmail (int memberId, Date secondEmailCutoff, NewStuff filler)
    {
        Status result = Status.OTHER;
        try {
            result = trySendRetentionEmail(memberId, secondEmailCutoff, filler);

        } catch (Exception e) {
            log.warning("Failed to send retention mail - call 911", "memberId", memberId, e);
        }
        return result;
    }

    /**
     * Non-exception-aware version of the above.
     */
    protected Status trySendRetentionEmail (int memberId, Date secondEmailCutoff, NewStuff filler)
    {
        SpamRecord spamRec = _spamRepo.loadSpamRecord(memberId);
        Date last = spamRec == null ? null : spamRec.retentionSent;
        Status status = last == null ? null : Status.lookup(spamRec.retentionStatus);

        if (last != null && last.after(secondEmailCutoff) && status != null && status.success) {
            // spammed recently, skip
            return Status.TOO_RECENTLY_SPAMMED;
        }

        // load the member
        MemberRecord mrec = _memberRepo.loadMember(memberId);
        if (mrec == null) {
            log.warning("Member deleted during retention mailing?", "memberId", memberId);
            return Status.MEMBER_DELETED;
        }

        // skip placeholder addresses
        if (MemberMailUtil.isPlaceholderAddress(mrec.accountName)) {
            return Status.PLACEHOLDER_ADDRESS;
        }

        // skip invalid addresses
        if (!MailUtil.isValidAddress(mrec.accountName)) {
            return Status.INVALID_ADDRESS;
        }

        // oh look, they've logged in! maybe the email(s) worked. clear counter
        boolean persuaded = (last != null) && mrec.lastSession.after(last);
        if (persuaded) {
            spamRec.retentionCountSinceLogin = 0;
            // fall through, we'll send a mail and save the record below

        } else if (status == Status.NOT_ENOUGH_FRIENDS || status == Status.NOT_ENOUGH_NEWS) {
            // reset legacy failures, we now send filler for these people
            spamRec.retentionCountSinceLogin = 0;
            // fall through, we'll send a mail and save the record below

        } else if (spamRec != null && spamRec.retentionCountSinceLogin >= 2) {
            // they are never coming back... oh well, there are plenty of other fish in the sea
            return Status.LOST_CAUSE;
        }

        // sending the email could take a while so update the spam record here to reduce window
        // where other peers may conflict with us. NOTE: we do plan to run this job on multiple
        // servers some day
        _spamRepo.noteRetentionEmailSending(memberId, spamRec);

        // choose a successful result based on previous attempts
        status = Status.SENT_DORMANT;
        if (persuaded) {
            status = Status.SENT_PERSUADED;
        } else if (spamRec == null || spamRec.retentionCount == 0) {
            status = Status.SENT_LAPSED;
        }

        // now send the email
        MailContent content = sendRetentionEmail(mrec, null, filler, true);

        // NOTE: this is sort of redundant but increases the integrity of the spam record and
        // reduces chance of a user getting two emails when we are 1M strong
        _spamRepo.noteRetentionEmailResult(memberId, status.value);

        // log an event for successes. the result is the lapse status
        if (status.success) {
            _eventLog.retentionMailSent(mrec.memberId, mrec.visitorId, status.name(),
                content.subjectLine, content.bucket.name, content.numFriends,
                content.numPersonalMessages, mrec.isValidated());
        }

        return status;
    }

    /**
     * Does the heavy lifting of sending a retention email. Returns a result indicating what was
     * included in the mailing.
     * @param realDeal testing flag; if false, we make sure the mail is sent regardless
     */
    protected MailContent sendRetentionEmail (
        final MemberRecord mrec, String addressOverride, NewStuff filler, boolean realDeal)
    {
        int memberId = mrec.memberId;

        // load up friends and feed categories
        Set<Integer> friendIds = _memberRepo.loadFriendIds(mrec.memberId);
        List<FeedCategory> categories = Lists.newArrayList(); // FIXME(bruno)

        // count up all messages and ones that are personal
        int count = 0;
        int personalMessages = 0;
        for (FeedCategory cat : categories) {
            count += cat.messages.length;
            switch (cat.category) {
            case ANNOUNCEMENTS:
                for (FeedMessage fm : cat.messages) {
                    if (fm.type != FeedMessageType.GLOBAL_ANNOUNCEMENT) {
                        personalMessages++;
                    }
                }
                break;
            default:
                personalMessages += cat.messages.length;
                break;
            }
        }

        // assign the user a bucket
        Bucket bucket;
        if (friendIds.size() == 0) {
            bucket = Bucket.HAS_NO_FRIENDS;

        } else if (personalMessages == 0) {
            bucket = Bucket.HAS_INACTIVE_FRIENDS;

        } else {
            bucket = Bucket.HAS_PERSONAL_EVENTS;
        }

        // generate the feed if we have at least a couple of items (this will always be true on
        // production until we stop posting announcements frequently)
        Parameters params = new Parameters();
        if (count >= MIN_NEWS_ITEM_COUNT) {
            // prepare the generators!
            final Generator generators[] = {
                new Generator(memberId, new PlainTextBuilder(), _messages),
                new Generator(memberId, new HTMLBuilder(), _messages)};

            // convert to our wrapped categories and items
            List<EmailFeedCategory> ecats = Lists.newArrayList();
            for (FeedCategory category : categories) {
                List<EmailFeedItem> eitems = Lists.transform(
                    FeedMessageAggregator.aggregate(Arrays.asList(category.messages), false),
                    new Function<FeedMessage, EmailFeedItem>() {;
                        public EmailFeedItem apply (FeedMessage fm) {
                            return new EmailFeedItem(generators, mrec.memberId, fm);
                        }
                    });
                if (eitems.isEmpty()) {
                    continue;
                }
                EmailFeedCategory ecat = new EmailFeedCategory(category.category, eitems);
                ecats.add(ecat);
            }

            // Sort to CATEGORIES order
            Collections.sort(ecats);

            params.set("feed", ecats);

        } else {
            // otherwise, tell the template not to show the feed section at all
            params.set("feed", null);
        }

        // list of threads with unread posts from friends
        List<ForumThread> threads =
            _forumLogic.loadUnreadFriendThreads(mrec, 0, FRIEND_THREAD_COUNT);
        if (threads.size() > 0) {
            params.set("threads", Lists.transform(threads,
                new Function<ForumThread, EmailForumThread>() {
                    public EmailForumThread apply (ForumThread thread) {
                        return new EmailForumThread(thread);
                    }
                }));

        } else {
            params.set("threads", null);
        }

        // pick a random subject line based on the bucket
        String subjectLine = RandomUtil.pickRandom(bucket.subjectLines);
        String address = (addressOverride != null) ? addressOverride : mrec.accountName;

        // fire off the email, the template will take care of looping over categories and items
        // TODO: it would be great if we could somehow get the final result of actually sending the
        // mail. A lot of users have emails like 123@myass.com and we are currently counting them
        // as sent. I also like my pie at 30,000 feet please.
        // only generate the feed if we have at least a few items
        // TODO: maybe change the template based on the bucket too
        params.set("server_url", DeploymentConfig.serverURL);
        params.set("name", mrec.name);
        params.set("member_id", mrec.memberId);
        params.set("avatars", filler.avatars);
        params.set("games", filler.games);
        params.set("subject", subjectLine);
        params.set("optoutbits", SpamUtil.generateOptOutHash(memberId, address) + "_" + memberId);

        _mailSender.sendTemplateEmail(realDeal ? MailSender.By.COMPUTER : MailSender.By.HUMAN,
            address, ServerConfig.getFromAddress(), MAIL_TEMPLATE, params);
        return new MailContent(subjectLine, bucket, friendIds.size(), personalMessages);
    }

    /**
     * Load up the filler listings.
     */
    protected NewStuff loadFiller ()
    {
        NewStuff filler = new NewStuff();
        try {
            filler.avatars = randomItems(MsoyItemType.AVATAR);
            filler.games = randomItems(MsoyItemType.LAUNCHER);

        } catch (ServiceException e) {
            throw new RuntimeException("Could not create feed mailing filler", e);
        }

        return filler;
    }

    /**
     * Loads a random set of new & hot and staff pick items for the filler.
     */
    protected List<EmailListing> randomItems (MsoyItemType itemType)
        throws ServiceException
    {
        int count = ITEM_COUNT;

        // load up items from new & hot and staff picks (x2), no dupes
        Set<ListingCard> resultSet = Sets.newHashSet();
        resultSet.addAll(_catalogLogic.loadCatalog(
                             null, itemType, CatalogQuery.SORT_BY_NEW_AND_HOT, count));
        resultSet.addAll(_itemLogic.resolveFavorites(
                             _faveRepo.loadRecentFavorites(0, count * 2, itemType)));

        // get a random subset into a list
        List<ListingCard> result;
        if (resultSet.size() > count) {
            result = CollectionUtil.selectRandomSubset(resultSet, count);
        } else {
            result = Lists.newArrayList(resultSet);
        }

        // transform to velocity wrapper
        return Lists.transform(result, _toListing);
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
            return createStaticIcon(text, "images/whirled/friend_gained_level.png");
        }

        // from Builder
        public Icon createSubscribedIcon (String text) {
            return createStaticIcon(text, "images/whirled/friend_subscribed.png");
        }

        // from Builder
        public Icon createCommentedIcon (String text) {
            return createStaticIcon(text, "images/whirled/friend_commented.png");
        }

        // from Builder
        public Icon createPokedIcon (String text) {
            return createStaticIcon(text, "images/whirled/friend_added_friend.png");
        }

        // from Builder
        public Icon createFoundedGroupIcon (String text) {
            return createStaticIcon(text, "images/whirled/friend_created_group.png");
        }

        // from Builder
        public Icon createListedItemIcon (String text) {
            return createStaticIcon(text, "images/whirled/friend_listed_item.png");
        }

        // from Builder
        public Icon createUpdatedRoomIcon (String text) {
            return createStaticIcon(text, "images/whirled/friend_updated_room.png");
        }

        // from Builder
        public Icon createLikedMusicIcon (String text) {
            return createStaticIcon(text, "images/whirled/friend_liked_music.png");
        }

        protected Icon createStaticIcon (String text, String imageSrc)
        {
            return new StringWrapper(_html.reset().open("img",
                "src", imageSrc,
                "style", imgStyle(new Dimensions("30px", "20px"))).close().append(text).finish());
        }

        // from Builder
        public String createLink (String label, Pages page, Args args) {
            return _html.reset().open("a", "href", link(page, args),"style", A_STYLE).append(label)
                .finish();
        }

        // from Builder
        public Media createMedia (MediaDesc md, Pages page, Args args) {
            // start with the anchor
            _html.reset().open("a", "href", link(page, args), "style", A_STYLE);

            if (!md.isImage()) {
                // don't bother with other media types, just use some fakey text
                // TODO: should we worry about this? I don't think I've ever seen any non-image
                // media in my feed before...
                return new StringWrapper(_html.append("[X]").finish());
            }
            if (md instanceof HashMediaDesc) {
                md = MediaDescFactory.createMediaDesc((HashMediaDesc) md,
                    (int) (Calendars.now().addYears(1).toTime()/1000));
            }
            int size = MediaDescSize.HALF_THUMBNAIL_SIZE;
            if (page == Pages.WORLD && args.get(0, "").startsWith("s")) {
                // snapshots are unconstrained at a set size; fake a width constraint for
                // TINY_SIZE.
                if (md != null) {
                    md = md.newWithConstraint(MediaDesc.HORIZONTALLY_CONSTRAINED);
                }
                size = MediaDescSize.SNAPSHOT_TINY_SIZE;
            }
            int width = MediaDescSize.getWidth(size);
            int height = MediaDescSize.getHeight(size);
            Dimensions dim = SharedMediaUtil.resolveImageSize(md, width, height);
            if (dim == null) {
                dim = new Dimensions(width + "px", height + "px");
            }
            return new StringWrapper(_html.open("img", "src", md.getMediaPath(),
                "style", imgStyle(dim)).finish());
        }

        protected static String link (Pages page, Args args)
        {
            return page.makeURL(args);
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
        public Icon createSubscribedIcon (String text) {
            return new StringWrapper(text);
        }

        // from Builder
        public Icon createCommentedIcon (String text) {
            return new StringWrapper(text);
        }

        // from Builder
        public Icon createPokedIcon (String text) {
            return new StringWrapper(text);
        }

        // from Builder
        public Icon createFoundedGroupIcon (String text) {
            return new StringWrapper(text);
        }

        // from Builder
        public Icon createListedItemIcon (String text) {
            return new StringWrapper(text);
        }

        // from Builder
        public Icon createUpdatedRoomIcon (String text) {
            return new StringWrapper(text);
        }

        // from Builder
        public Icon createLikedMusicIcon (String text) {
            return new StringWrapper(text);
        }

        // from Builder
        public String createLink (String label, Pages page, Args args) {
            return label;
        }

        // from Builder
        public Media createMedia (MediaDesc md, Pages page, Args args) {
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
     * Data for prepending to retention emails.
     */
    protected static class NewStuff
    {
        /** New avatar listings. */
        public List<EmailListing> avatars;

        /** New game listings. */
        protected List<EmailListing> games;
    }

    /**
     * The status of a user in regard to retention email sending.
     */
    protected enum Status
    {
        // successful results
        SENT_LAPSED(1, true), SENT_PERSUADED(2, true), SENT_DORMANT(3, true),

        // failed results
        TOO_RECENTLY_SPAMMED(4), NOT_ENOUGH_FRIENDS(5), NOT_ENOUGH_NEWS(6), MEMBER_DELETED(7),
        PLACEHOLDER_ADDRESS(8), INVALID_ADDRESS(9), LOST_CAUSE(10), OTHER(11);

        /** Persisted value for results. */
        public int value;

        /** Whether the results indicates a successfully sent message. */
        public boolean success;

        /**
         * Returns the result with the given code, or null if none.
         */
        public static Status lookup (int value)
        {
            return _lookup.get(value);
        }

        Status (int value)
        {
            this(value, false);
        }

        Status (int value, boolean success)
        {
            this.value = value;
            this.success = success;
        }
    }

    /**
     * Relevant information on the content of a sent retention mailing.
     */
    protected static class MailContent
    {
        /** Which subject line was sent. */
        public String subjectLine;

        /** The bucket this user was assigned to. */
        public Bucket bucket;

        /** The number of friends this user has. */
        public int numFriends;

        /** The number of non-global messages in the user's feed. */
        public int numPersonalMessages;

        /** Creates a new content summary. */
        public MailContent (
            String subjectLine, Bucket bucket, int numFriends, int numPersonalMessages)
        {
            this.subjectLine = subjectLine;
            this.bucket = bucket;
            this.numFriends = numFriends;
            this.numPersonalMessages = numPersonalMessages;
        }

        public String toString ()
        {
            StringBuilder buf = new StringBuilder(getClass().getSimpleName());
            buf.append(" [");
            StringUtil.fieldsToString(buf, this);
            buf.append("]");
            return buf.toString();
        }
    }

    /**
     * Returns a random subset of a list, or the list itself if there are not enough items.
     */
    protected static <T> List<T> randomSubset (int size, List<T> list)
    {
        if (list.size() < size) {
            return list;
        }
        return CollectionUtil.selectRandomSubset(list, size);
    }

    /**
     * Appends items from list2 to a copy of list1 that are not already in list1, then returns a
     * random subset of the copy.
     */
    protected static <T> List<T> uniqueRandomSubset (int size, List<T> list1, List<T> list2)
    {
        List<T> result = Lists.newArrayListWithCapacity(size);
        result.addAll(list1);
        if (list2 != null) {
            for (T item : list2) {
                if (!result.contains(item)) {
                    result.add(item);
                }
            }
        }
        return randomSubset(size, result);
    }

    /** Converts listing cards to the velocity listing. */
    protected Function<ListingCard, EmailListing> _toListing =
        new Function<ListingCard, EmailListing> () {
            public EmailListing apply (ListingCard card) {
                return new EmailListing(card);
            }
        };

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
        public String medal (String medal, String group) {
            return _pmsgs.get("medal", medal, group);
        }

        // from Messages
        public String unknownMember () {
            return _pmsgs.get("feedProfileMemberUnknown");
        }

        // from Messages
        public String action (
            FeedMessage message, String subject, String object, Plural plural) {
            switch (message.type) {
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
            case SELF_ITEM_COMMENT:
            case SELF_GAME_COMMENT:
            case SELF_PROFILE_COMMENT:
                if (message.type == FeedMessageType.SELF_PROFILE_COMMENT) {
                    object = _pmsgs.get("wall", object);
                }
                switch (message.getCommentVerb()) {
                case FeedMessage.COMMENT_REPLIED:
                    // We can assume "your" here, others won't be viewing your self feed messages
                    return _pmsgs.get("selfCommentReply", subject, _pmsgs.get("your"), object);
                case FeedMessage.COMMENT_FOLLOWED_UP:
                    return _pmsgs.get("selfCommentFollowUp", subject, _pmsgs.get("your"), object);
                default:
                    return _pmsgs.get("selfComment", subject, object);
                }

            case SELF_FORUM_REPLY:
                switch (plural) {
                default:
                case NONE:
                    return _pmsgs.get("selfPersonRepliedToForumPost", subject, object);
                case SUBJECT:
                    return _pmsgs.get("selfPeopleRepliedToForumPost", subject, object);
                case OBJECT:
                    return _pmsgs.get("selfPersonRepliedToForumPosts", subject, object);
                }

            case FRIEND_PLAYED_GAME:
                switch (plural) {
                default:
                case NONE:
                    return _pmsgs.get("friendPlayedGame", subject, object);
                case SUBJECT:
                    return _pmsgs.get("friendsPlayedGame", subject, object);
                case OBJECT:
                    return _pmsgs.get("friendPlayedGames", subject, object);
                }

            case FRIEND_CREATED_GROUP:
                return _pmsgs.get("friendCreatedGroup", subject, object);

            case FRIEND_JOINED_GROUP:
                switch (plural) {
                default:
                case NONE:
                    return _pmsgs.get("friendJoinedGroup", subject, object);
                case SUBJECT:
                    return _pmsgs.get("friendsJoinedGroup", subject, object);
                case OBJECT:
                    return _pmsgs.get("friendJoinedGroups", subject, object);
                }

            case FRIEND_SUBSCRIBED:
                return plural == Plural.SUBJECT ?
                    _pmsgs.get("friendsSubscribed", subject) :
                    _pmsgs.get("friendSubscribed", subject);
            }

            return subject + " " + message.type + " " + object + " (plural: " + plural + ").";
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

    @Inject protected @BatchInvoker Invoker _batchInvoker;
    @Inject protected CatalogLogic _catalogLogic;
    @Inject protected CronLogic _cronLogic;
    @Inject protected FavoritesRepository _faveRepo;
    @Inject protected FeedLogic _feedLogic;
    @Inject protected ForumLogic _forumLogic;
    @Inject protected GroupLogic _groupLogic;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MailSender _mailSender;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected SpamRepository _spamRepo;

    /** Map of result codes to results. */
    protected static Map<Integer, Status> _lookup = Maps.newHashMap();

    protected static final int ITEMS_PER_CATEGORY = 50;
    protected static final String MAIL_TEMPLATE = "feed";
    protected static final int LAPSED_CUTOFF = 3 * 24*60*60*1000;
    protected static final int SECOND_EMAIL_CUTOFF = 10 * 24*60*60*1000;
    protected static final int MIN_NEWS_ITEM_COUNT = 2;
    protected static final int ITEM_COUNT = 5;
    protected static final int SEND_LIMIT = DeploymentConfig.devDeployment ? 100 : 5000;
    protected static final int FRIEND_THREAD_COUNT = 5;

    protected static final String IMG_STYLE = "border: 0px; padding: 2px; margin: 2px; " +
        "vertical-align: middle;";
    protected static final String A_STYLE = "text-decoration: none;";

    /** We want these categories first. */
    protected static final Category[] CATEGORIES = {Category.ANNOUNCEMENTS, Category.LISTED_ITEMS,
        Category.ROOMS, Category.GAMES, Category.FRIENDINGS};

    static
    {
        // set up the static lookup table for result codes
        for (Status result : Status.values()) {
            _lookup.put(result.value, result);
        }
    }
}
