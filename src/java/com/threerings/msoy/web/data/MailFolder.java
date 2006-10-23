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
    public static final int INBOX_FOLDER_ID = 1;
    public static final int TRASH_FOLDER_ID = 2;
    public static final int SENT_FOLDER_ID = 3;

    /** The id of this folder, unique relative to its member. */
    public int folderId;

    /** The id of the member who owns this folder. */
    public int ownerId;
    
    /** The name of this folder. */
    public String name;
}
