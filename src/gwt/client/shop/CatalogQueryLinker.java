//
// $Id$

package client.shop;

import com.threerings.msoy.item.gwt.CatalogQuery;

import client.item.ShopUtil;

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
    public boolean isSelected (byte itemType)
    {
        return (_query.itemType == itemType);
    }

    // from interface SideBar.Linker
    public String composeArgs (byte itemType)
    {
        CatalogQuery copy = new CatalogQuery(_query);
        copy.itemType = itemType;
        return ShopUtil.composeArgs(copy, 0);
    }

    protected CatalogQuery _query;
}
