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
import com.samskivert.util.Tuple;
import com.threerings.msoy.data.all.MemberMailUtil;
import com.threerings.msoy.money.server.persist.MemberAccountRecord;
import com.threerings.msoy.server.persist.EntryVectorRecord;
import com.threerings.msoy.server.persist.FunnelByDateRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.presents.annotation.BlockingThread;

@Singleton @BlockingThread
public class FunnelByDate implements JSONReporter
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

    /**
     * This one is reused from {@link FunnelByVector} so it's public.
     */
    public static String summarizeVector (String vector)
    {
        if (vector.startsWith("gpage.landing")) {
            return "GWT/Landing";
        }
        if (vector.equals("page.default") || vector.equals("page.")) {
            return "Web/Broken";
        }
        if (vector.startsWith("page.")) {
            return "Web/Other";
        }
        if (vector.startsWith("gpage.")) {
            return "GWT/Other";
        }
        if (vector.startsWith("e.mochi.")) {
            return "Embed/Mochi";
        }
        if (vector.startsWith("e.kongregate")) {
            return "Embed/Kongregate";
        }
        if (vector.equals("game_session")) {
            return "Embed/?Game";
        }
        if (vector.equals("world_session")) {
            return "Embed/?Room";
        }
        if (vector.startsWith("e.")) {
            return "Embed/Other";
        }
        if (vector.startsWith("a.")) {
            return "Ad/Other";
        }
        return "Other/Other";
    }

    @Inject public FunnelByDate ()
    {
    }

    public String buildJSONReport ()
    {
        Multiset<FunnelByDateBit> newSummary = HashMultiset.create();
        Set<Tuple<String, Date>> keys = Sets.newHashSet();
        for (Entry<FunnelByDateBit> entry : getFunnelByDateSummary().entrySet()) {
            FunnelByDateBit bit = entry.getElement();
            String group = summarizeVector(bit.vector);
            newSummary.add(new FunnelByDateBit(bit.phase, group, bit.date), entry.getCount());
            keys.add(Tuple.newTuple(group, bit.date));
        }

        List<FunnelByDateOutput> resultBits = Lists.newArrayList();
        for (Tuple<String, Date> key : keys) {
            FunnelByDateOutput result = new FunnelByDateOutput(key.left, key.right);
            result.visited = newSummary.count(new FunnelByDateBit(Phase.VISITED, key.left, key.right));
            result.played = newSummary.count(new FunnelByDateBit(Phase.PLAYED, key.left, key.right));
            result.registered = newSummary.count(new FunnelByDateBit(Phase.REGISTERED, key.left, key.right));
            result.returned = newSummary.count(new FunnelByDateBit(Phase.RETURNED, key.left, key.right));
            result.retained = newSummary.count(new FunnelByDateBit(Phase.RETAINED, key.left, key.right));
            result.paid = newSummary.count(new FunnelByDateBit(Phase.PAID, key.left, key.right));
            result.subscribed = newSummary.count(new FunnelByDateBit(Phase.SUBSCRIBED, key.left, key.right));

            resultBits.add(result);
        }
        return new Gson().toJson(ImmutableMap.of("events", resultBits));
    }

    protected Multiset<FunnelByDateBit> getFunnelByDateSummary ()
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
        fromRecords(Phase.VISITED, null, _memberRepo.funnelByDate(null, null));

        // people who have PLAYED have an entry in MemberRecord, so join against that
        fromRecords(Phase.PLAYED, null, _memberRepo.funnelByDate(MemberRecord.MEMBER_ID, null));

        fromRecords(Phase.REGISTERED, null, _memberRepo.funnelByDate(MemberRecord.MEMBER_ID,
            MemberRecord.ACCOUNT_NAME.notLike(MemberMailUtil.PERMAGUEST_SQL_PATTERN)));

        // people who RETURNED have a session at least 24 hours after their creation time
        fromRecords(Phase.RETURNED, null, _memberRepo.funnelByDate(MemberRecord.MEMBER_ID,
            MemberRecord.LAST_SESSION.minus(EntryVectorRecord.CREATED)
                .greaterEq(Exps.days(MemberRepository.FUNNEL_RETURNED_DAYS))));

        // people who were RETAINED are REGISTERED and also played at least 7 days after creation
        fromRecords(Phase.RETAINED, Phase.REGISTERED, _memberRepo.funnelByDate(
            MemberRecord.MEMBER_ID, MemberRecord.LAST_SESSION.minus(EntryVectorRecord.CREATED)
                .greaterEq(Exps.days(MemberRepository.FUNNEL_RETAINED_DAYS))));

        // people who PAID are RETAINED who have also accumulated bars one way or another
        // TODO: make this an actual payment check?
        fromRecords(Phase.PAID, Phase.RETAINED, _memberRepo.funnelByDate(
            MemberAccountRecord.MEMBER_ID, MemberAccountRecord.ACC_BARS.greaterThan(0)));

        // people who have SUBSCRIBED simply have the relevant flag set on MemberRecord
        fromRecords(Phase.SUBSCRIBED, Phase.PAID, _memberRepo.funnelByDate(MemberRecord.MEMBER_ID,
            MemberRecord.FLAGS.bitAnd(MemberRecord.Flag.SUBSCRIBER.getBit()).notEq(0)));

        // expire the funnel next midnight
        _expiration = Calendars.now().zeroTime().addDays(1).toDate();

    }

    protected void fromRecords (
        Phase phase, Phase subsetOf, Iterable<FunnelByDateRecord> records)
    {
        for (FunnelByDateRecord rec : records) {
            if (subsetOf == null ||
                    _entries.contains(new FunnelByDateBit(subsetOf, rec.vector, rec.date))) {
                _entries.add(new FunnelByDateBit(phase, rec.vector, rec.date), rec.count);
            }
        }
    }

    protected Multiset<FunnelByDateBit> _entries = HashMultiset.create();
    protected Date _expiration = new Date();

    @Inject protected MemberRepository _memberRepo;

    protected static class FunnelByDateOutput
    {
        public String group;
        public long date;

        public int visited;
        public int played;
        public int registered;
        public int returned;
        public int retained;
        public int paid;
        public int subscribed;

        public FunnelByDateOutput (String group, Date date)
        {
            this.group = group;
            this.date = date.getTime();
        }
    }

    public static class FunnelByDateBit
    {
        public Phase phase;
        public String vector;
        public Date date;

        public FunnelByDateBit (Phase phase, String vector, Date date)
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
            FunnelByDateBit otherKey = (FunnelByDateBit) other;
            return otherKey.vector.equals(vector) && otherKey.date.equals(date) &&
                otherKey.phase == phase;
        }
    }
}
