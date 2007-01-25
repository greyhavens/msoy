//
// $Id$

package client.item;

import client.shell.CShell;

import com.threerings.msoy.web.client.CatalogServiceAsync;
import com.threerings.msoy.web.client.ItemServiceAsync;

/**
 * Extends {@link CShell} and provides item-specific services.
 */
public class CItem extends CShell
{
    /** Provides item-related services. */
    public static ItemServiceAsync itemsvc;

    /** Provides catalog-related services. */
    public static CatalogServiceAsync catalogsvc;

    /** Messages used by the item interfaces. */
    public static ItemMessages imsgs;
}
