//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Photo;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.GroupMembershipRecord;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.client.PersonService;
import com.threerings.msoy.web.data.BlurbData;
import com.threerings.msoy.web.data.FriendEntry;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.web.data.PersonLayout;
import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.data.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link PersonService}.
 */
public class PersonServlet extends MsoyServiceServlet
    implements PersonService
{
    // from interface PersonService
    public ArrayList loadBlurbs (int memberId)
        throws ServiceException
    {
        try {
            MemberRecord memrec = MsoyServer.memberRepo.loadMember(memberId);
            if (memrec == null) {
                return null;
            }

            PersonLayout layout = loadLayout(memrec);
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

    protected PersonLayout loadLayout (MemberRecord memrec)
        throws PersistenceException
    {
        // TODO: store this metadata in a database, allow it to be modified
        PersonLayout layout = new PersonLayout();
        layout.layout = PersonLayout.TWO_COLUMN_LAYOUT;

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
        Profile profile = new Profile();
        profile.memberId = memrec.memberId;
        profile.displayName = memrec.name;
        // profile.lastLogon = ;

        // fake bits!
        profile.photo = new Photo();
        profile.photo.photoMedia = new MediaDesc(
            StringUtil.unhexlate("816cd5aebc2d9d228bf66cff193b81eba1a6ac85"),
            MediaDesc.IMAGE_JPEG);
        profile.headline = "Arr! Mateys, this here be me profile!";
        profile.homePageURL = "http://www.puzzlepirates.com/";
        profile.isMale = true;
        profile.location = "San Francisco, CA";
        profile.age = 36;

//         ProfileRecord prec = _profileRepo.loadProfile(memberId);
//         if (prec != null) {
//             profile.

        // load other bits!
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
