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

    // TODO: add all pages that exist in gwt (as needed)
    // TODO: fix all the hardwired page names throughout the code to use this enum
    finishedEnumerating(Page);

    /**
     * Looks up the Page value with a name corresponding to the one given. The name must exactly
     * match the all-caps name of the enum value. To obtain the Page for a lower case "path"
     * value, use <code>fromPath</code>.
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
     * Looks up the Page value with a path equal to the one given. Returns null if no such page is
     * found.
     */
    public static function fromPath (path :String) :Page
    {
        for each (var page :Page in values()) {
            if (page.path == path) {
                return page;
            }
        }
        return null;
    }

    /**
     * Accesses the path of this page. The path is the string that it used to access the gwt
     * module associated with this page. Some paths have a special.
     */
    public function get path () :String
    {
        return _path;
    }

    /** @private */
    public function Page (name :String)
    {
        super(name);

        // TODO: this does not work for all pages, update behavior when necessary
        _path = name.toLowerCase();
    }

    protected var _path :String;
}
}
