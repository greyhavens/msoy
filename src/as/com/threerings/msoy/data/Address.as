//
// $Id$

package com.threerings.msoy.data {

import com.threerings.util.ArrayUtil;
import com.threerings.util.Equalable;
import com.threerings.util.Joiner;

import com.threerings.msoy.utils.Args;

import com.threerings.msoy.item.data.all.ItemTypes;

/**
 * Represents the address of a specific page on the whirled web site.
 */
public class Address
    implements Equalable
{
    /** The default me page. */
    public static const ME :Address = new Address(Page.ME, []);

    /** The share whirled page. */
    public static const SHARE :Address = new Address(Page.PEOPLE, ["invites", "links"]);

    /** The invite friends page. */
    public static const INVITE :Address = new Address(Page.PEOPLE, ["invites"]);

    /** The contests page. */
    public static const CONTESTS :Address = new Address(Page.ME, ["contests"]);

    /** The contests page. */
    public static const PASSPORT :Address = new Address(Page.ME, ["passport"]);

    /** The furniture shop page. */
    public static const SHOP_FURNI :Address = shopDefault(ItemTypes.FURNITURE);

    /** The billing subscription page. */
    public static const SUBSCRIBE :Address = new Address(Page.BILLING, ["subscribe"]);

    /** My transactions page. */
    public static const TRANSACTIONS :Address = new Address(Page.ME, ["transactions"]);

    /** My transactions page. */
    public static const REGISTER :Address = new Address(Page.ACCOUNT, ["create"]);

    /**
     * Creates the address of a member's profile page.
     */
    public static function profile (memberId :int) :Address
    {
        return new Address(Page.PEOPLE, [String(memberId)]);
    }

    /**
     * Creates the address of a shop page for the supplied item type and with a default sort.
     */
    public static function shopDefault (type :int) :Address
    {
        return new Address(Page.SHOP, [String(type)]);
    }

    /**
     * Creates an new address from a page name and a token. Returns null if the provided name does
     * not match any known Page.name(), see <code>Page</code>.
     * @throws ArgumentError if the name is null
     */
    public static function fromToken (pageName :String, token :String) :Address
    {
        if (pageName == null) {
            throw new ArgumentError("Null page");
        }
        var page :Page = Page.findByName(pageName);
        return new Address(page == null ? Page.UNKNOWN : page, Args.split(token));
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
        return Joiner.pairs("Address", "page", _page, "args", _args.join(","));
    }

    protected var _page :Page;
    protected var _args :Array;
}
}
