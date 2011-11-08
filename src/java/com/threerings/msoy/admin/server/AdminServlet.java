//
// $Id$

package com.threerings.msoy.admin.server;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.whirled.bureau.data.BureauTypes;

import com.samskivert.util.Invoker.Unit;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.samskivert.servlet.util.ServiceWaiter;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager.NodeAction;

import com.threerings.s3.client.S3Connection;
import com.threerings.web.gwt.ServiceException;
import com.threerings.web.server.ServletWaiter;

import com.threerings.msoy.admin.data.MsoyAdminCodes;
import com.threerings.msoy.admin.gwt.ABTest;
import com.threerings.msoy.admin.gwt.ABTestSummary;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.BureauLauncherInfo.BureauInfo;
import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.msoy.admin.gwt.MemberAdminInfo;
import com.threerings.msoy.admin.gwt.StatsModel;
import com.threerings.msoy.admin.server.persist.ABTestRecord;
import com.threerings.msoy.admin.server.persist.ABTestRepository;
import com.threerings.msoy.admin.server.persist.MediaBlacklistRepository;
import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemFlagRecord;
import com.threerings.msoy.item.server.persist.ItemFlagRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.mail.server.MailLogic;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.gwt.BroadcastHistory;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.money.server.persist.BroadcastHistoryRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.room.server.MsoySceneRegistry;
import com.threerings.msoy.server.BureauManager;
import com.threerings.msoy.server.MediaDescFactory;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.CharityRecord;
import com.threerings.msoy.server.persist.ContestRecord;
import com.threerings.msoy.server.persist.ContestRepository;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.PromotionRecord;
import com.threerings.msoy.server.persist.PromotionRepository;
import com.threerings.msoy.server.persist.SubscriptionRepository;
import com.threerings.msoy.underwire.server.SupportLogic;
import com.threerings.msoy.web.gwt.Contest;
import com.threerings.msoy.web.gwt.Promotion;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.server.InvalidationAPI;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link AdminService}.
 */
public class AdminServlet extends MsoyServiceServlet
    implements AdminService
{
    // from interface AdminService
    public MemberAdminInfo getMemberInfo (final int memberId, int affiliateOfCount)
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
        info.validated = tgtrec.isValidated();
        info.permaName = tgtrec.permaName;
        if (tgtrec.isSet(MemberRecord.Flag.MAINTAINER)) {
            info.role = WebCreds.Role.MAINTAINER;
        } else if (tgtrec.isSet(MemberRecord.Flag.ADMIN)) {
            info.role = WebCreds.Role.ADMIN;
        } else if (tgtrec.isSet(MemberRecord.Flag.SUPPORT)) {
            info.role = WebCreds.Role.SUPPORT;
        } else if (tgtrec.isSet(MemberRecord.Flag.SUBSCRIBER) ||
                tgtrec.isSet(MemberRecord.Flag.SUBSCRIBER_PERMANENT)) {
            info.role = WebCreds.Role.SUBSCRIBER;
        } else {
            info.role = WebCreds.Role.REGISTERED;
        }
        info.flow = money.coins;
        info.accFlow = (int)money.accCoins;
        info.gold = money.bars;
        info.sessions = tgtrec.sessions;
        info.sessionMinutes = tgtrec.sessionMinutes;
        if (tgtrec.lastSession != null) {
            info.lastSession = new Date(tgtrec.lastSession.getTime());
        }
        if (tgtrec.affiliateMemberId != 0) {
            info.affiliate = _memberRepo.loadMemberName(tgtrec.affiliateMemberId);
        }
        info.affiliateOfCount = _memberRepo.countMembersAffiliatedTo(memberId);
        info.affiliateOf = _memberRepo.loadMembersAffiliatedTo(memberId, 0, affiliateOfCount);

        // Check if this member is set as a charity.
        CharityRecord charity = _memberRepo.getCharityRecord(memberId);
        if (charity == null) {
            info.charity = false;
            info.coreCharity = false;
            info.charityDescription = "";
        } else {
            info.charity = true;
            info.coreCharity = charity.core;
            info.charityDescription = charity.description;
        }
        return info;
    }

    @Override
    public List<MemberName> getAffiliates (int memberId, int offset, int count)
        throws ServiceException
    {
        requireSupportUser();
        return _memberRepo.loadMembersAffiliatedTo(memberId, offset, count);
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
        boolean isSub = tgtrec.isSet(MemberRecord.Flag.SUBSCRIBER);
        boolean isPermSub = tgtrec.isSet(MemberRecord.Flag.SUBSCRIBER_PERMANENT);
        if (role == WebCreds.Role.SUBSCRIBER) {
            if (!isSub && !isPermSub) {
                log.warning("Making user a permanent subscriber", new Exception());
                tgtrec.setFlag(MemberRecord.Flag.SUBSCRIBER_PERMANENT, true);
                _subscripRepo.noteSubscriptionBilled(tgtrec.memberId, 0);
            }

        } else if ((role.ordinal() < WebCreds.Role.SUBSCRIBER.ordinal()) && (isPermSub || isSub)) {
            log.warning("Stripping user of subscription!", "permanent", isPermSub, new Exception());
            tgtrec.setFlag(
                isPermSub ? MemberRecord.Flag.SUBSCRIBER_PERMANENT : MemberRecord.Flag.SUBSCRIBER,
                false);
            if (isPermSub != isSub) {
                // normal case: end the sub. We only don't if they were PERM and normal!
                _subscripRepo.noteSubscriptionEnded(tgtrec.memberId);
            }
        }
        tgtrec.setFlag(MemberRecord.Flag.SUPPORT, role == WebCreds.Role.SUPPORT);
        if (memrec.isMaintainer()) {
            tgtrec.setFlag(MemberRecord.Flag.ADMIN, role == WebCreds.Role.ADMIN);
        }
        if (memrec.isRoot()) {
            tgtrec.setFlag(MemberRecord.Flag.MAINTAINER, role == WebCreds.Role.MAINTAINER);
        }
        _memberRepo.storeFlags(tgtrec);
        MemberNodeActions.tokensChanged(tgtrec.memberId, tgtrec.toTokenRing());
    }

    // from interface AdminService
    public void setDisplayName (int memberId, String name)
        throws ServiceException
    {
        final MemberRecord memrec = requireSupportUser();
        final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
        if (tgtrec == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        _memberLogic.setDisplayName(tgtrec.memberId, name, tgtrec.isSupport());
        // log this as a warning so that it shows up in the nightly filtered logs
        log.warning("Set display name", "setter", memrec.who(), "target", tgtrec.who(),
                    "name", name);
    }

    // from interface AdminService
    public void setPermaName (int memberId, String name)
        throws ServiceException
    {
        final MemberRecord memrec = requireSupportUser();
        final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
        if (tgtrec == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        tgtrec.permaName = name;
        _memberRepo.update(tgtrec);
        // log this as a warning so that it shows up in the nightly filtered logs
        log.warning("Set perma name", "setter", memrec.who(), "target", tgtrec.who(),
                    "name", name);
    }

    // from interface AdminService
    public void setValidated (int memberId, boolean value)
        throws ServiceException
    {
        requireSupportUser();
        final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
        if (tgtrec == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        tgtrec.setFlag(MemberRecord.Flag.VALIDATED, value);
        _memberRepo.storeFlags(tgtrec);
    }

    // from interface AdminService
    public List<ABTest> getABTests ()
        throws ServiceException
    {
        requireSupportUser();
        return Lists.newArrayList(Iterables.transform(_testRepo.loadTests(), ABTestRecord.TO_TEST));
    }

    // from interface AdminService
    public ABTestSummary getABTestSummary (int testId)
        throws ServiceException
    {
        requireSupportUser();
        return _testLogic.getSummary(testId);
    }

    // from interface AdminService
    public ItemFlagsResult getItemFlags (int start, int count, boolean needCount)
        throws ServiceException
    {
        requireSupportUser();
        ItemFlagsResult result = new ItemFlagsResult();

        // get the total if needed
        if (needCount) {
            result.total = _itemFlagRepo.countItemFlags();
        }

        // get the page of item flags
        result.page = Lists.newArrayList(Iterables.transform(_itemFlagRepo.loadFlags(start, count),
            new Function<ItemFlagRecord, ItemFlag>() {
                public ItemFlag apply (ItemFlagRecord rec) {
                    return rec.toItemFlag();
                }
            }));

        // collect all ids by type that we require
        Multimap<MsoyItemType, Integer> itemsToLoad = HashMultimap.create();
        for (ItemFlag flag : result.page) {
            itemsToLoad.put(flag.itemIdent.type, flag.itemIdent.itemId);
        }

        // load items and stash by ident, also grab the creator id
        result.items = Maps.newHashMap();
        Set<Integer> memberIds = Sets.newHashSet();
        for (Map.Entry<MsoyItemType, Collection<Integer>> ee : itemsToLoad.asMap().entrySet()) {
            for (ItemRecord rec : _itemLogic.getRepository(ee.getKey()).loadItems(ee.getValue())) {
                ItemDetail detail = new ItemDetail();
                detail.item = rec.toItem();
                result.items.put(detail.item.getIdent(), detail);
                memberIds.add(detail.item.creatorId);
            }
        }

        // now add all the reporters
        for (ItemFlag flag : result.page) {
            memberIds.add(flag.memberId);
        }

        // resolve and store names for display
        result.memberNames = Maps.newHashMap();
        result.memberNames.putAll(_memberRepo.loadMemberNames(memberIds));

        // backfill the creator names
        for (ItemDetail detail : result.items.values()) {
            detail.creator = result.memberNames.get(detail.item.creatorId);
        }

        return result;
    }

    // from interface AdminService
    public ItemTransactionResult getItemTransactions (ItemIdent iident, int from, int count)
        throws ServiceException
    {
        requireSupportUser();

        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);
        final ItemRecord item = repo.loadOriginalItem(iident.itemId);

        if (item == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }
        ItemTransactionResult result = new ItemTransactionResult();
        result.page = _moneyLogic.getItemTransactions(iident, from, count, true);

        Set<Integer> memberIds = Sets.newHashSet();
        for (MoneyTransaction tx : result.page) {
            memberIds.add(tx.memberId);
        }
        result.memberNames = Maps.newHashMap();
        result.memberNames.putAll(_memberRepo.loadMemberNames(memberIds));
        return result;
    }

    // from interface AdminService
    public ItemDeletionResult deleteItemAdmin (
        final ItemIdent iident, final String subject, final String body)
        throws ServiceException
    {
        final MemberRecord memrec = requireSupportUser();

        log.info("Deleting item for admin", "who", memrec.accountName, "item", iident,
            "subject", subject);

        final MsoyItemType type = iident.type;
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
        final ItemRecord item = repo.loadOriginalItem(iident.itemId);

        if (item == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        // create a note for storing with the support system later
        StringBuilder note = new StringBuilder();
        appendToNote(note, "Message Body", body);
        appendToNote(note, "Item Record", item);

        final Set<Integer> owners = Sets.newHashSet();

        ItemDeletionResult result = new ItemDeletionResult();
        owners.add(item.creatorId);
        if (item.ownerId != 0) {
            owners.add(item.ownerId);
        }

        // find the catalog record and remove it, load original if any
        ItemRecord original = null;
        if (item.catalogId != 0) {
            CatalogRecord catrec = repo.loadListing(item.catalogId, true);
            if (catrec != null) {
                appendToNote(note, "Catalog Record", catrec);
                if (catrec.listedItemId != item.itemId) {
                    log.warning("Catalog record doesn't match item", "itemId", item.itemId,
                        "listedItemId", catrec.listedItemId);

                } else {
                    result.listings = _itemLogic.removeListing(memrec, type, item.catalogId);
                    if (catrec.originalItemId != item.itemId) {
                        original = repo.loadOriginalItem(catrec.originalItemId);
                        if (original == null) {
                            log.warning("Could not load original item",
                                "id", catrec.originalItemId);
                        } else {
                            appendToNote(note, "Original Item Record", original);
                        }
                    }
                }
            }
        }

        // reclaim the item and all its copies from scenes
        ItemReclaimer reclaimer = new ItemReclaimer(iident, memrec.memberId);
        reclaimer.addItem(item);
        if (original != null) {
            reclaimer.addItem(original);
        }
        for (final CloneRecord record : repo.loadCloneRecords(item.itemId)) {
            reclaimer.addItem(repo.loadItem(record.itemId));
        }
        reclaimer.reclaim();

        // TODO: depending on frequency of errors here, we may want to provide details on errors
        result.reclaimCount += reclaimer.succeeded;
        result.reclaimErrors += reclaimer.failed;

        // TODO: what about memories? tags? comments?
        // TODO: we need some intermediate logic classes. e.g. items will be deleted elsewhere too

        // delete the clones and add each to the reclaimer
        for (final CloneRecord record : repo.loadCloneRecords(item.itemId)) {
            repo.deleteItem(record.itemId);
            result.deletionCount ++;
            owners.add(record.ownerId);
        }

        // finally delete the actual item
        repo.deleteItem(item.itemId);
        result.deletionCount ++;

        // ... and the owner's original
        if (original != null) {
            repo.deleteItem(original.itemId);
            result.deletionCount ++;
        }

        // admin deleting their own item? sure, whatever!
        owners.remove(memrec.memberId);

        // notify the owners of the deletion, ignoring mute lists
        _mailLogic.startBulkConversation(memrec, owners, subject, body, null, false);

        // now do the refunds
        result.refunds += _moneyLogic.refundAllItemPurchases(new ItemIdent(
            type, item.itemId), item.name);

        appendToNote(note, "Result", result);

        // attach a note to the creator's support history
        _supportLogic.addNote(memrec.getName(), item.creatorId, subject,
            note.toString(), item.toItem().getPrimaryMedia().toString());

        return result;
    }

    public void nukeMedia (byte[] hash, byte type, String note)
        throws ServiceException
    {
        final MemberRecord memrec = requireSupportUser();

        HashMediaDesc desc = MediaDescFactory.createMediaDesc(hash, type);

        log.info("Nuking media for admin", "who", memrec.accountName, "media", desc);

        String fileName = HashMediaDesc.hashToString(hash) +
            MediaMimeTypes.mimeTypeToSuffix(type);

        File file = new File(ServerConfig.mediaDir, fileName);
        if (!file.delete()) {
            log.warning("Local media file was not successfully deleted", "file", file);
        }

        if (_blacklistRepo.isBlacklisted(desc)) {
            log.warning("Media was already blacklisted in database", "media", desc);
        } else {
            _blacklistRepo.blacklist(desc, memrec.accountName + ": " + note);
        }

        if (!ServerConfig.mediaS3Enable) {
            return;
        }

        try {
            // delete the media from S3
            S3Connection s3Conn = new S3Connection(ServerConfig.mediaS3Id, ServerConfig.mediaS3Key);
            s3Conn.deleteObject(ServerConfig.mediaS3Bucket, fileName);

            // invalidate it in the cloud
            InvalidationAPI cloudConn = new InvalidationAPI(
                ServerConfig.cloudId, ServerConfig.cloudKey);
            cloudConn.invalidateObjects(
                ServerConfig.cloudDistribution, ImmutableList.of("/" + fileName));

        } catch (Exception e) {
            log.warning("S3/Cloudfront operation failed", "media", desc, e);
            throw new ServiceException(MsoyAdminCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public BureauLauncherInfo[] getBureauLauncherInfo ()
        throws ServiceException
    {
        final ServletWaiter<BureauLauncherInfo[]> waiter =
            new ServletWaiter<BureauLauncherInfo[]>("getBureauLauncherInfo");

        _omgr.postRunnable(new Runnable() {
            public void run () {
                _bureauMgr.getBureauLauncherInfo(waiter);
            }
        });

        BureauLauncherInfo[] infos = waiter.waitForResult();

        Map<String, GameInfoRecord> games = Maps.newHashMap();
        for (BureauLauncherInfo linfo : infos) {
            for (BureauInfo binfo : linfo.bureaus) {
                GameInfoRecord game = games.get(binfo.bureauId);
                if (game == null) {
                    games.put(binfo.bureauId, game = resolveBureauGame(binfo.bureauId));
                }
                if (game != null) {
                    binfo.gameId = game.gameId;
                    binfo.gameName = game.name;
                }
            }
        }
        return infos;
    }

    // from interface AdminService
    public List<Promotion> loadPromotions ()
        throws ServiceException
    {
        requireSupportUser();
        return Lists.newArrayList(
            Lists.transform(_promoRepo.loadPromotions(), PromotionRecord.TO_PROMOTION));
    }

    // from interface AdminService
    public void addPromotion (Promotion promo)
        throws ServiceException
    {
        requireSupportUser();
        _promoRepo.addPromotion(promo);
    }

    public void updatePromotion (Promotion promo)
        throws ServiceException
    {
        requireSupportUser();
        _promoRepo.updatePromotion(promo);
    }

    // from interface AdminService
    public void deletePromotion (String promoId)
        throws ServiceException
    {
        requireSupportUser();
        _promoRepo.deletePromotion(promoId);
    }

    // from interface AdminService
    public List<Contest> loadContests ()
        throws ServiceException
    {
        requireSupportUser();
        return Lists.newArrayList(Lists.transform(_contestRepo.loadContests(),
            ContestRecord.TO_CONTEST));
    }

    // from interface AdminService
    public void addContest (Contest contest)
        throws ServiceException
    {
        requireSupportUser();
        _contestRepo.addContest(contest);
    }

    // from interface AdminService
    public void updateContest (Contest contest)
        throws ServiceException
    {
        requireSupportUser();
        _contestRepo.updateContest(contest);
    }

    // from interface AdminService
    public void deleteContest (String contestId)
        throws ServiceException
    {
        requireSupportUser();
        _contestRepo.deleteContest(contestId);
    }

    // from interface AdminService
    public StatsModel getStatsModel (StatsModel.Type type)
        throws ServiceException
    {
        requireSupportUser();
        try {
            return _adminMgr.compilePeerStatistics(type).get();
        } catch (InterruptedException ie) {
            log.warning("Stats compilation timed out", "type", type, "error", ie);
            throw new ServiceException(MsoyAdminCodes.E_INTERNAL_ERROR);
        } catch (Exception e) {
            log.warning("Stats compilation failed", "type", type, e);
            throw new ServiceException(MsoyAdminCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public void setCharityInfo (CharityInfo charityInfo)
        throws ServiceException
    {
        requireSupportUser();

        // Save or delete charity record depending on value of 'charity.
        CharityRecord charityRec = new CharityRecord(charityInfo.memberId, charityInfo.core,
            charityInfo.description);
        _memberRepo.saveCharity(charityRec);
    }

    // from interface AdminService
    public void removeCharityStatus (int memberId)
        throws ServiceException
    {
        requireSupportUser();

        _memberRepo.deleteCharity(memberId);
    }

    // from interface AdminService
    public void scheduleReboot (int minutes, final String message)
        throws ServiceException
    {
        MemberRecord mrec = requireAdminUser();
        final long time = System.currentTimeMillis() + minutes * 60 * 1000;
        final String initiator = mrec.name + " (" + mrec.memberId + ")";
        _omgr.postRunnable(new Runnable() {
            public void run () {
                // these fields need to be set for the correct logging and email information
                // to get picked up by our admin manager
                _runtimeConfig.server.setServletRebootInitiator(initiator);
                _runtimeConfig.server.setServletReboot(time);
                _runtimeConfig.server.setServletRebootNode(_peerMgr.getNodeObject().nodeName);
                _runtimeConfig.server.setCustomRebootMsg(message);

                // this actually triggers the reboot
                _runtimeConfig.server.setNextReboot(time);
            }
        });
    }

    // from interface AdminService
    public Set<String> getPeerNodeNames ()
        throws ServiceException
    {
        requireSupportUser();

        return Sets.newHashSet(
            Iterables.transform(_peerMgr.getNodeObjects(), new Function<NodeObject, String>() {
            public String apply (NodeObject node) {
                return node.nodeName;
            }
        }));
    }

    // from interface AdminService
    public void restartPanopticon (Set<String> nodeNames)
        throws ServiceException
    {
        requireAdminUser();

        for (String node : nodeNames) {
            _peerMgr.invokeNodeAction(node, new RestartPanopticonAction());
        }
    }

    // from AdminService
    public BroadcastHistoryResult getBroadcastHistory (int offset, int count, boolean needCount)
    {
        BroadcastHistoryResult result = new BroadcastHistoryResult();

        // load count if needed
        if (needCount) {
            result.total = _moneyRepo.countBroadcastHistoryRecords();
        }

        // transform and add results
        result.page = Lists.newArrayList();
        result.page.addAll(Lists.transform(_moneyRepo.getBroadcastHistoryRecords(offset, count),
            new Function<BroadcastHistoryRecord, BroadcastHistory>() {
                public BroadcastHistory apply (BroadcastHistoryRecord rec) {
                    return rec.toBroadcastHistory();
                }
            }));

        // resolve and store names for display
        Set<Integer> memberIds = Sets.newHashSet();
        for (BroadcastHistory bh : result.page) {
            memberIds.add(bh.memberId);
        }
        result.memberNames = Maps.newHashMap();
        result.memberNames.putAll(_memberRepo.loadMemberNames(memberIds));
        return result;
    }

    protected void sendGotInvitesMail (final int senderId, final int recipientId, final int number)
    {
        final String subject = _serverMsgs.getBundle("server").get("m.got_invites_subject", number);
        final String body = _serverMsgs.getBundle("server").get("m.got_invites_body", number);
        _mailRepo.startConversation(recipientId, senderId, subject, body, null, true, true);
    }

    protected GameInfoRecord resolveBureauGame (String bureauId)
    {
        final String prefix = BureauTypes.GAME_BUREAU_ID_PREFIX;
        if (!bureauId.startsWith(prefix)) {
            return null;
        }
        return _mgameRepo.loadGame(Integer.parseInt(bureauId.substring(prefix.length())));
    }

    protected static void appendToNote (StringBuilder note, String objectName, Object object)
    {
        String sep = "\n  ";
        note.append(objectName).append(":").append(sep);
        StringUtil.fieldsToString(note, object, sep);
        note.append("\n");
    }

    protected static class RestartPanopticonAction extends NodeAction
    {
        public RestartPanopticonAction () {}

        @Override
        protected void execute ()
        {
            // Restart the logger on this node and all game servers attached to this node.
            // Restarting is a blocking operation, so run it on the invoker.
            _invoker.postUnit(new Unit() {
                @Override public boolean invoke () {
                    _nodeLogger.restart();
                    return false;
                }
            });
        }

        @Override
        public boolean isApplicable (NodeObject nodeobj)
        {
            // This will automatically go to the node we want.
            return true;
        }

        @Inject protected transient @MainInvoker Invoker _invoker;
        @Inject protected transient MsoyEventLogger _nodeLogger;
    }

    /**
     * Manages the reclamation of all items or item clones from scenes in which they are used. Must
     * be posted to the domgr thread. Provides a means of waiting for all reclamations to finish by
     * extending the waiter.
     */
    protected class ItemReclaimer extends ServiceWaiter<Void>
        implements Runnable
    {
        /** Number of successful reclamations. */
        public int succeeded;

        /** Number of failed reclamations. */
        public int failed;

        /**
         * Creates a new reclaimer.
         */
        public ItemReclaimer (ItemIdent original, int memberId)
        {
            super(60); // one minute timeout... this could take a while
            _memberId = memberId;
            _original = original;
        }

        /**
         * Adds an item to be reclaimed. If the item is not in use, does nothing.
         */
        public void addItem (ItemRecord item)
        {
            // only if it's used in a room
            if ((item.location == 0) || !item.getType().isRoomType()) {
                return;
            }

            ItemIdent ident = new ItemIdent(item.getType(), item.itemId);
            _items.add(Tuple.newTuple(item.location, ident));
        }

        /**
         * Posts all reclamations and waits for them to complete. Throws a service exception if
         * this takes longer than a minute.
         */
        public void reclaim ()
            throws ServiceException
        {
            _omgr.postRunnable(this);
            try {
                waitForResponse();

            } catch (ServiceWaiter.TimeoutException te) {
                log.warning("Timeout occurred while reclaiming items", "remaining", _items.size());
                throw new ServiceException("A timeout occurred while reclaiming items. Please " +
                    "wait a few minutes and try again.");
            }

            // item usage update units are not waited upon, so flush invoker queue
            ServletWaiter.queueAndWait(_invoker, "reclaimFlush", new Callable<Void> () {
                public Void call () {
                    return null;
                }
            });
        }

        // from Runnable
        public void run ()
        {
            log.info("Starting reclamation for item deletion", "original", _original,
                "locations", _items.size());

            for (final Tuple<Integer, ItemIdent> item : Lists.newArrayList(_items)) {
                ResultListener<Void> lner = new ResultListener<Void>() {
                    public void requestCompleted (Void result) {
                        finishedOne(item, true);
                    }

                    public void requestFailed (Exception cause) {
                        finishedOne(item, false);
                    }
                };
                _sceneReg.reclaimItem(item.left, _memberId, item.right, lner);
            }

            checkFinished();
        }

        /**
         * Marks the given item as finished with the given success status. If all items are
         * finished, posts the result to the waiter can stop.
         */
        protected void finishedOne (Tuple<Integer, ItemIdent> item, boolean success)
        {
            if (_items.remove(item)) {
                if (success) {
                    succeeded++;
                } else {
                    failed++;
                }
                checkFinished();
            } else {
                log.warning("Finished reclamation for item deletion", "item", item);
            }
        }

        protected void checkFinished ()
        {
            if (_items.size() == 0) {
                log.info("Finished reclaiming items", "original", _original,
                    "succeeded", succeeded, "failed", failed);
                postSuccess(null);
            }
        }

        protected int _memberId;
        protected ItemIdent _original;
        protected Set<Tuple<Integer, ItemIdent>> _items = Sets.newHashSet();
    }

    // our dependencies
    @Inject @MainInvoker Invoker _invoker;
    @Inject protected ABTestLogic _testLogic;
    @Inject protected ABTestRepository _testRepo;
    @Inject protected BureauManager _bureauMgr;
    @Inject protected ContestRepository _contestRepo;
    @Inject protected ItemFlagRepository _itemFlagRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MailLogic _mailLogic;
    @Inject protected MailRepository _mailRepo;
    @Inject protected MediaBlacklistRepository _blacklistRepo;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MoneyRepository _moneyRepo;
    @Inject protected MsoyAdminManager _adminMgr;
    @Inject protected MsoyEventLogger _eventLogger;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected MsoyPeerManager _peerMgr;
    @Inject protected MsoySceneRegistry _sceneReg;
    @Inject protected PromotionRepository _promoRepo;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected RuntimeConfig _runtimeConfig;
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected SubscriptionRepository _subscripRepo;
    @Inject protected SupportLogic _supportLogic;
}
