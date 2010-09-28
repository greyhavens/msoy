//
// $Id$

package client.shop;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ShopUtil;
import client.item.SideBar;
import client.util.Link;

/**
 * Generates links to catalog pages.
 */
public class CatalogQueryLinker implements SideBar.Linker
{
    public CatalogQueryLinker (CatalogQuery query)
    {
        _query = query;
    }

    // from interface SideBar.Linker
    public boolean isSelected (MsoyItemType itemType)
    {
        return (_query.itemType == itemType);
    }

    // from interface SideBar.Linker
    public Widget createLink (String name, MsoyItemType itemType)
    {
        CatalogQuery copy = new CatalogQuery(_query);
        copy.itemType = itemType;
        return Link.create(name, Pages.SHOP, ShopUtil.composeArgs(copy, 0));
    }

    protected CatalogQuery _query;
}
