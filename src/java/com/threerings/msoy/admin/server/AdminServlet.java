//
// $Id$

package com.threerings.msoy.admin.server;

import static com.threerings.msoy.Log.log;

import java.util.Date;
import java.util.List;

import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntSet;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.server.BureauManager;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.AffiliateMapRecord;
import com.threerings.msoy.server.persist.AffiliateMapRepository;
import com.threerings.msoy.server.persist.MemberInviteStatusRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.server.MsoyServiceServlet;
import com.threerings.msoy.web.server.ServletWaiter;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;

import com.threerings.msoy.mail.server.MailLogic;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.server.MoneyLogic;

import com.threerings.msoy.admin.data.MsoyAdminCodes;
import com.threerings.msoy.admin.gwt.ABTest;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AffiliateMapping;
import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.msoy.admin.gwt.MemberAdminInfo;
import com.threerings.msoy.admin.gwt.MemberInviteResult;
import com.threerings.msoy.admin.gwt.MemberInviteStatus;
import com.threerings.msoy.admin.server.persist.ABTestRecord;
import com.threerings.msoy.admin.server.persist.ABTestRepository;
import com.threerings.presents.dobj.RootDObjectManager;

/**
 * Provides the server implementation of {@link AdminService}.
 */
public class AdminServlet extends MsoyServiceServlet
    implements AdminService
{
    // from interface AdminService
    public void grantInvitations (final int numberInvitations, final int memberId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAdminUser();
        _memberRepo.grantInvites(memberId, numberInvitations);
        sendGotInvitesMail(memrec.memberId, memberId, numberInvitations);
    }

    // from interface AdminService
    public MemberAdminInfo getMemberInfo (final int memberId)
        throws ServiceException
    {
        requireSupportUser();

        final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
        if (tgtrec == null) {
            return null;
        }

        final MemberMoney money = _moneyLogic.getMoneyFor(memberId);
        final MemberAdminInfo info = new MemberAdminInfo();
        info.name = tgtrec.getName();
        info.accountName = tgtrec.accountName;
        info.permaName = tgtrec.permaName;
        if (tgtrec.isSet(MemberRecord.Flag.MAINTAINER)) {
            info.role = WebCreds.Role.MAINTAINER;
        } else if (tgtrec.isSet(MemberRecord.Flag.ADMIN)) {
            info.role = WebCreds.Role.ADMIN;
        } else if (tgtrec.isSet(MemberRecord.Flag.SUPPORT)) {
            info.role = WebCreds.Role.SUPPORT;
        } else {
            info.role = WebCreds.Role.USER;
        }
        info.flow = money.coins;
        info.accFlow = (int)money.accCoins;
        info.gold = money.bars;
        info.sessions = tgtrec.sessions;
        info.sessionMinutes = tgtrec.sessionMinutes;
        if (tgtrec.lastSession != null) {
            info.lastSession = new Date(tgtrec.lastSession.getTime());
        }
        info.humanity = tgtrec.humanity;
        if (tgtrec.affiliateMemberId != 0) {
            // TODO: could be your inviter, but really just your affiliate
            info.inviter = _memberRepo.loadMemberName(tgtrec.affiliateMemberId);
        }
        info.invitees = _memberRepo.loadMembersInvitedBy(memberId);

        return info;
    }

    // from interface AdminService
    public MemberInviteResult getPlayerList (final int inviterId)
        throws ServiceException
    {
        requireSupportUser();

        final MemberInviteResult res = new MemberInviteResult();
        final MemberRecord memRec = inviterId == 0 ? null : _memberRepo.loadMember(inviterId);
        if (memRec != null) {
            res.name = memRec.permaName == null || memRec.permaName.equals("") ?
                memRec.name : memRec.permaName;
            res.memberId = inviterId;
            // TODO: your affiliate is not necessarily your inviter
            res.invitingFriendId = memRec.affiliateMemberId;
        }

        final List<MemberInviteStatus> players = Lists.newArrayList();
        for (final MemberInviteStatusRecord rec : _memberRepo.getMembersInvitedBy(inviterId)) {
            players.add(rec.toWebObject());
        }
        res.invitees = players;
        return res;
    }

    // from interface AdminService
    public void setRole (int memberId, WebCreds.Role role)
        throws ServiceException
    {
        final MemberRecord memrec = requireAdminUser();
        final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
        if (tgtrec == null) {
            return;
        }

        // log this as a warning so that it shows up in the nightly filtered logs
        log.warning("Configuring role", "setter", memrec.who(), "target", tgtrec.who(),
                    "role", role);
        tgtrec.setFlag(MemberRecord.Flag.SUPPORT, role == WebCreds.Role.SUPPORT);
        if (memrec.isMaintainer()) {
            tgtrec.setFlag(MemberRecord.Flag.ADMIN, role == WebCreds.Role.ADMIN);
        }
        if (memrec.isRoot()) {
            tgtrec.setFlag(MemberRecord.Flag.MAINTAINER, role == WebCreds.Role.MAINTAINER);
        }
        _memberRepo.storeFlags(tgtrec);
    }

    // from interface AdminService
    public List<ABTest> getABTests ()
        throws ServiceException
    {
        List<ABTestRecord> records = _testRepo.loadTests();
        final List<ABTest> tests = Lists.newArrayList();
        for (final ABTestRecord record : records) {
            final ABTest test = record.toABTest();
            tests.add(test);
        }
        return tests;
    }

    // from interface AdminService
    public void createTest (final ABTest test)
        throws ServiceException
    {
        // make sure there isn't already a test with this name
        if (_testRepo.loadTestByName(test.name) != null) {
            throw new ServiceException(MsoyAdminCodes.E_AB_TEST_DUPLICATE_NAME);
        }
        _testRepo.insertABTest(test);
    }

    // from interface AdminService
    public void updateTest (final ABTest test)
        throws ServiceException
    {
        // make sure there isn't already a test with this name
        final ABTestRecord existingTest = _testRepo.loadTestByName(test.name);
        if (existingTest != null && existingTest.abTestId != test.abTestId) {
            throw new ServiceException(MsoyAdminCodes.E_AB_TEST_DUPLICATE_NAME);
        }
        _testRepo.updateABTest(test);
    }

    // from interface AdminService
    public PagedResult<AffiliateMapping> getAffiliateMappings (
        int start, int count, boolean needTotal)
        throws ServiceException
    {
        requireSupportUser();

        PagedResult<AffiliateMapping> result = new PagedResult<AffiliateMapping>();
        result.page = Lists.newArrayList(Iterables.transform(
            _affMapRepo.getMappings(start, count), AffiliateMapRecord.TO_MAPPING));
        if (needTotal) {
            result.total = _affMapRepo.getMappingCount();
        }
        return result;
    }

    // from interface AdminService
    public void mapAffiliate (String affiliate, int memberId)
        throws ServiceException
    {
        requireSupportUser();

        _affMapRepo.storeMapping(affiliate, memberId);
        _memberRepo.updateAffiliateMemberId(affiliate, memberId);
    }

    // from interface AdminService
    public List<ItemDetail> getFlaggedItems (final int count)
        throws ServiceException
    {
        requireSupportUser();

        // it'd be nice to round-robin the item types or something, so the first items in the queue
        // aren't always from the same type... perhaps we'll just do something clever in the UI
        final List<ItemDetail> items = Lists.newArrayList();
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
    }

    // from interface AdminService
    public Integer deleteItemAdmin (final ItemIdent iident, final String subject, final String body)
        throws ServiceException
    {
        final MemberRecord memrec = requireSupportUser();

        final byte type = iident.type;
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
        final ItemRecord item = repo.loadOriginalItem(iident.itemId);
        final IntSet owners = new ArrayIntSet();

        int deletionCount = 0;
        owners.add(item.creatorId);

        // we've loaded the original item, if it represents the original listing or a catalog
        // master item, we want to squish the original catalog listing.
        if (item.catalogId != 0) {
            final CatalogRecord catrec = repo.loadListing(item.catalogId, false);
            if (catrec != null && catrec.listedItemId == item.itemId) {
                _itemLogic.removeListing(memrec, type, item.catalogId);
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
            if (ownerId == memrec.memberId) {
                continue; // admin deleting their own item? sure, whatever!
            }
            final MemberRecord owner = _memberRepo.loadMember(ownerId);
            if (owner != null) {
                _mailLogic.startConversation(memrec, owner, subject, body, null);
            }
        }

        return Integer.valueOf(deletionCount);
    }

    // from interface AdminService
    public void refreshBureauLauncherInfo ()
        throws ServiceException
    {
        // Post the request to the event thread and wait for result
        ServletWaiter.queueAndWait(_omgr, "refreshBureauLauncherInfo", new Supplier<Void>() {
            public Void get () {
                _bureauMgr.refreshBureauLauncherInfo();
                return null;
            }
        });
    }
    
    // from interface AdminService
    public BureauLauncherInfo[] getBureauLauncherInfo ()
        throws ServiceException
    {
        // Post the request to the event thread and wait for result
        return ServletWaiter.queueAndWait(
            _omgr, "getBureauLauncherInfo", new Supplier<BureauLauncherInfo[]>() {
                public BureauLauncherInfo[] get () {
                    return _bureauMgr.getBureauLauncherInfo();
                }
            });
    }
    
    protected void sendGotInvitesMail (final int senderId, final int recipientId, final int number)
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
    @Inject protected AffiliateMapRepository _affMapRepo;
    @Inject protected BureauManager _bureauMgr;
    @Inject protected RootDObjectManager _omgr;
}
