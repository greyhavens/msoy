//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import com.samskivert.util.RunQueue;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.server.ReportManager;

/**
 * Extends the Presents report manager and does some extra fiddly bits for MSOY.
 */
@Singleton @EventThread
public class MsoyReportManager extends ReportManager
{
    public static interface AuxReporter
    {
        public void generateReport (String type, Function<String, Void> receiver);
    }

    @Override // from ReportManager
    public void init (RunQueue rqueue)
    {
        // disable state of the server report logging by not calling super
    }

    /**
     * Registers an auxiliary report source. This reporter will be given a chance to generate a
     * report when the local server's report is generated and all aux reports will be appended to
     * the local report before being provided to the caller.
     */
    public void registerAuxReporter (AuxReporter reporter)
    {
        _auxreps.add(reporter);
    }

    /**
     * Generates the report of the supplied type. This will result in asynchronous calls to obtain
     * reports from our slave servers and thus the result will not be immediately available.
     */
    public void generateReport (String type, Function<String, Void> receiver)
    {
        if (_auxreps.size() == 0) {
            receiver.apply(generateReport(type));
            return;
        }

        ReportCollector collector = new ReportCollector(
            generateReport(type), _auxreps.size(), receiver);
        for (AuxReporter arep : _auxreps) {
            arep.generateReport(type, collector);
        }
    }

    protected static class ReportCollector implements Function<String, Void>
    {
        public ReportCollector (String report, int reporters, Function<String, Void> receiver) {
            _rbuf.append(report).append("\n");
            _reporters = reporters;
            _receiver = receiver;
        }

        public Void apply (String result) {
            _rbuf.append(result);
            if (--_reporters == 0) {
                _receiver.apply(_rbuf.toString());
            }
            return null;
        }

        protected StringBuilder _rbuf = new StringBuilder();
        protected int _reporters;
        protected Function<String, Void> _receiver;
    }

    protected List<AuxReporter> _auxreps = Lists.newArrayList();
}
