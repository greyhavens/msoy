package com.threerings.msoy.ui {

import mx.collections.IList;

import mx.controls.List;

import mx.events.ListEvent;

import com.threerings.msoy.client.MsoyContext;

public class List extends mx.controls.List
{
    public function List (ctx :MsoyContext)
    {
        super();
        _ctx = ctx;

        addEventListener(ListEvent.ITEM_CLICK,
            function (event :ListEvent) :void {
                var dp :Object = dataProvider;
                if (dp is IList) {
                    itemClicked((dp as IList).getItemAt(event.rowIndex));

                } else {
                    Log.getLog(com.threerings.msoy.ui.List).warning(
                        "Unknown dataProvider: " + dp);
                }
            });
    }

    /**
     * Get the label to use for the specified list item.
     */
    protected function getLabelFor (obj :Object) :String
    {
        return obj.toString();
    }

    /**
     * Get the icon to use for the specified list item.
     */
    protected function getIconFor (obj :Object) :Class
    {
        return null;
    }

    /**
     * Get the tip to use for the specified list item.
     */
    protected function getTipFor (obj :Object) :String
    {
        return null;
    }

    /**
     * Handle an item being clicked.
     */
    protected function itemClicked (obj :Object) :void
    {
        // nada
    }

    /** The giver of life. */
    protected var _ctx :MsoyContext;
}
}
