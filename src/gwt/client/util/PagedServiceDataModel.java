//
// $Id$

package client.util;

import java.util.List;

import com.threerings.gwt.util.PagedResult;

public abstract class PagedServiceDataModel<T> extends ServiceBackedDataModel<T, PagedResult<T>>
{
    @Override
    protected int getCount (PagedResult<T> result)
    {
        return result.total;
    }

    @Override
    protected List<T> getRows (PagedResult<T> result)
    {
        return result.page;
    }
}
