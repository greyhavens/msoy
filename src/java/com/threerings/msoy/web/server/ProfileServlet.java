//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.GroupMembershipRecord;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.UserAction;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.BlurbData;
import com.threerings.msoy.web.data.FriendEntry;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.data.ProfileLayout;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ProfileService}.
 */
public class ProfileServlet extends MsoyServiceServlet
    implements ProfileService
{
    // from interface ProfileService
    public void updateProfile (WebCreds creds, Profile profile)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);

        // TODO: whatever filtering and profanity checking that we want

        // firstly stuff their profile data into the database
        try {
            MsoyServer.profileRepo.storeProfile(new ProfileRecord(memrec.memberId, profile));
            if (memrec.name == null || !memrec.name.equals(profile.displayName)) {
                MsoyServer.memberRepo.configureDisplayName(memrec.memberId, profile.displayName);

                // let the member manager know about the display name change
                final MemberName name = new MemberName(profile.displayName, memrec.memberId);
                MsoyServer.omgr.postRunnable(new Runnable() {
                    public void run () {
                        MsoyServer.memberMan.displayNameChanged(name);
                        // TODO: Figure out grant vs log, and whences comes the flow constant?
                        MsoyServer.memberMan.grantFlow(
                            name.getMemberId(), 3, UserAction.UPDATED_PROFILE, null);
                        MsoyServer.memberMan.logUserAction(name, UserAction.UPDATED_PROFILE, null);
                    }
                });
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to update member's profile " +
                    "[who=" + memrec.who() + "].", pe);
        }
    }

    // from interface ProfileService
    public ArrayList loadProfile (int memberId)
        throws ServiceException
    {
        try {
            MemberRecord memrec = MsoyServer.memberRepo.loadMember(memberId);
            if (memrec == null) {
                return null;
            }

            ProfileLayout layout = loadLayout(memrec);
            ArrayList<Object> data = new ArrayList<Object>();
            data.add(layout);
            for (Object bdata : layout.blurbs) {
                data.add(resolveBlurbData(memrec, (BlurbData)bdata));
            }
            return data;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure resolving blurbs [who=" + memberId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    protected ProfileLayout loadLayout (MemberRecord memrec)
        throws PersistenceException
    {
        // TODO: store this metadata in a database, allow it to be modified
        ProfileLayout layout = new ProfileLayout();
        layout.layout = ProfileLayout.TWO_COLUMN_LAYOUT;

        ArrayList<BlurbData> blurbs = new ArrayList<BlurbData>();
        BlurbData blurb = new BlurbData();
        blurb.type = BlurbData.PROFILE;
        blurb.blurbId = 0;
        blurbs.add(blurb);

        blurb = new BlurbData();
        blurb.type = BlurbData.FRIENDS;
        blurb.blurbId = 1;
        blurbs.add(blurb);

        blurb = new BlurbData();
        blurb.type = BlurbData.GROUPS;
        blurb.blurbId = 2;
        blurbs.add(blurb);

//         blurb = new BlurbData();
//         blurb.type = BlurbData.HOOD;
//         blurb.blurbId = 3;
//         blurbs.add(blurb);

        layout.blurbs = blurbs;
        return layout;
    }

    protected Object resolveBlurbData (MemberRecord memrec, BlurbData bdata)
        throws PersistenceException
    {
        switch (bdata.type) {
        case BlurbData.PROFILE: return resolveProfileData(memrec);
        case BlurbData.FRIENDS: return resolveFriendsData(memrec);
        case BlurbData.GROUPS: return resolveGroupsData(memrec);
        case BlurbData.HOOD: return resolveHoodData(memrec);
        default:
            log.log(Level.WARNING, "Requested to resolve unknown blurb type " + bdata + ".");
            return new BlurbData.ResolutionFailure(MsoyCodes.INTERNAL_ERROR);
        }
    }

    protected Profile resolveProfileData (MemberRecord memrec)
        throws PersistenceException
    {
        ProfileRecord prec = MsoyServer.profileRepo.loadProfile(memrec.memberId);
        Profile profile = (prec == null) ? new Profile() : prec.toProfile();

        // fill in bits from their member record
        profile.memberId = memrec.memberId;
        profile.displayName = memrec.name;
        profile.lastLogon = (memrec.lastSession != null) ? memrec.lastSession.getTime() : 0L;

        return profile;
    }

    protected List<FriendEntry> resolveFriendsData (MemberRecord memrec)
        throws PersistenceException
    {
        return MsoyServer.memberRepo.getFriends(memrec.memberId);
    }

    protected List<GroupMembership> resolveGroupsData (MemberRecord memrec)
        throws PersistenceException
    {
        MemberName name = memrec.getName();
        List<GroupMembership> result = new ArrayList<GroupMembership>();
        for (GroupMembershipRecord gmRec : MsoyServer.groupRepo.getMemberships(memrec.memberId)) {
            GroupRecord gRec = MsoyServer.groupRepo.loadGroup(gmRec.groupId);
            if (gRec == null) {
                log.warning("Unknown group membership [memberId=" + memrec.memberId +
                            ", groupId=" + gmRec.groupId + "]");
                continue;
            }
            result.add(gmRec.toGroupMembership(gRec, name));
        }
        return result;
    }

    protected String resolveHoodData (MemberRecord memrec)
        throws PersistenceException
    {
//         MsoyServer.memberMan.serializeNeighborhood(_memberId, false, new ResultListener<String>() {
//             public void requestCompleted (String hood) {
//                 resolutionCompleted(hood);
//             }
//             public void requestFailed (Exception cause) {
//                 resolutionFailed(cause);
//             }
//         });
        // TODO: do we really want the hood on the profile page?
        return null;
    }
}
