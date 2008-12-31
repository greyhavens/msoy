//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.data.all.NavItemData;

/**
 * Standard implementation of {@link NavItemData} that uses an integer ID to identify the
 * action the user should take and a string Name that will be displayed directly to the user.
 *
 */
public class BasicNavItemData
    implements NavItemData
{
    public function BasicNavItemData ()
    {
        // used for deserialization
    }

    public function getId () :int
    {
        return _id;
    }

    public function getName () :String
    {
        return _name;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        _id = ins.readInt();
        _name = ins.readField(String) as String;
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error();
    }

    protected var _id :int;
    protected var _name :String;
}
}
