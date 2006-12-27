//
// $Id$

package client.item;

import client.shell.ShellContext;

import com.threerings.msoy.web.client.CatalogServiceAsync;
import com.threerings.msoy.web.client.ItemServiceAsync;

/**
 * Extends {@link ShellContext} and provides item-specific services.
 */
public class ItemContext extends ShellContext
{
    /** Provides item-related services. */
    public ItemServiceAsync itemsvc;

    /** Provides catalog-related services. */
    public CatalogServiceAsync catalogsvc;

    /** Messages used by the item interfaces. */
    public ItemMessages imsgs;
}
