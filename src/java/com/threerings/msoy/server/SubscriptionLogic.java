//
// $Id$

package com.threerings.msoy.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.annotation.BlockingThread;

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
     * Note that the specified user has purchased a subscription, either interactively or
     * through a recurring billing.
     *
     * @throws Exception We freak the fuck out if anything goes wrong.
     */
    @BlockingThread
    public void noteSubscriptionStarted (String accountName, long endTime)
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
        }
        // create or update their subscription record
        _subscripRepo.noteSubscription(mrec.memberId, endTime);
        // TODO: FFS, turn them into a subscriber instantly, whereever they are
        // grant them their monthly bar allowance
        int bars = _runtime.money.monthlySubscriberBarGrant;
        if (bars > 0) {
            _moneyLogic.grantSubscriberBars(mrec.memberId, bars);
        }
        // TODO: give them the current Item Of The Month
    }

    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected SubscriptionRepository _subscripRepo;
}
