//
// $Id$

package client.util;

import java.util.List;

import com.threerings.gwt.util.PagedResult;

public abstract class PagedServiceDataModel<T, R extends PagedResult<T>>
    extends ServiceBackedDataModel<T, R>
{
    @Override
    protected int getCount (R result)
    {
        return result.total;
    }

    @Override
    protected List<T> getRows (R result)
    {
        return result.page;
    }
}
