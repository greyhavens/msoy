package com.threerings.msoy.web.server;

import java.text.NumberFormat;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.util.StringUtil;
import com.threerings.presents.server.ReportManager;

/**
 * Singleton profiler for msoy RPC calls, hooks into reporting.
 */
@Singleton
public class RPCProfiler extends MethodProfiler
{
    /**
     * Creates a new rpc profiler.
     */
    @Inject
    public RPCProfiler (ReportManager reportMgr)
    {
        reportMgr.registerReporter("rpc", new ReportManager.Reporter () {
            public void appendReport (
                StringBuilder buffer, long now, long sinceLast, boolean reset) {
                RPCProfiler.this.appendReport(buffer);
                if (reset) {
                    reset();
                }
            }
        });
    }

    /**
     * Appends a report of all methods called and statistics.
     */
    public void appendReport (StringBuilder buffer)
    {
        Map<String, Result> results = getResults();

        buffer.append("* RPC methods\n");
        buffer.append(String.format(HEADER_FMT, "method", "calls", "average(ms)", "deviation(ms)"));
        buffer.append("\n");
        for (Map.Entry<String, Result> entry : results.entrySet()) {
            String method = entry.getKey();
            Result result = entry.getValue();
            buffer.append(String.format(LINE_FMT, StringUtil.truncate(method, 28),
                result.numSamples, result.averageTime, result.standardDeviation));
            buffer.append("\n");
        }
    }

    protected static final String HEADER_FMT = "- %-28s %5s %12s %13s";
    protected static final String LINE_FMT = "- %-28s %5d %12.2f %13.2f";

    protected static final NumberFormat _numberFormat = NumberFormat.getInstance();
    static {
        _numberFormat.setMinimumIntegerDigits(1);
        _numberFormat.setMinimumFractionDigits(2);
        _numberFormat.setMaximumFractionDigits(2);
    }
}
