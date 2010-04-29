package com.threerings.msoy.server;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;
import com.google.common.collect.Multiset.Entry;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.depot.Exps;
import com.samskivert.util.Calendars;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.money.server.persist.MemberAccountRecord;
import com.threerings.msoy.server.persist.EntryVectorRecord;
import com.threerings.msoy.server.persist.FunnelByVectorRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.presents.annotation.BlockingThread;

@Singleton @BlockingThread
public class FunnelByVector implements JSONReporter
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

    @Inject public FunnelByVector ()
    {
    }

    public String buildJSONReport ()
    {
        Multiset<FunnelByVectorBit> newSummary = HashMultiset.create();
        Set<String> vectors = Sets.newHashSet();
        for (Entry<FunnelByVectorBit> entry : getFunnelByVectorSummary().entrySet()) {
            FunnelByVectorBit bit = entry.getElement();
            newSummary.add(new FunnelByVectorBit(bit.phase, bit.vector), entry.getCount());
            vectors.add(bit.vector);
        }

        List<FunnelByVectorOutput> resultBits = Lists.newArrayList();
        for (String vector : vectors) {
            String group = FunnelByDate.summarizeVector(vector);
            FunnelByVectorOutput result = new FunnelByVectorOutput(vector, group);
            result.visited = newSummary.count(new FunnelByVectorBit(Phase.VISITED, vector));
            result.played = newSummary.count(new FunnelByVectorBit(Phase.PLAYED, vector));
            result.registered = newSummary.count(new FunnelByVectorBit(Phase.REGISTERED, vector));
            result.returned = newSummary.count(new FunnelByVectorBit(Phase.RETURNED, vector));
            result.retained = newSummary.count(new FunnelByVectorBit(Phase.RETAINED, vector));
            result.paid = newSummary.count(new FunnelByVectorBit(Phase.PAID, vector));
            result.subscribed = newSummary.count(new FunnelByVectorBit(Phase.SUBSCRIBED, vector));

            resultBits.add(result);
        }
        return new Gson().toJson(ImmutableMap.of("events", resultBits));
    }

    public Multiset<FunnelByVectorBit> getFunnelByVectorSummary ()
    {
        synchronized(_entries) {
            if (new Date().after(_expiration)) {
                createByVectorFunnel();
            }
            return Multisets.unmodifiableMultiset(_entries);
        }
    }

    protected void createByVectorFunnel ()
    {
        _entries.clear();

        // total visitors is just all the recent EntryVectorRecord rows
        fromRecords(Phase.VISITED, null, _memberRepo.funnelByVector(null, null));

        // people who have PLAYED have an entry in MemberRecord, so join against that
        fromRecords(Phase.PLAYED, null,
            _memberRepo.funnelByVector(MemberRecord.MEMBER_ID, null));

        fromRecords(Phase.REGISTERED, null,
            _memberRepo.funnelByVector(MemberRecord.MEMBER_ID, MemberRecord.ACCOUNT_NAME.notLike(
                MemberMailUtil.PERMAGUEST_SQL_PATTERN)));

        // people who RETURNED have a session at least 24 hours after their creation time
        fromRecords(Phase.RETURNED, null, _memberRepo.funnelByVector(
            MemberRecord.MEMBER_ID, MemberRecord.LAST_SESSION.minus(EntryVectorRecord.CREATED)
                .greaterEq(Exps.days(MemberRepository.FUNNEL_RETURNED_DAYS))));

        // people who were RETAINED are REGISTERED and also played at least 7 days after creation
        fromRecords(Phase.RETAINED, Phase.REGISTERED, _memberRepo.funnelByVector(
            MemberRecord.MEMBER_ID, MemberRecord.LAST_SESSION.minus(EntryVectorRecord.CREATED)
                    .greaterEq(Exps.days(MemberRepository.FUNNEL_RETAINED_DAYS))));

        // people who PAID are RETAINED who have also accumulated bars one way or another
        // TODO: make this an actual payment check?
        fromRecords(Phase.PAID, Phase.RETAINED, _memberRepo.funnelByVector(
            MemberAccountRecord.MEMBER_ID, MemberAccountRecord.ACC_BARS.greaterThan(0)));

        // people who have SUBSCRIBED simply have the relevant flag set on MemberRecord
        fromRecords(Phase.SUBSCRIBED, Phase.PAID, _memberRepo.funnelByVector(
            MemberRecord.MEMBER_ID, MemberRecord.FLAGS.bitAnd(
                MemberRecord.Flag.SUBSCRIBER.getBit()).notEq(0)));

        // expire the funnel next midnight
        _expiration = Calendars.now().zeroTime().addDays(1).toDate();
    }

    protected void fromRecords (
        Phase phase, Phase subsetOf, Iterable<FunnelByVectorRecord> records)
    {
        for (FunnelByVectorRecord rec : records) {
            String vector = rec.vector;

            int didx = vector.indexOf("-");
            if (didx >= 0) {
                vector = vector.substring(0, didx+1) + "...";
            }

            if (subsetOf == null ||
                    _entries.contains(new FunnelByVectorBit(subsetOf, vector))) {
                _entries.add(new FunnelByVectorBit(phase, vector), rec.count);
            }
        }
    }

    protected Multiset<FunnelByVectorBit> _entries = HashMultiset.create();
    protected Date _expiration = new Date();

    @Inject protected MemberRepository _memberRepo;

    protected static class FunnelByVectorBit
    {
        public Phase phase;
        public String vector;

        public FunnelByVectorBit (Phase phase, String vector)
        {
            this.phase = phase;
            this.vector = vector;
        }

        @Override
        public int hashCode ()
        {
            return 31*vector.hashCode() + phase.name().hashCode();
        }

        @Override
        public boolean equals (Object other)
        {
            if (other == null || !other.getClass().equals(this.getClass())) {
                return false;
            }
            return ((FunnelByVectorBit) other).vector.equals(vector)
                && ((FunnelByVectorBit) other).phase == phase;
        }
    }

    protected static class FunnelByVectorOutput
    {
        public String vector;
        public String group;

        public int visited;
        public int played;
        public int registered;
        public int returned;
        public int retained;
        public int paid;
        public int subscribed;

        public FunnelByVectorOutput (String vector, String group)
        {
            this.vector = vector;
            this.group = group;
        }
    }

}
