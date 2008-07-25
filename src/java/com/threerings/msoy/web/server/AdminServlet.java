//
// $Id$

package com.threerings.msoy.web.server;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.net.MailUtil;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntSet;

import com.threerings.msoy.admin.data.MsoyAdminCodes;
import com.threerings.msoy.admin.server.persist.ABTestRecord;
import com.threerings.msoy.admin.server.persist.ABTestRepository;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.MemberInviteStatusRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.person.server.persist.MailRepository;

import com.threerings.msoy.web.client.AdminService;
import com.threerings.msoy.web.data.ABTest;
import com.threerings.msoy.web.data.MemberAdminInfo;
import com.threerings.msoy.web.data.MemberInviteResult;
import com.threerings.msoy.web.data.MemberInviteStatus;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link AdminService}.
 */
public class AdminServlet extends MsoyServiceServlet
    implements AdminService
{
    // from interface AdminService
    public void grantInvitations (WebIdent ident, int numberInvitations, Date activeSince)
        throws ServiceException
    {
        MemberRecord memrec = requireAdmin(ident);

        try {
            Timestamp since = activeSince != null ? new Timestamp(activeSince.getTime()) : null;
            for (int memberId : _memberRepo.grantInvites(numberInvitations, since)) {
                sendGotInvitesMail(memrec.memberId, memberId, numberInvitations);
            }

        } catch (PersistenceException pe) {
            log.warning("grantInvitations failed [num=" + numberInvitations +
                    ", activeSince=" + activeSince + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public void grantInvitations (WebIdent ident, int numberInvitations, int memberId)
        throws ServiceException
    {
        MemberRecord memrec = requireAdmin(ident);

        try {
            _memberRepo.grantInvites(memberId, numberInvitations);
            sendGotInvitesMail(memrec.memberId, memberId, numberInvitations);

        } catch (PersistenceException pe) {
            log.warning("grantInvitations failed [num=" + numberInvitations +
                ", memberId=" + memberId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public MemberAdminInfo getMemberInfo (WebIdent ident, int memberId)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        if (!memrec.isSupport()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        try {
            MemberRecord tgtrec = _memberRepo.loadMember(memberId);
            if (tgtrec == null) {
                return null;
            }

            MemberAdminInfo info = new MemberAdminInfo();
            info.name = tgtrec.getName();
            info.accountName = tgtrec.accountName;
            info.permaName = tgtrec.permaName;
            info.isSupport = tgtrec.isSupportOnly();
            info.isAdmin = tgtrec.isAdmin();
            info.flow = tgtrec.flow;
            info.accFlow = tgtrec.accFlow;
            // info.gold = TODO: load gold
            info.sessions = tgtrec.sessions;
            info.sessionMinutes = tgtrec.sessionMinutes;
            if (tgtrec.lastSession != null) {
                info.lastSession = new Date(tgtrec.lastSession.getTime());
            }
            info.humanity = tgtrec.humanity;
            if (tgtrec.invitingFriendId != 0) {
                info.inviter = _memberRepo.loadMemberName(tgtrec.invitingFriendId);
            }
            info.invitees = _memberRepo.loadMembersInvitedBy(memberId);

            return info;

        } catch (PersistenceException pe) {
            log.warning("getMemberInfo failed [id=" + memberId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public MemberInviteResult getPlayerList (WebIdent ident, int inviterId)
        throws ServiceException
    {
        requireAdmin(ident);
        MemberInviteResult res = new MemberInviteResult();
        try {
            MemberRecord memRec = inviterId == 0 ? null : _memberRepo.loadMember(inviterId);
            if (memRec != null) {
                res.name = memRec.permaName == null || memRec.permaName.equals("") ?
                    memRec.name : memRec.permaName;
                res.memberId = inviterId;
                res.invitingFriendId = memRec.invitingFriendId;
            }

            List<MemberInviteStatus> players = Lists.newArrayList();
            for (MemberInviteStatusRecord rec : _memberRepo.getMembersInvitedBy(inviterId)) {
                players.add(rec.toWebObject());
            }
            res.invitees = players;

        } catch (PersistenceException pe) {
            log.warning("getPlayerList failed [inviterId=" + inviterId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        return res;
    }

    // from interface AdminService
    public int[] spamPlayers (WebIdent ident, String subject, String body, int startId, int endId)
        throws ServiceException
    {
        MemberRecord memrec = requireAdmin(ident);

        log.info("Spamming the players [spammer=" + memrec.who() + ", subject=" + subject +
                 ", startId=" + startId + ", endId=" + endId + "].");

        // TODO: if we want to continue to use this mechanism to send mass emails to our members,
        // we will need to farm out the mail deliver task to all nodes in the network so that we
        // don't task one node with sending out a million email messages

        // start with member 1 if we weren't given a higher starting id
        startId = Math.max(startId, 1);

        // if we don't have an endId, go all the way
        if (endId <= 0) {
            endId = Integer.MAX_VALUE;
        }

        // we'll track the number of sent, failed and opted out accounts
        int[] results = new int[] { 0, 0, 0 };

        // loop through 100 members at a time and load up their record and send emails
        String from = ServerConfig.getFromAddress();
        int found;
        try {
            do {
                IntSet memIds = new ArrayIntSet();
                for (int ii = 0; ii < MEMBERS_PER_LOOP; ii++) {
                    int memberId = ii + startId;
                    if (memberId > endId) {
                        break;
                    }
                    memIds.add(memberId);
                }
                if (memIds.size() == 0) {
                    break;
                }

                found = 0;
                for (MemberRecord mrec : _memberRepo.loadMembers(memIds)) {
                    found++;

                    if (mrec.isSet(MemberRecord.Flag.NO_ANNOUNCE_EMAIL)) {
                        results[2]++;
                        continue;
                    }

                    try {
                        MailUtil.deliverMail(new String[] { mrec.accountName }, from, subject, body);
                        results[0]++;
                    } catch (Exception e) {
                        results[1]++;
                        log.warning("Failed to spam member [subject=" + subject +
                                    ", email=" + mrec.accountName + ", error=" + e + "].");
                        // roll on through and try the next one
                    }
                }

                startId += MEMBERS_PER_LOOP;
            } while (startId < endId && found > 0);

            return results;

        } catch (PersistenceException pe) {
            log.warning("spamPlayers failed [subject=" + subject +
                ", startId=" + startId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Configures a member as support personnel or not. Only callable by admins.
     */
    public void setIsSupport (WebIdent ident, int memberId, boolean isSupport)
        throws ServiceException
    {
        MemberRecord memrec = requireAdmin(ident);

        try {
            MemberRecord tgtrec = _memberRepo.loadMember(memberId);
            if (tgtrec != null) {
                // log this as a warning so that it shows up in the nightly filtered logs
                log.warning("Configured support flag [setter=" + memrec.who() +
                            ", target=" + tgtrec.who() + ", isSupport=" + isSupport + "].");
                tgtrec.setFlag(MemberRecord.Flag.SUPPORT, isSupport);
                _memberRepo.storeFlags(tgtrec);
            }

        } catch (PersistenceException pe) {
            log.warning("setIsSupport failed [id=" + memberId +
                ", isSupport=" + isSupport + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Fetch a list of A/B test records
     */
    public List<ABTest> getABTests (WebIdent ident)
        throws ServiceException
    {
        List<ABTestRecord> records;
        try {
            records = _testRepo.loadTests();
            List<ABTest> tests = Lists.newArrayList();
            for (ABTestRecord record : records) {
                ABTest test = record.toABTest();
                tests.add(test);
            }
            return tests;
        } catch (PersistenceException pe) {
            log.warning("getABTests failed", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Create a new A/B Test record
     */
    public void createTest (WebIdent ident, ABTest test)
        throws ServiceException
    {
        try {
            // make sure there isn't already a test with this name
            if (_testRepo.loadTestByName(test.name) != null) {
                throw new ServiceException(MsoyAdminCodes.E_AB_TEST_DUPLICATE_NAME);
            }
            _testRepo.insertABTest(test);
        } catch (PersistenceException pe) {
            log.warning("Failed to create test " + test + ".", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Update an existing A/B Test record
     */
    public void updateTest (WebIdent ident, ABTest test)
        throws ServiceException
    {
        try {
            // make sure there isn't already a test with this name
            ABTestRecord existingTest = _testRepo.loadTestByName(test.name);
            if (existingTest != null && existingTest.abTestId != test.abTestId) {
                throw new ServiceException(MsoyAdminCodes.E_AB_TEST_DUPLICATE_NAME);
            }
            _testRepo.updateABTest(test);
        } catch (PersistenceException pe) {
            log.warning("Failed to update test " + test + ".", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    protected MemberRecord requireAdmin (WebIdent ident)
        throws ServiceException
    {
        MemberRecord memrec = _mhelper.requireAuthedUser(ident);
        if (!memrec.isAdmin()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }
        return memrec;
    }

    protected void sendGotInvitesMail (int senderId, int recipientId, int number)
        throws PersistenceException
    {
        String subject = _serverMsgs.getBundle("server").get("m.got_invites_subject", number);
        String body = _serverMsgs.getBundle("server").get("m.got_invites_body", number);
        _mailRepo.startConversation(recipientId, senderId, subject, body, null);
    }

    // our dependencies
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected MailRepository _mailRepo;
    @Inject protected ABTestRepository _testRepo;

    protected static final int MEMBERS_PER_LOOP = 100;
}
