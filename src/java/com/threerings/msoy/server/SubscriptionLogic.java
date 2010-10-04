//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.util.ByteEnumUtil;
import com.samskivert.util.Calendars;
import com.samskivert.util.StringUtil;

import com.threerings.cron.server.CronLogic;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.SubscriptionRecord;
import com.threerings.msoy.server.persist.SubscriptionRepository;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.server.FeedLogic;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.money.server.MoneyLogic;

import static com.threerings.msoy.Log.log;

/**
 * Manages user subscriptions.
 */
@Singleton
public class SubscriptionLogic
{
    public SubscriptionLogic ()
    {
    }

    /**
     * Initialize our subscription logic.
     */
    public void init ()
    {
        // check for bar and special item grants every hour
        _cronLogic.scheduleEvery(1, "SubscriptionLogic hourly", new Runnable() {
            public void run () {
                endBarscribers();
                grantBars();
                grantSpecialItem();
            }

            public String toString () {
                return "SubscriptionLogic.hourly";
            }
        });

        // Temporary hack for subscription booch early October 2010 // Zell
        if (DeploymentConfig.devDeployment) {
            return;
        }
        _subscripRepo.registerMigrationHack (new DataMigration("2010_10_04_subscription_booch") {
            @Override public void invoke () throws DatabaseException {
                String[] accounts = {
                    "igorthebrazilian@yahoo.com", "leftwich_jevon@yahoo.com",
                    "xxxnigger@hotmail.com", "khreelah@yahoo.com", "onlysoniclover@aol.com",
                    "raqennu14@aol.com", "igortriska@yahoo.com", "mattprz1022@yahoo.com",
                    "iwantthatavi@aol.com", "tomblair01@hotmail.com", "almaguer111@gmail.com",
                    "johnrocks1219@yahoo.com", "guitarfreak74@yahoo.com",
                    "snookybloomalan@gmail.com", "lordofthoru@yahoo.com", "loganrezendes@yahoo.com",
                    "savannahwaldo@yahoo.com", "nailmagik@yahoo.com", "halodixie@live.co.uk",
                    "shaneboyw@comcast.net", "sarab621@gmail.com", "sandyhuezo22@yahoo.com",
                    "hooksdh@yahoo.com", "southrowkid@hotmail.com", "sirenia_uchiha@hotmail.com",
                    "johnytremian1234@gmail.com", "johnytremian12345@gmail.com",
                    "munoz.john10@gmail.com", "kayla_baker0@yahoo.com",
                    "liamthegoldelite@hotmail.com", "asshole80@live.com",
                    "freekshow.jhon@luckymail.com", "extraless@live.com.au",
                    "lalfredolandaverde@yahoo.com", "dostlund@sbcglobal.net",
                    "natifishtorn@sbcglobal.net", "sk8rguyx@sbcglobal.net", "mara.taco@yahoo.com",
                    "lainef_14@hotmail.com", "jacksonrobert@hotmail.co.uk",
                    "rabbittoykeith@aim.com", "teioh@att.net", "silver.hedgehog29@yahoo.com",
                    "roma.levesque.rhs@gmail.com", "moondolly@optusnet.com.au",
                    "raikou34@hotmail.com", "jordan_stratton@yahoo.com", "gintamaster@yahoo.com",
                    "mythicella@aim.com", "floralists@hotmail.com", "goobert369@googlemail.com",
                    "mcclellanamy60@yahoo.com", "librairies@hotmail.com", "blindpilot@hotmail.com",
                    "cyprex@post.com", "tylercarstens@yahoo.com", "heatherjkx125@myway.com",
                    "banana_phone_1@hotmail.com", "banana_phone_2@hotmail.com", "mweal01@yahoo.com",
                    "zimandgirfan@hotmail.com", "pennyman.destany@yahoo.com",
                    "whirledstuff@live.ca", "katrina00181@mail.com", "joseph.barnette9@gmail.com",
                    "cocoamrx51@hotmail.com", "zell@threerings.net", "alptokat@hotmail.com"
                    
                };
                for (String account : accounts) {
                    log.info("Retroactively noting monthly subscription", "account", account);
                    try {
                        noteSubscriptionBilled(account, 1);
                    } catch (Exception e) {
                        log.warning("Subscription failed!", e);
                    }
                }
            }
        });
    }

    @BlockingThread
    public void barscribe (MemberRecord mrec)
    {
        // insert a record saying they're paid up for a month
        _subscripRepo.noteBarscribed(mrec.memberId);
        noteSubscriptionBilled(mrec, 0 /* no bar granting! */, false /* not from billing */);
    }

    /**
     * Note that the specified user has been billed for a subscription, either interactively or
     * through a recurring billing.
     *
     * @throws Exception We freak the fuck out if anything goes wrong.
     */
    @BlockingThread
    public void noteSubscriptionBilled (String accountName, int months)
        throws Exception
    {
        // first, look up the record of the member
        MemberRecord mrec = _memberRepo.loadMember(accountName);
        if (mrec == null) {
            throw new Exception("Could not locate MemberRecord");
        }
        noteSubscriptionBilled(mrec, months, true);
    }

    /**
     * Note that the specified user's subscription has ended.
     *
     * @throws Exception We freak the fuck out if anything goes wrong.
     */
    @BlockingThread
    public void noteSubscriptionEnded (String accountName)
        throws Exception
    {
        MemberRecord mrec = _memberRepo.loadMember(accountName);
        if (mrec == null) {
            throw new Exception("Could not locate MemberRecord");
        }
        noteSubscriptionEnded(mrec);
    }

    /**
     * Note that the specified user has been billed for a subscription, either interactively or
     * through a recurring billing.
     *
     * @throws Exception We freak the fuck out if anything goes wrong.
     */
    @BlockingThread
    protected void noteSubscriptionBilled (MemberRecord mrec, int months, boolean fromBilling)
    {
        // If coming from billing, blow away any "barscription" record they may have.
        // Since the billing system does not know about the barscription,
        // they lose any unused time.
        // TODO: we could add complexity sauce and have the billing server query us for remaining
        // barscription time and add that to their first month of subscription... ugh.
        if (fromBilling && _subscripRepo.noteBarscriptionEnded(mrec.memberId)) {
            log.warning("A new subscriber was previously a barscriber. Their extra time was lost.",
                "memberId", mrec.memberId);
        }

        // make them a subscriber if not already
        if (mrec.updateFlag(MemberRecord.Flag.SUBSCRIBER, true)) {
            _memberRepo.storeFlags(mrec);
            MemberNodeActions.tokensChanged(mrec.memberId, mrec.toTokenRing());
            _feedLogic.publishMemberMessage(mrec.memberId, FeedMessageType.FRIEND_SUBSCRIBED);
        }

        // Create or update their subscription record. (months == barGrantsLeft)
        SubscriptionRecord rec = _subscripRepo.noteSubscriptionBilled(mrec.memberId, months);

        // see if they need the special item right now
        CatalogRecord listing = getSpecialItem();
        if (listing != null) {
            MsoyItemType type = listing.item.getType();
            int itemId = listing.item.itemId;
            if ((type != rec.specialItemType) || (itemId != rec.specialItemId)) {
                // they need the special item
                try {
                    _itemLogic.grantItem(mrec, listing);
                    _subscripRepo.noteSpecialItemGranted(mrec.memberId, type, itemId);
                } catch (Exception e) {
                    log.warning("Unable to grant a subscriber their special item",
                        "memberId", mrec.memberId, e);
                }
            }
        }

        // see if they need their monthly bar allowance
        if (rec.grantsLeft > 0) {
            // make sure the last granting was at least a month ago, otherwise they'll get it later
            if ((rec.lastGrant == null) ||
                    (rec.lastGrant.getTime() < Calendars.now().addMonths(-1).toTime())) {
                try {
                    int bars = _runtime.subscription.monthlyBarGrant;
                    if (bars > 0) {
                        _moneyLogic.grantSubscriberBars(mrec.memberId, bars);
                    }
                    _subscripRepo.noteBarsGranted(mrec.memberId);
                } catch (Exception e) {
                    log.warning("Unable to grant a subscriber their monthly bars",
                        "memberId", mrec.memberId, e);
                }
            }
        }
    }

    @BlockingThread
    protected void noteSubscriptionEnded (MemberRecord mrec)
    {
        if (mrec.updateFlag(MemberRecord.Flag.SUBSCRIBER, false)) {
            _memberRepo.storeFlags(mrec);
            MemberNodeActions.tokensChanged(mrec.memberId, mrec.toTokenRing());
        } else {
            log.warning("Weird! A subscription-end message arrived for a non-subscriber",
                "accountName", mrec.accountName);
            // but continue on... we need to make sure they're really cleared
        }

        // end their subscription
        _subscripRepo.noteSubscriptionEnded(mrec.memberId);
    }

    /**
     * Turn off subscriptions for any expired barscribers.
     */
    @BlockingThread
    protected void endBarscribers ()
    {
        List<Integer> memberIds = _subscripRepo.loadExpiredBarscribers();
        for (Integer memberId : memberIds) {
            try {
                MemberRecord mrec = _memberRepo.loadMember(memberId);
                noteSubscriptionEnded(mrec);
                _subscripRepo.noteBarscriptionEnded(memberId);
            } catch (Exception e) {
                log.warning("Unable to end barscriber", "memberId", memberId, e);
            }
        }
    }

    /**
     * Grant bars to any subscribers that are quarterly or yearly and need their monthly bars.
     */
    @BlockingThread
    protected void grantBars ()
    {
        // figure out how many bars we're going to be granting
        int bars = _runtime.subscription.monthlyBarGrant;
        List<Integer> memberIds = _subscripRepo.loadSubscribersNeedingBarGrants();
        for (Integer memberId : memberIds) {
            // let's do each one in turn, don't let one booch hork the whole set
            try {
                if (bars > 0) {
                    _moneyLogic.grantSubscriberBars(memberId, bars);
                }
                // always note the granting, even if it was 0.
                _subscripRepo.noteBarsGranted(memberId);
            } catch (Exception e) {
                log.warning("Unable to grant a subscriber their monthly bars",
                    "memberId", memberId, e);
            }
        }
    }

    /**
     * Grant the special item to any current subscribers, if they haven't gotten it already.
     */
    @BlockingThread
    protected void grantSpecialItem ()
    {
        CatalogRecord listing = getSpecialItem();
        if (listing == null) {
            return;
        }

        MsoyItemType type = listing.item.getType();
        int itemId = listing.item.itemId;
        List<Integer> memberIds = _subscripRepo.loadSubscribersNeedingItem(type, itemId);
        for (Integer memberId : memberIds) {
            // do each one separately
            try {
                MemberRecord mrec = _memberRepo.loadMember(memberId);
                _itemLogic.grantItem(mrec, listing);
                _subscripRepo.noteSpecialItemGranted(memberId, type, itemId);
            } catch (Exception e) {
                log.warning("Unable to grant a subscriber their special item",
                    "memberId", memberId, e);
            }
        }
    }

    /**
     * Resolve the CatalogRecord of the current special item, or null if none.
     */
    @BlockingThread
    protected CatalogRecord getSpecialItem ()
    {
        String specialItem = _runtime.subscription.specialItem;
        if (!StringUtil.isBlank(specialItem) && !"0:0".equals(specialItem)) {
            try {
                String[] tokens = specialItem.split(":");
                if (tokens.length != 2) {
                    throw new IllegalArgumentException("Format is '<type>:<id>'");
                }
                ItemIdent ident = new ItemIdent(
                        ByteEnumUtil.fromByte(MsoyItemType.class, Byte.parseByte(tokens[0])),
                        Integer.parseInt(tokens[1]));
                return _itemLogic.requireListing(ident.type, ident.itemId, true);

            } catch (Exception e) {
                log.warning("Trouble resolving special item", "ident", specialItem, e);
            }
        }
        return null;
    }

    @Inject protected CronLogic _cronLogic;
    @Inject protected FeedLogic _feedLogic;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected SubscriptionRepository _subscripRepo;
}
