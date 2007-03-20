//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;

import client.item.ItemEntryPoint;

/**
 * Configures {@link CItem} for item-derived pages.
 */
public abstract class EditemEntryPoint extends ItemEntryPoint
{
    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CEditem.emsgs = (EditemMessages)GWT.create(EditemMessages.class);
    }
}
