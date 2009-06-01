//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.DeploymentConfig;

import com.threerings.msoy.server.persist.BatchInvoker;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.SubscriptionRepository;

import com.threerings.msoy.admin.server.RuntimeConfig;
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
        // let's grant bars to subscribers that need them, every hour
        _cronLogic.scheduleAt(1, new Runnable() {
            public void run () {
                _batchInvoker.postUnit(new Invoker.Unit(toString()) {
                    public boolean invoke () {
                        grantBars();
                        return false;
                    }
                });
            }

            public String toString () {
                return "SubscriptionLogic.grantBars";
            }
        });

//        // TEMP TEMP TEMP
//        if (DeploymentConfig.devDeployment) {
//            new Interval(_batchInvoker) {
//                public void expired () {
//                    flip = !flip;
//                    try {
//                        if (flip) {
//                            noteSubscriptionStarted("ray@bogocorp.com", 1);
//                            System.err.println("==== Made ray a subscriber");
//                        } else {
//                            noteSubscriptionEnded("ray@bogocorp.com");
//                            System.err.println("==== Made ray NOT a subscriber");
//                        }
//                    } catch (Exception e) {
//                        log.warning("Test problem", e);
//                    }
//                }
//
//                protected boolean flip;
//
//            }.schedule(15 * 1000, true);
//        }
//        // END: TEMP TEMP TEMP
    }

    /**
     * Note that the specified user has purchased a subscription, either interactively or
     * through a recurring billing.
     *
     * @throws Exception We freak the fuck out if anything goes wrong.
     */
    @BlockingThread
    public void noteSubscriptionStarted (String accountName, int months)
        throws Exception
    {
        // first, look up the record of the member
        MemberRecord mrec = _memberRepo.loadMember(accountName);
        if (mrec == null) {
            throw new Exception("Could not locate MemberRecord");
        }
        // make them a subscriber if not already
        if (mrec.updateFlag(MemberRecord.Flag.SUBSCRIBER, true)) {
            _memberRepo.storeFlags(mrec);
            MemberNodeActions.tokensChanged(mrec.memberId, mrec.toTokenRing());
        }
        // create or update their subscription record
        int barGrantsLeft = months; // make explicit this equivalence
        boolean grantBars = _subscripRepo.noteSubscriptionStarted(mrec.memberId, barGrantsLeft);
        // TODO: FFS, turn them into a subscriber instantly, whereever they are.

        // Grant them their monthly bar allowance.
        int bars = _runtime.money.monthlySubscriberBarGrant;
        if (grantBars && bars > 0) {
            _moneyLogic.grantSubscriberBars(mrec.memberId, bars);
        }
        // TODO: give them the current Item Of The Month. However, if they already received the
        // item of the month; because they were a subscriber when it came out, or if we don't
        // quite update it monthly and they already got it during the last billing cycle; don't.
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
        if (mrec.updateFlag(MemberRecord.Flag.SUBSCRIBER, false)) {
            _memberRepo.storeFlags(mrec);
            MemberNodeActions.tokensChanged(mrec.memberId, mrec.toTokenRing());
        } else {
            log.warning("Weird! A subscription-end message arrived for a non-subscriber",
                "accountName", accountName);
            return;
        }

        // look up their subscription record and make sure the end time is now.
        int barGrantsLeft = _subscripRepo.noteSubscriptionEnded(mrec.memberId);
        if (barGrantsLeft != 0) {
            log.warning("Shazbot! Bars grants left is not 0!",
                "accountName", accountName, "grantsLeft", barGrantsLeft, new Exception());
        }
    }

    /**
     * Grant bars to any subscribers that are quarterly or yearly and need their monthly bars.
     */
    @BlockingThread
    protected void grantBars ()
    {
        // figure out how many bars we're going to be granting
        int bars = _runtime.money.monthlySubscriberBarGrant;
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

    @Inject protected CronLogic _cronLogic;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected SubscriptionRepository _subscripRepo;
    @Inject protected @BatchInvoker Invoker _batchInvoker;
}
