//
// $Id$

package com.threerings.msoy.person.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Represents a named folder belonging to a member.
 */
final public class MailFolder
    implements IsSerializable, Streamable
{
    public static final int INBOX_FOLDER_ID = 1;
    public static final int TRASH_FOLDER_ID = 2;
    public static final int SENT_FOLDER_ID = 3;

    public static final int[] STOCK_FOLDERS = { INBOX_FOLDER_ID, TRASH_FOLDER_ID, SENT_FOLDER_ID };

    /** The id of this folder, unique relative to its member. */
    public int folderId;

    /** The id of the member who owns this folder. */
    public int ownerId;

    /** The name of this folder. */
    public String name;

    /** The number of unread messages in the folder, a computed value. */
    public int unreadCount;

    /** The number of read messages in the folder, a computed value. */
    public int readCount;

    @Override
    public int hashCode ()
    {
        return folderId + 31*ownerId;
    }

    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof MailFolder)) {
            return false;
        }
        MailFolder other = (MailFolder) obj;
        return other.folderId == folderId && other.ownerId == ownerId;
    }
}
