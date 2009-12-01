//
// $Id$

package com.threerings.msoy.data {

import com.threerings.util.Enum;

/**
 * Enumeration of the "pages" provided by the whirled web site. The "page" terminology is inherited
 * from the java/gwt enumeration "Pages". These actually represent GWT modules, though the mapping
 * is not 1 to 1 since some Pages represent the same module, but a different display mode.
 * <p>NOTE that the enum is called "Pages" in java.</p>
 */
public class Page extends Enum
{
    public static const PEOPLE :Page = new Page("PEOPLE");
    public static const ME :Page = new Page("ME");
    public static const SHOP :Page = new Page("SHOP");
    public static const BILLING :Page = new Page("BILLING");
    public static const ACCOUNT :Page = new Page("ACCOUNT");
    public static const GAMES :Page = new Page("GAMES");

    /** This is a catchall page, may be used as a placeholder. */
    public static const UNKNOWN :Page = new Page("UNKNOWN");

    // TODO: add all pages that exist in gwt (as needed)
    // TODO: fix all the hardwired page names throughout the code to use this enum
    finishedEnumerating(Page);

    /**
     * Looks up the Page value with a name corresponding to the one given. The name must exactly
     * match the all-caps name of the enum value.
     * @throws ArgumentError is no such page is found.
     */
    public static function valueOf (name :String) :Page
    {
        return Enum.valueOf(Page, name) as Page;
    }

    /**
     * Returns an array of all Page instances.
     */
    public static function values () :Array
    {
        return Enum.values(Page);
    }

    /**
     * Attempts to find a Page instance with a name corresponding to the one given. The name must
     * exactly match the all-caps name of the enum value. Returns null if no such page is found.
     */
    public static function findByName (name :String) :Page
    {
        for each (var page :Page in values()) {
            if (name == page.name()) {
                return page;
            }
        }
        return null;
    }

    /**
     * Returns the path of the page. This should only be used when converting to a URL to actually
     * display the page and nowhere else.
     */
    public function get path () :String
    {
        // TODO: this does not work for *all* pages, update behavior when it becomes necessary
        return _name.toLowerCase();
    }

    /** @private */
    public function Page (name :String)
    {
        super(name);
    }
}
}
