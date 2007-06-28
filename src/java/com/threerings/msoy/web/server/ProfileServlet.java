//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.HashIntMap;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.UserAction;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.GroupMembershipRecord;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.person.server.persist.ProfileRecord;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.BlurbData;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GroupMembership;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.Profile;
import com.threerings.msoy.web.data.ProfileLayout;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ProfileService}.
 */
public class ProfileServlet extends MsoyServiceServlet
    implements ProfileService
{
    // from interface ProfileService
    public void updateProfile (WebIdent ident, String displayName, Profile profile)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(ident);

        // TODO: whatever filtering and profanity checking that we want

        try {
            // load their old profile record for "first time configuration" purposes
            ProfileRecord oprof = MsoyServer.profileRepo.loadProfile(memrec.memberId);

            // stuff their updated profile data into the database
            ProfileRecord nrec = new ProfileRecord(memrec.memberId, profile);
            if (oprof != null) {
                nrec.modifications = oprof.modifications+1;
                nrec.birthday = oprof.birthday;
                nrec.realName = oprof.realName;
            } else {
                log.warning("Account missing old profile [id=" + memrec.memberId + "].");
            }
            MsoyServer.profileRepo.storeProfile(nrec);

            // record that the user updated their profile
            logUserAction(memrec, (nrec.modifications == 1) ?
                          UserAction.CREATED_PROFILE : UserAction.UPDATED_PROFILE, null);

            // handle a display name change if necessary
            if (memrec.name == null || !memrec.name.equals(displayName)) {
                MsoyServer.memberRepo.configureDisplayName(memrec.memberId, displayName);

                // let the member manager know about the display name change
                final MemberName name = new MemberName(displayName, memrec.memberId);
                MsoyServer.omgr.postRunnable(new Runnable() {
                    public void run () {
                        MsoyServer.memberMan.displayNameChanged(name);
                    }
                });
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to update member's profile " +
                    "[who=" + memrec.who() + "].", pe);
        }
    }

    // from interface ProfileService
    public ArrayList loadProfile (WebIdent ident, int memberId)
        throws ServiceException
    {
        MemberRecord memrec = getAuthedUser(ident);

        try {
            MemberRecord tgtrec = MsoyServer.memberRepo.loadMember(memberId);
            if (tgtrec == null) {
                return null;
            }

            ProfileLayout layout = loadLayout(tgtrec);
            ArrayList<Object> data = new ArrayList<Object>();
            data.add(layout);
            data.add(tgtrec.getName());
            for (Object bdata : layout.blurbs) {
                data.add(resolveBlurbData(memrec, tgtrec, (BlurbData)bdata));
            }
            return data;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure resolving blurbs [who=" + memberId + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface ProfileService
    public ArrayList findProfiles (String type, String search)
        throws ServiceException
    {
        try {
            // locate the members that match the supplied search
            HashIntMap<MemberCard> cards = new HashIntMap<MemberCard>();
            if ("email".equals(type)) {
                MemberRecord memrec = MsoyServer.memberRepo.loadMember(search);
                if (memrec != null) {
                    MemberCard card = new MemberCard();
                    card.name = new MemberName(memrec.name, memrec.memberId);
                    cards.put(memrec.memberId, card);
                }

            } else {
                List<MemberNameRecord> names = null;
                if ("display".equals(type)) {
                    names = MsoyServer.memberRepo.findMemberNames(search, MAX_PROFILE_MATCHES);
                } else if ("name".equals(type)) {
                    names = MsoyServer.profileRepo.findMemberNames(search, MAX_PROFILE_MATCHES);
                }
                if (names != null) {
                    for (MemberNameRecord mname : names) {
                        MemberCard card = new MemberCard();
                        card.name = new MemberName(mname.name, mname.memberId);
                        cards.put(mname.memberId, card);
                    }
                }
            }

            // load up their profile data
            resolveCardData(cards);

            ArrayList<Object> results = new ArrayList<Object>();
            results.addAll(cards.values());
            return results;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failure finding profiles [search=" + search + "].", pe);
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

    protected Object resolveBlurbData (MemberRecord reqrec, MemberRecord tgtrec, BlurbData bdata)
        throws PersistenceException
    {
        switch (bdata.type) {
        case BlurbData.PROFILE: return resolveProfileData(reqrec, tgtrec);
        case BlurbData.FRIENDS: return resolveFriendsData(reqrec, tgtrec);
        case BlurbData.GROUPS: return resolveGroupsData(reqrec, tgtrec);
        case BlurbData.HOOD: return resolveHoodData(reqrec, tgtrec);
        default:
            log.log(Level.WARNING, "Requested to resolve unknown blurb type " + bdata + ".");
            return new BlurbData.ResolutionFailure(MsoyCodes.INTERNAL_ERROR);
        }
    }

    protected Profile resolveProfileData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        ProfileRecord prec = MsoyServer.profileRepo.loadProfile(tgtrec.memberId);
        Profile profile = (prec == null) ? new Profile() : prec.toProfile(tgtrec.permaName);

        // TODO: if they're online right now, show that
        profile.lastLogon = (tgtrec.lastSession != null) ? tgtrec.lastSession.getTime() : 0L;

        return profile;
    }

    protected List<MemberCard> resolveFriendsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        HashIntMap<MemberCard> cards = new HashIntMap<MemberCard>();
        for (FriendEntry entry : MsoyServer.memberRepo.getFriends(tgtrec.memberId)) {
            MemberCard card = new MemberCard();
            card.name = entry.name;
            cards.put(entry.name.getMemberId(), card);
        }
        resolveCardData(cards);

        ArrayList<MemberCard> results = new ArrayList<MemberCard>();
        results.addAll(cards.values());
        return results;
    }

    protected List<GroupMembership> resolveGroupsData (MemberRecord reqrec, MemberRecord tgtrec)
        throws PersistenceException
    {
        MemberName name = tgtrec.getName();
        List<GroupMembership> result = new ArrayList<GroupMembership>();
        for (GroupMembershipRecord gmRec : MsoyServer.groupRepo.getMemberships(tgtrec.memberId)) {
            GroupRecord gRec = MsoyServer.groupRepo.loadGroup(gmRec.groupId);
            if (gRec == null) {
                log.warning("Unknown group membership [memberId=" + tgtrec.memberId +
                            ", groupId=" + gmRec.groupId + "]");
                continue;
            }
            result.add(gmRec.toGroupMembership(gRec, name));
        }
        return result;
    }

    protected String resolveHoodData (MemberRecord reqrec, MemberRecord tgtrec)
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

    protected void resolveCardData (HashIntMap<MemberCard> cards)
        throws PersistenceException
    {
        for (ProfileRecord profile : MsoyServer.profileRepo.loadProfiles(
                 cards.intKeySet().toIntArray())) {
            MemberCard card = cards.get(profile.memberId);
            card.photo = profile.getPhoto();
            card.headline = profile.headline;
        }
    }

    protected static final int MAX_PROFILE_MATCHES = 100;
}
