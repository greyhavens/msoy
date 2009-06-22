//
// $Id$

package com.threerings.msoy.data {

import com.threerings.msoy.data.all.NavItemData;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;


public class GwtPageNavItemData implements NavItemData
{
    /** @private */ // we only create this class for deserializing
    public function GwtPageNavItemData ()
    {
    }

    public function getName () :String
    {
        return _name;
    }

    public function getPage () :String
    {
        return _page;
    }

    public function getArgs () :String
    {
        return _args;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        _name = ins.readField(String) as String;
        _page = ins.readField(String) as String;
        _args = ins.readField(String) as String;
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error();
    }

    protected var _name :String;
    protected var _page :String;
    protected var _args :String;
}

}
