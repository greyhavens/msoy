package com.threerings.msoy.server;

import java.util.Date;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.depot.Exps;
import com.samskivert.util.Calendars;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.money.server.persist.MemberAccountRecord;
import com.threerings.msoy.server.persist.EntryVectorRecord;
import com.threerings.msoy.server.persist.FunnelEntryRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.presents.annotation.BlockingThread;

@Singleton @BlockingThread
public class FunnelSummary
{
    public enum Phase {
        VISITED,
        PLAYED,
        REGISTERED,
        RETURNED,
        RETAINED,
        PAID,
        SUBSCRIBED
    }

    public static class FunnelBit
    {
        public Phase phase;
        public String vector;
        public Date date;

        public FunnelBit (Phase phase, String vector, Date date)
        {
            this.phase = phase;
            this.vector = vector;
            this.date = date;
        }

        @Override
        public int hashCode ()
        {
            return 17*(31*vector.hashCode() + date.hashCode()) + phase.hashCode();
        }

        @Override
        public boolean equals (Object other)
        {
            if (other == null || !other.getClass().equals(this.getClass())) {
                return false;
            }
            FunnelBit otherKey = (FunnelBit) other;
            return otherKey.vector.equals(vector) && otherKey.date.equals(date) &&
                otherKey.phase == phase;
        }
    }

    @Inject public FunnelSummary ()
    {
    }

    public Multiset<FunnelBit> getFunnelSummary ()
    {
        synchronized(_entries) {
            if (_entries == null || new Date().after(_expiration)) {
                createFunnel();
            }
            return Multisets.unmodifiableMultiset(_entries);
        }
    }

    protected void createFunnel ()
    {
        _entries.clear();

        // total visitors is just all the recent EntryVectorRecord rows
        fromRecords(Phase.VISITED, _memberRepo.funnelQuery(null, null));

        // people who have played have an entry in MemberRecord, so join against that
        fromRecords(Phase.PLAYED, _memberRepo.funnelQuery(MemberRecord.MEMBER_ID, null));

        // people who registered have a non-anonymous account name
        fromRecords(Phase.REGISTERED, _memberRepo.funnelQuery(MemberRecord.MEMBER_ID,
            MemberRecord.ACCOUNT_NAME.notLike(MemberMailUtil.PERMAGUEST_SQL_PATTERN)));

        // people who returned have a session at least 24 hours after their creation time
        fromRecords(Phase.RETURNED, _memberRepo.funnelQuery(MemberRecord.MEMBER_ID,
            MemberRecord.LAST_SESSION.minus(EntryVectorRecord.CREATED)
                .greaterEq(Exps.days(RETURNED_DAYS))));

        // people who were retained have a session at least 7 days after their creation time
        fromRecords(Phase.RETAINED, _memberRepo.funnelQuery(MemberRecord.MEMBER_ID,
            MemberRecord.LAST_SESSION.minus(EntryVectorRecord.CREATED)
                .greaterEq(Exps.days(RETAINED_DAYS))));

        // people who paid are actually those who have accumulated bars one way or another
        // TODO: make this an actual payment check?
        fromRecords(Phase.PAID, _memberRepo.funnelQuery(MemberAccountRecord.MEMBER_ID,
            MemberAccountRecord.ACC_BARS.greaterThan(0)));

        // people who have subscribed simply have the relevant flag set on MemberRecord
        fromRecords(Phase.SUBSCRIBED, _memberRepo.funnelQuery(MemberRecord.MEMBER_ID,
            MemberRecord.FLAGS.bitAnd(MemberRecord.Flag.SUBSCRIBER.getBit()).notEq(0)));

        // expire the funnel next midnight
        _expiration = Calendars.now().zeroTime().addDays(1).toDate();

    }

    protected void fromRecords (Phase phase, Iterable<FunnelEntryRecord> records)
    {
        for (FunnelEntryRecord rec : records) {
            _entries.add(new FunnelBit(phase, rec.vector, rec.date), rec.count);
        }
    }

    protected Multiset<FunnelBit> _entries = HashMultiset.create();
    protected Date _expiration = new Date();

    @Inject protected MemberRepository _memberRepo;

    protected static final int RETURNED_DAYS = 1;
    protected static final int RETAINED_DAYS = 7;
}
