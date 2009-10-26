//
// $Id$

package client.item;

import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.web.gwt.Args;

/**
 * Utility functions for the catalog.
 */
public class ShopUtil
{
    /**
     * Composes arguments to browse the catalog.
     */
    public static Args composeArgs (byte type, String tag, String search, int creatorId)
    {
        CatalogQuery query = new CatalogQuery();
        query.itemType = type;
        query.tag = tag;
        query.search = search;
        query.creatorId = creatorId;
        return composeArgs(query, 0);
    }

    /**
     * Composes arguments to browse the catalog.
     */
    public static Args composeArgs (CatalogQuery query, String tag, String search, int creatorId)
    {
        query.tag = tag;
        query.search = search;
        query.creatorId = creatorId;
        return composeArgs(query, 0);
    }

    /**
     * Composes arguments to browse the catalog.
     */
    public static Args composeArgs (CatalogQuery query, int page)
    {
        Args args = new Args();
        args.add(query.itemType);
        args.add(query.sortBy);
        if (query.tag != null) {
            args.add("t" + query.tag);
        } else if (query.search != null && query.search.length() > 0) {
            args.add("s" + query.search);
        } else if (query.creatorId != 0) {
            args.add("c" + query.creatorId);
        } else {
            args.add("");
        }
        if (query.themeGroupId != 0) {
            args.add("t" + query.themeGroupId);
        }
        if (page > 0) {
            args.add(page);
        }
        return args;
    }

    /**
     * Parses args previously composed via {@link #composeArgs}, populating the supplied
     * {@link CatalogQuery} and returning the page number (defaulting to zero).
     */
    public static int parseArgs (Args args, CatalogQuery query)
    {
        query.itemType = args.get(0,  query.itemType);
        query.sortBy = args.get(1, query.sortBy);
        String action = args.get(2, "");
        if (action.startsWith("s")) {
            query.search = action.substring(1);
        } else if (action.startsWith("t")) {
            query.tag = action.substring(1);
        } else if (action.startsWith("c")) {
            try {
                query.creatorId = Integer.parseInt(action.substring(1));
            } catch (Exception e) {
                // oh well
            }
        }
        // if there is a third argument and it begins with 't', it adds a theme restriction
        // to whatever else may be going on in the query --the page, if any, will be fourth
        int page;
        String third = args.get(3, "");
        if (third.length() > 0 && third.startsWith("t")) {
            try {
                query.themeGroupId = Integer.parseInt(third.substring(1));
            } catch (Exception e) {
                // oh well
            }
            page = args.get(4, 0);
        } else {
            page = args.get(3, 0);
        }
        return page;
    }
}
