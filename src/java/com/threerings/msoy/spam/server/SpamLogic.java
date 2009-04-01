//
// $Id$

package com.threerings.msoy.spam.server;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.net.MailUtil;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.CountHashMap;
import com.samskivert.util.IntSet;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberMailUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.FavoritesRepository;
import com.threerings.msoy.item.server.persist.GameRecord;

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
import com.threerings.msoy.web.gwt.ServiceException;
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
     * Item listing for the purposes of filling in a velocity template. NOTE: this class must be
     * public so that velocity can inspect its members.
     */
    public class Listing
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

        protected Listing (ListingCard listing)
        {
            _listing = listing;
            if (listing.itemType == Item.GAME) {
                try{
                    _gameId = ((GameRecord)_itemLogic.requireListing(
                        Item.GAME, listing.catalogId, true).item).gameId;
                } catch (ServiceException e) {
                    log.warning("Could not load game id for new & hot game",
                        "catalogID", listing.catalogId);
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
        if (DeploymentConfig.devDeployment) {
            _cronLogic.scheduleAt(1, new Runnable () {
                public void run () {
                    sendFeedEmails();
                }
                public String toString () {
                    return "News feed emailer";
                }
            });
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

        // load the filler, data from this is included in all mailings
        NewStuff filler = loadFiller();

        // do the sending and record results
        int totalSent = 0;
        Date secondEmailCutoff = new Date(now - SECOND_EMAIL_CUTOFF);
        CountHashMap<Result> stats = new CountHashMap<Result>();
        for (Integer memberId : lapsedIds) {
            Result result = sendFeedEmail(memberId, secondEmailCutoff, filler);
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
        log.info("Finished feed mailing", statLog.toArray(new Object[statLog.size()]));
    }

    /**
     * For testing the feed email content, just sends a message to the given member id with no
     * checking.
     * TODO: remove
     */
    public boolean testFeedEmail (int memberId, String address)
    {
        Result result = sendFeedEmail(
            _memberRepo.loadMember(memberId), Result.SENT_LAPSED, address, loadFiller(), false);
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
    protected Result sendFeedEmail (int memberId, Date secondEmailCutoff, NewStuff filler)
    {
        Result result = Result.OTHER;
        try {
            result = trySendFeedEmail(memberId, secondEmailCutoff, filler);

        } catch (Exception e) {
            log.warning("Failed to send feed", "memberId", e);
        }
        return result;
    }

    /**
     * Non-exception-aware version of the above.
     */
    protected Result trySendFeedEmail (int memberId, Date secondEmailCutoff, NewStuff filler)
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
        result = sendFeedEmail(mrec, result, null, filler, true);

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
        final MemberRecord mrec, Result successResult, String addressOverride, NewStuff filler,
        boolean realDeal)
    {
        int memberId = mrec.memberId;

        // load up friends and feed categories
        IntSet friendIds = _memberRepo.loadFriendIds(mrec.memberId);
        List<FeedCategory> categories = friendIds.size() > 0 ? _feedLogic.loadFeedCategories(
            mrec, friendIds, ITEMS_PER_CATEGORY, null) : new ArrayList<FeedCategory>();

        // count up all messages
        int count = 0;
        for (FeedCategory cat : categories) {
            count += cat.messages.length;
        }

        // generate the feed if we have at least a couple of items
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

            params.set("feed", ecats);

        } else {
            // otherwise, tell the template not to show the feed section at all
            params.set("feed", null);
        }

        // fire off the email, the template will take care of looping over categories and items
        // TODO: it would be great if we could somehow get the final result of actually sending the
        // mail. A lot of users have emails like 123@myass.com and we are currently counting them
        // as sent. I also like my pie at 30,000 feet please.
        // only generate the feed if we have at least a few items
        params.set("server_url", DeploymentConfig.serverURL);
        params.set("name", mrec.name);
        params.set("member_id", mrec.memberId);
        params.set("avatars", filler.avatars);
        params.set("furniture", filler.furniture);
        params.set("games", filler.games);
        String address = addressOverride != null ? addressOverride : mrec.accountName;
        _mailSender.sendTemplateEmail(realDeal ? MailSender.By.COMPUTER : MailSender.By.HUMAN,
            address, ServerConfig.getFromAddress(), MAIL_TEMPLATE, params);
        return successResult;
    }

    /**
     * Load up the filler listings.
     */
    protected NewStuff loadFiller ()
    {
        NewStuff filler = new NewStuff();
        ArrayIntSet memberIds = new ArrayIntSet(ServerConfig.getShopFavoriteMemberIds());
        try {
            filler.avatars = randomItems(memberIds, Item.AVATAR);
            filler.furniture = randomItems(memberIds, Item.FURNITURE);
            filler.games = randomGames();

        } catch (ServiceException e) {
            throw new RuntimeException("Could not create feed mailing filler", e);
        }

        return filler;
    }

    /**
     * Loads a random set of new & hot games for the filler.
     */
    protected List<Listing> randomGames ()
        throws ServiceException
    {
        List<ListingCard> games = randomSubset(GAME_COUNT, loadNewAndHot(Item.GAME, GAME_COUNT*2));
        return Lists.transform(games, _toListing);
    }

    /**
     * Loads a random set of new & hot and staff pick items for the filler.
     */
    protected List<Listing> randomItems (Collection<Integer> memberIds, byte itemType)
        throws ServiceException
    {
        List<ListingCard> items = uniqueRandomSubset(ITEM_COUNT, loadNewAndHot(
            itemType, ITEM_COUNT * 2), loadFavorites(memberIds, itemType, ITEM_COUNT * 2));
        return Lists.transform(items, _toListing);
    }

    /**
     * Loads the given number of new & hot items of the given type.
     */
    protected List<ListingCard> loadNewAndHot (byte itemType, int count)
        throws ServiceException
    {
        return Lists.transform(_itemLogic.getRepository(itemType).loadCatalog(
            CatalogQuery.SORT_BY_NEW_AND_HOT, false, null, 0, 0, null, 0, 0, count),
            CatalogRecord.TO_CARD);
    }

    /**
     * Loads the given number of recent favorites of the given member ids.
     */
    protected List<ListingCard> loadFavorites (
        Collection<Integer> memberIds, byte itemType, int count)
        throws ServiceException
    {
        return _itemLogic.resolveFavorites(
            _faveRepo.loadRecentFavorites(memberIds, count, itemType));
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
            return Pages.makeLink(page, args);
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
     * Data for prepending to retention emails.
     */
    protected static class NewStuff
    {
        /** New avatar listings. */
        public List<Listing> avatars;

        /** New furniture listings. */
        protected List<Listing> furniture;

        /** New game listings. */
        protected List<Listing> games;
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
    protected Function<ListingCard, Listing> _toListing = new Function<ListingCard, Listing> () {
        public Listing apply (ListingCard card) {
            return new Listing(card);
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

    @Inject protected @BatchInvoker Invoker _batchInvoker;
    @Inject protected CronLogic _cronLogic;
    @Inject protected FeedLogic _feedLogic;
    @Inject protected MailSender _mailSender;
    @Inject protected MemberRepository _memberRepo; 
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected SpamRepository _spamRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected FavoritesRepository _faveRepo;

    protected static final int ITEMS_PER_CATEGORY = 50;
    protected static final String MAIL_TEMPLATE = "feed";
    protected static final int LAPSED_CUTOFF = 3 * 24*60*60*1000;
    protected static final int SECOND_EMAIL_CUTOFF = 10 * 24*60*60*1000;
    protected static final int MIN_NEWS_ITEM_COUNT = 2;
    protected static final int ITEM_COUNT = 5;
    protected static final int GAME_COUNT = 10;
    protected static final int SEND_LIMIT = DeploymentConfig.devDeployment ? 100 : 1000;

    protected static final String IMG_STYLE = "border: 0px; padding: 2px; margin: 2px; " +
        "vertical-align: middle;";
    protected static final String A_STYLE = "text-decoration: none;";

    /** We want these categories first. */
    protected static final Category[] CATEGORIES = {Category.ANNOUNCEMENTS, Category.LISTED_ITEMS, 
        Category.ROOMS, Category.TROPHIES, Category.FRIENDINGS};
}
