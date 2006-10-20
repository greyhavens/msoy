//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Represents a named folder belonging to a member.
 */
public class MailFolder
    implements IsSerializable, Streamable
{
    /** The id of this folder, unique relative to this member. */
    public int folderId;

    /** The id of the member who owns this folder. */
    public int memberId;
    
    /** The name of this folder. */
    public String name;
}
