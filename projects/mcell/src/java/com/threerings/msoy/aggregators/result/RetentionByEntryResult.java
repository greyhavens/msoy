package com.threerings.msoy.aggregators.result;

import java.util.Map;

import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.common.event.EventData;

public class RetentionByEntryResult extends FieldAggregatedResult
{
    public int converted, retained, total;

    @Override
    protected void doInit (EventData data)
    {
        total = 1;
        converted = data.getInt("conv") == 1 ? 1 : 0;
        retained = data.getInt("ret_weeks") > 0 ? 1 : 0;
    }

    @Override
    public boolean putData (Map<String, Object> result)
    {
        result.put("converted", converted);
        result.put("retained", retained);
        result.put("total", total);
        result.put("converted_p", converted/(double)total);
        result.put("retained_p", retained/(double)total);
        return false;
    }
}
