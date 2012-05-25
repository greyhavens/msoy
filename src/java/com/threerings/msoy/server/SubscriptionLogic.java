//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ByteEnumUtil;
import com.samskivert.util.Calendars;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.cron.server.CronLogic;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.server.FeedLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.SubscriptionRecord;
import com.threerings.msoy.server.persist.SubscriptionRepository;

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
