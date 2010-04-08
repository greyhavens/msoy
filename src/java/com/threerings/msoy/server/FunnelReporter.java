package com.threerings.msoy.server;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.collect.Multiset.Entry;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.util.Tuple;
import com.threerings.msoy.server.FunnelSummary.FunnelBit;
import com.threerings.msoy.server.FunnelSummary.Phase;
import com.threerings.presents.annotation.BlockingThread;

@Singleton @BlockingThread
public class FunnelReporter
{
    @Inject public FunnelReporter ()
    {
    }

    public String buildVectorSummaryReport ()
    {
        Multiset<FunnelBit> newSummary = HashMultiset.create();
        Set<Tuple<String, Date>> keys = Sets.newHashSet();
        for (Entry<FunnelBit> entry : _summary.getFunnelSummary().entrySet()) {
            FunnelBit bit = entry.getElement();
            String group = summarizeVector(bit.vector);
            newSummary.add(new FunnelBit(bit.phase, group, bit.date), entry.getCount());
            keys.add(Tuple.newTuple(group, bit.date));
        }

        List<FunnelOutput> resultBits = Lists.newArrayList();
        for (Tuple<String, Date> key : keys) {
            FunnelOutput result = new FunnelOutput(key.left, key.right);
            result.visited = newSummary.count(new FunnelBit(Phase.VISITED, key.left, key.right));
            result.played = newSummary.count(new FunnelBit(Phase.PLAYED, key.left, key.right));
            result.registered = newSummary.count(new FunnelBit(Phase.REGISTERED, key.left, key.right));
            result.returned = newSummary.count(new FunnelBit(Phase.RETURNED, key.left, key.right));
            result.retained = newSummary.count(new FunnelBit(Phase.RETAINED, key.left, key.right));
            result.paid = newSummary.count(new FunnelBit(Phase.PAID, key.left, key.right));
            result.subscribed = newSummary.count(new FunnelBit(Phase.SUBSCRIBED, key.left, key.right));

            resultBits.add(result);
        }
        return new Gson().toJson(ImmutableMap.of("events", resultBits));
    }

    protected String summarizeVector (String vector)
    {
        if (vector.equals("gpage.landing")) {
            return "GWT/Landing";
        }
        if (vector.equals("page.default") || vector.equals("page.")) {
            return "Web/Organic";
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
        if (vector.startsWith("e.")) {
            return "Embed/Other";
        }
        if (vector.startsWith("a.")) {
            return "Ad/Other";
        }
        return "Other/Other";
    }

    protected static class FunnelOutput
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

        public FunnelOutput (String group, Date date)
        {
            this.group = group;
            this.date = date.getTime();
        }
    }

    @Inject protected FunnelSummary _summary;
}
