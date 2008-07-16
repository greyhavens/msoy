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
 * TODO: use more than just a special username
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
    public function WindowCredentials (bureauId :String)
    {
        super(new Name(PREFIX + bureauId + SUFFIX));
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
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
    }
}
}
