//
// $Id$

package com.threerings.msoy.data {

import com.threerings.util.ArrayUtil;
import com.threerings.util.Equalable;
import com.threerings.util.Joiner;

import com.threerings.msoy.utils.Args;

/**
 * Represents the address of a specific page on the whirled web site.
 */
public class Address
    implements Equalable
{
    /** The default me page. */
    public static const ME :Address = new Address(Page.ME, []);

    /**
     * Creates the address of a member's profile page.
     */
    public static function profile (memberId :int) :Address
    {
        return new Address(Page.PEOPLE, [String(memberId)]);
    }

    /**
     * Creates an new address from a path and token. Returns null if the provided path does not
     * match any known path, see <code>Page</code>.
     * @throws ArgumentError if path is null
     */
    public static function fromToken (path :String, token :String) :Address
    {
        if (path == null) {
            throw new ArgumentError("Null path");
        }
        var page :Page = Page.fromPath(path);
        return page == null ? null : new Address(page, Args.split(token));
    }

    /**
     * Creates a new address from a page and an array of arguments.
     */
    public function Address (page :Page, args :Array)
    {
        _page = page;
        _args = args;
    }

    /**
     * Accesses the page value of this address.
     */
    public function get page () :Page
    {
        return _page;
    }

    /**
     * Accesses the array of arguments of this address.
     */
    public function get args () :Array
    {
        return _args;
    }

    /** @inheritDoc */ // from Equalable
    public function equals (obj :Object) :Boolean
    {
        if (!(obj is Address)) {
            return false;
        }

        var a :Address = Address(obj);
        return a._page == _page && ArrayUtil.equals(a._args, _args);
    }

    /** @inheritDoc */ // from Object
    public function toString () :String
    {
        return Joiner.pairs("Address", "page", _page.path, "args", _args.join(","));
    }

    protected var _page :Page;
    protected var _args :Array;
}
}
