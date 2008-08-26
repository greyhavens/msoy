//
// $Id$

package com.threerings.msoy.admin.server;

import static com.threerings.msoy.Log.log;

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
import com.threerings.msoy.admin.gwt.ABTest;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.MemberAdminInfo;
import com.threerings.msoy.admin.gwt.MemberInviteResult;
import com.threerings.msoy.admin.gwt.MemberInviteStatus;
import com.threerings.msoy.admin.server.persist.ABTestRecord;
import com.threerings.msoy.admin.server.persist.ABTestRepository;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.person.server.MailLogic;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.MemberInviteStatusRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

/**
 * Provides the server implementation of {@link AdminService}.
 */
public class AdminServlet extends MsoyServiceServlet
    implements AdminService
{
    // from interface AdminService
    public void grantInvitations (final int numberInvitations, final Date activeSince)
        throws ServiceException
    {
        final MemberRecord memrec = requireAdmin();

        try {
            final Timestamp since = activeSince != null ? new Timestamp(activeSince.getTime()) : null;
            for (final int memberId : _memberRepo.grantInvites(numberInvitations, since)) {
                sendGotInvitesMail(memrec.memberId, memberId, numberInvitations);
            }

        } catch (final PersistenceException pe) {
            log.warning("grantInvitations failed [num=" + numberInvitations +
                    ", activeSince=" + activeSince + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public void grantInvitations (final int numberInvitations, final int memberId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAdmin();

        try {
            _memberRepo.grantInvites(memberId, numberInvitations);
            sendGotInvitesMail(memrec.memberId, memberId, numberInvitations);

        } catch (final PersistenceException pe) {
            log.warning("grantInvitations failed [num=" + numberInvitations +
                ", memberId=" + memberId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public MemberAdminInfo getMemberInfo (final int memberId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser();
        if (!memrec.isSupport()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        try {
            final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
            if (tgtrec == null) {
                return null;
            }

            final MemberMoney money = _moneyLogic.getMoneyFor(memberId);
            final MemberAdminInfo info = new MemberAdminInfo();
            info.name = tgtrec.getName();
            info.accountName = tgtrec.accountName;
            info.permaName = tgtrec.permaName;
            info.isSupport = tgtrec.isSupportOnly();
            info.isAdmin = tgtrec.isAdmin();
            info.flow = money.getCoins();
            info.accFlow = (int)money.getAccCoins();
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

        } catch (final PersistenceException pe) {
            log.warning("getMemberInfo failed [id=" + memberId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public MemberInviteResult getPlayerList (final int inviterId)
        throws ServiceException
    {
        requireAdmin();
        final MemberInviteResult res = new MemberInviteResult();
        try {
            final MemberRecord memRec = inviterId == 0 ? null : _memberRepo.loadMember(inviterId);
            if (memRec != null) {
                res.name = memRec.permaName == null || memRec.permaName.equals("") ?
                    memRec.name : memRec.permaName;
                res.memberId = inviterId;
                res.invitingFriendId = memRec.invitingFriendId;
            }

            final List<MemberInviteStatus> players = Lists.newArrayList();
            for (final MemberInviteStatusRecord rec : _memberRepo.getMembersInvitedBy(inviterId)) {
                players.add(rec.toWebObject());
            }
            res.invitees = players;

        } catch (final PersistenceException pe) {
            log.warning("getPlayerList failed [inviterId=" + inviterId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        return res;
    }

    // from interface AdminService
    public int[] spamPlayers (final String subject, final String body, int startId, int endId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAdmin();

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
        final int[] results = new int[] { 0, 0, 0 };

        // loop through 100 members at a time and load up their record and send emails
        final String from = ServerConfig.getFromAddress();
        int found;
        try {
            do {
                final IntSet memIds = new ArrayIntSet();
                for (int ii = 0; ii < MEMBERS_PER_LOOP; ii++) {
                    final int memberId = ii + startId;
                    if (memberId > endId) {
                        break;
                    }
                    memIds.add(memberId);
                }
                if (memIds.size() == 0) {
                    break;
                }

                found = 0;
                for (final MemberRecord mrec : _memberRepo.loadMembers(memIds)) {
                    found++;

                    if (mrec.isSet(MemberRecord.Flag.NO_ANNOUNCE_EMAIL)) {
                        results[2]++;
                        continue;
                    }

                    try {
                        MailUtil.deliverMail(
                            new String[] { mrec.accountName }, from, subject, body);
                        results[0]++;
                    } catch (final Exception e) {
                        results[1]++;
                        log.warning("Failed to spam member [subject=" + subject +
                                    ", email=" + mrec.accountName + ", error=" + e + "].");
                        // roll on through and try the next one
                    }
                }

                startId += MEMBERS_PER_LOOP;
            } while (startId < endId && found > 0);

            return results;

        } catch (final PersistenceException pe) {
            log.warning("spamPlayers failed [subject=" + subject +
                ", startId=" + startId + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public void setIsSupport (final int memberId, final boolean isSupport)
        throws ServiceException
    {
        final MemberRecord memrec = requireAdmin();

        try {
            final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
            if (tgtrec != null) {
                // log this as a warning so that it shows up in the nightly filtered logs
                log.warning("Configured support flag [setter=" + memrec.who() +
                            ", target=" + tgtrec.who() + ", isSupport=" + isSupport + "].");
                tgtrec.setFlag(MemberRecord.Flag.SUPPORT, isSupport);
                _memberRepo.storeFlags(tgtrec);
            }

        } catch (final PersistenceException pe) {
            log.warning("setIsSupport failed [id=" + memberId +
                ", isSupport=" + isSupport + "]", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public List<ABTest> getABTests ()
        throws ServiceException
    {
        List<ABTestRecord> records;
        try {
            records = _testRepo.loadTests();
            final List<ABTest> tests = Lists.newArrayList();
            for (final ABTestRecord record : records) {
                final ABTest test = record.toABTest();
                tests.add(test);
            }
            return tests;
        } catch (final PersistenceException pe) {
            log.warning("getABTests failed", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public void createTest (final ABTest test)
        throws ServiceException
    {
        try {
            // make sure there isn't already a test with this name
            if (_testRepo.loadTestByName(test.name) != null) {
                throw new ServiceException(MsoyAdminCodes.E_AB_TEST_DUPLICATE_NAME);
            }
            _testRepo.insertABTest(test);
        } catch (final PersistenceException pe) {
            log.warning("Failed to create test " + test + ".", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public void updateTest (final ABTest test)
        throws ServiceException
    {
        try {
            // make sure there isn't already a test with this name
            final ABTestRecord existingTest = _testRepo.loadTestByName(test.name);
            if (existingTest != null && existingTest.abTestId != test.abTestId) {
                throw new ServiceException(MsoyAdminCodes.E_AB_TEST_DUPLICATE_NAME);
            }
            _testRepo.updateABTest(test);
        } catch (final PersistenceException pe) {
            log.warning("Failed to update test " + test + ".", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public List<ItemDetail> getFlaggedItems (final int count)
        throws ServiceException
    {
        final MemberRecord mRec = requireAuthedUser();

        if (!mRec.isSupport()) {
            throw new ServiceException(ItemCodes.ACCESS_DENIED);
        }
        final List<ItemDetail> items = Lists.newArrayList();
        // it'd be nice to round-robin the item types or something, so the first items in the queue
        // aren't always from the same type... perhaps we'll just do something clever in the UI
        try {
            for (final byte type : _itemLogic.getRepositoryTypes()) {
                final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
                for (final ItemRecord record : repo.loadFlaggedItems(count)) {
                    final Item item = record.toItem();

                    // get auxiliary info and construct an ItemDetail
                    final ItemDetail detail = new ItemDetail();
                    detail.item = item;
                    detail.creator = _memberRepo.loadMemberName(record.creatorId);

                    // add the detail to our result and see if we're done
                    items.add(detail);
                    if (items.size() == count) {
                        return items;
                    }
                }
            }
            return items;

        } catch (final PersistenceException pe) {
            log.warning("Getting flagged items failed.", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public Integer deleteItemAdmin (final ItemIdent iident, final String subject, final String body)
        throws ServiceException
    {
        final MemberRecord admin = requireAuthedUser();
        if (!admin.isSupport()) {
            throw new ServiceException(ItemCodes.ACCESS_DENIED);
        }

        final byte type = iident.type;
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
        try {
            final ItemRecord item = repo.loadOriginalItem(iident.itemId);
            final IntSet owners = new ArrayIntSet();

            int deletionCount = 0;
            owners.add(item.creatorId);

            // we've loaded the original item, if it represents the original listing
            // or a prototype item, we want to squish the original catalog listing.
            if (item.catalogId != 0) {
                final CatalogRecord catrec = repo.loadListing(item.catalogId, false);
                if (catrec != null && catrec.listedItemId == item.itemId) {
                    repo.removeListing(catrec);
                }
            }

            // then delete any potential clones
            for (final CloneRecord record : repo.loadCloneRecords(item.itemId)) {
                repo.deleteItem(record.itemId);
                deletionCount ++;
                owners.add(record.ownerId);
            }

            // finally delete the actual item
            repo.deleteItem(item.itemId);
            deletionCount ++;

            // notify the owners of the deletion
            for (final int ownerId : owners) {
                if (ownerId == admin.memberId) {
                    continue; // admin deleting their own item? sure, whatever!
                }
                final MemberRecord owner = _memberRepo.loadMember(ownerId);
                if (owner != null) {
                    _mailLogic.startConversation(admin, owner, subject, body, null);
                }
            }

            return Integer.valueOf(deletionCount);

        } catch (final PersistenceException pe) {
            log.warning("Admin item delete failed [item=" + iident + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    protected MemberRecord requireAdmin ()
        throws ServiceException
    {
        final MemberRecord memrec = requireAuthedUser();
        if (!memrec.isAdmin()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }
        return memrec;
    }

    protected void sendGotInvitesMail (final int senderId, final int recipientId, final int number)
        throws PersistenceException
    {
        final String subject = _serverMsgs.getBundle("server").get("m.got_invites_subject", number);
        final String body = _serverMsgs.getBundle("server").get("m.got_invites_body", number);
        _mailRepo.startConversation(recipientId, senderId, subject, body, null);
    }

    // our dependencies
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected MailRepository _mailRepo;
    @Inject protected ABTestRepository _testRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MailLogic _mailLogic;
    @Inject protected MoneyLogic _moneyLogic;
    
    protected static final int MEMBERS_PER_LOOP = 100;
}
