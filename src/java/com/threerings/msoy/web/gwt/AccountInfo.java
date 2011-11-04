//
// $Id$

package com.threerings.msoy.web.gwt;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.msoy.data.all.MemberName;

/**
 * Contains all account information not already contained in WebCreds.
 */
public class AccountInfo implements IsSerializable
{
    /** The user's real name.  Used for searching only. */
    public String realName = "";

    /** Whether or not to send email upon receipt of Whirled mail. */
    public boolean emailWhirledMail;

    /** Whether or not to send announcement email. */
    public boolean emailAnnouncements;

    /** ID of the charity member who will receive donations from this account. */
    public int charityMemberId;

    /** List of available charities that can be selected. */
    public List<MemberName> charityNames;

    /** Map of member ID to all the charities. */
    public Map<Integer, CharityInfo> charities;

    /** Map of member ID to charity photos. */
    public Map<Integer, MediaDesc> charityPhotos;

    /** A map of external site connections. */
    public Map<ExternalSiteId, String> externalSites;

    /** Whether to automatically go to your room when you visit Whirled. */
    public boolean autoFlash;

    /**
     * Checks if our external sites include one with the given auther.
     */
    public boolean hasAuther (ExternalSiteId.Auther auther)
    {
        for (ExternalSiteId site : externalSites.keySet()) {
            if (site.auther == auther) {
                return true;
            }
        }
        return false;
    }
}
