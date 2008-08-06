//
// $Id$

package com.threerings.msoy.bureau.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.presents.net.Credentials;
import com.threerings.util.Name;
import com.threerings.util.StringBuilder;

/**
 * Extends the basic credentials to provide window-specific fields.
 */
public class WindowCredentials extends Credentials
{
    /** Prepended to the bureau id to form a username */
    public static const PREFIX :String = "@@bureauwindow:";

    /** Appended to the bureau id to form a username */
    public static const SUFFIX :String = "@@";

    /**
     * Creates new credentials for a specific bureau.
     */
    public function WindowCredentials (bureauId :String, sharedSecret :String)
    {
        super(new Name(PREFIX + bureauId + SUFFIX));
        _sharedSecret = sharedSecret;
    }

    /** @inheritDoc */
    override protected function toStringBuf (buf :StringBuilder) :void
    {
        super.toStringBuf(buf);
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _sharedSecret = ins.readField(String) as String;
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(_sharedSecret);
    }

    protected var _sharedSecret :String;
}
}
