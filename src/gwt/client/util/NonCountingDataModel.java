//
// $Id: ServiceBackedDataModel.java 9 2009-06-30 00:25:24Z jamie $

package client.util;

import com.threerings.gwt.util.ServiceBackedDataModel;

import client.ui.MsoyUI;

/**
 * A version of {@link ServiceBackedDataModel} that does not expect an accurate count from
 * the server, but uses {@link PagedWidget}'s ability to step through pages on demand.
 */
public abstract class NonCountingDataModel<T, R> extends ServiceBackedDataModel<T, R>
{
    @Override // from interface DataModel
    public int getItemCount ()
    {
        return -1;
    }

    /**
     * Lets subclasses know that the result has arrived for the current page. This is handy if the
     * getRows method needs to access a cross-section of the result such as a map of member names.
     */
    @Override
    protected void setCurrentResult (R result)
    {
        super.setCurrentResult(result);
        _count = Math.max(_count, getCount(result));
    }

    @Override // from ServiceBackedDataModel
    protected int getCount (R result)
    {
        return _pageOffset + getRows(result).size();
    }

    @Override // from ServiceBackedDataModel
    protected void reportFailure (Throwable caught)
    {
        MsoyUI.reportServiceFailure(caught);
    }    
}
