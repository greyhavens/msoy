//
// $Id$

package com.threerings.msoy.mail.gwt;

/**
 * Extra data for a game invitation message.
 */
public class GameInvitePayload extends MailPayload
{
    /** Page arguments to accept the invite. */
    public String args;

    /**
     * Default constructor for deserialization.
     */
    public GameInvitePayload ()
    {
    }

    /**
     * Creates a new payload with the given page arguments.
     */
    public GameInvitePayload (String args)
    {
        this.args = args;
    }

    @Override // from MailPayload
    public int getType ()
    {
        return MailPayload.TYPE_GAME_INVITE;
    }
}
