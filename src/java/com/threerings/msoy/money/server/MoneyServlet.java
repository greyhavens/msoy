//
// $Id$

package com.threerings.msoy.money.server;

import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.money.data.all.BlingExchangeResult;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.CashOutEntry;
import com.threerings.msoy.money.data.all.CashOutInfo;
import com.threerings.msoy.money.data.all.CharityBlingInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.money.data.all.TransactionPageResult;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.CharityRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.MailSender;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.server.MsoyServiceServlet;

/**
 * Provides the server implementation of {@link MoneyService}.
 */
public class MoneyServlet extends MsoyServiceServlet
    implements MoneyService
{
    public TransactionPageResult getTransactionHistory (
        int memberId, ReportType report, int from, int count)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        if (mrec.memberId != memberId && !mrec.isSupport()) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        // Get transactions and, if support, load reference member names.
        List<MoneyTransaction> page = _moneyLogic.getTransactions(
            memberId, report.transactions, report.currency, from, count, true, mrec.isSupport());

        return new TransactionPageResult(page, getBlingInfo(memberId));
    }

    public BlingExchangeResult exchangeBlingForBars (int memberId, int blingAmount)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        if (mrec.memberId != memberId) {
            // I guess someday we'll allow support+ to exchange for others, but for now
            // the memberId parameter is sorta worthless...
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }
        return _moneyLogic.exchangeBlingForBars(memberId, blingAmount);
    }

    public BlingInfo requestCashOutBling (
        int memberId, int blingAmount, String password, CashOutBillingInfo info)
        throws ServiceException
    {
        // Re-auth the user before allowing them to continue.
        MemberRecord mrec = requireAuthedUser();
        if (mrec.memberId != memberId) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }
        _authenticator.authenticateSession(mrec.accountName, password);

        BlingInfo result = _moneyLogic.requestCashOutBling(memberId, blingAmount, info);

        // Spam the cash out mailing list
        _mailer.sendTemplateEmail(
            MailSender.By.HUMAN, ServerConfig.getCashOutAddress(), "no-reply@whirled.com",
            "blingCashOutNotice", "memberId", mrec.memberId, "name", mrec.name,
            "server_url", DeploymentConfig.serverURL,
            "url", Pages.ADMINZ.makeURL("cashout")); // TODO: A more meaningful URL

        return result;
    }

    public List<CashOutEntry> getBlingCashOutRequests ()
        throws ServiceException
    {
        Map<Integer, CashOutInfo> blingMap = _moneyLogic.getBlingCashOutRequests();

        // Get all member names for the members in the map.
        final Map<Integer, MemberRecord> names = Maps.uniqueIndex(
            _memberRepo.loadMembers(blingMap.keySet()), new Function<MemberRecord, Integer>() {
                public Integer apply (MemberRecord record) {
                    return record.memberId;
                }
            });

        // Get list of charities
        final List<CharityRecord> charities = _memberRepo.getCharities();

        // Transform the list into cash out entries.
        return Lists.newArrayList(Iterables.transform(blingMap.entrySet(),
            new Function<Map.Entry<Integer, CashOutInfo>, CashOutEntry>() {
                public CashOutEntry apply (Entry<Integer, CashOutInfo> entry) {
                    MemberRecord member = names.get(entry.getKey());
                    return new CashOutEntry(
                        entry.getKey(), member.getName().getNormal(), entry.getValue(),
                        member.accountName, isCharity(member.memberId, charities));
                }
        }));
    }

    public List<CharityBlingInfo> getCharityBlingInfo ()
    {
        // Get all charity members.
        List<CharityRecord> charities = _memberRepo.getCharities();
        Set<Integer> memberIds = Sets.newHashSet();
        for (CharityRecord charity : charities) {
            memberIds.add(charity.memberId);
        }
        Map<Integer, MemberRecord> memberMap = Maps.newHashMap();
        for (MemberRecord member : _memberRepo.loadMembers(memberIds)) {
            memberMap.put(member.memberId, member);
        }

        // Get money info for all the members.
        Map<Integer, MemberMoney> monies = _moneyLogic.getMoneyFor(memberIds);

        // Create CharityBlingInfo objects from this information.
        List<CharityBlingInfo> charityBlingInfos =
            Lists.newArrayListWithExpectedSize(charities.size());
        for (CharityRecord charity : charities) {
            MemberRecord member = memberMap.get(charity.memberId);
            MemberMoney money = monies.get(charity.memberId);
            charityBlingInfos.add(new CharityBlingInfo(charity.memberId,
                member.getName().getNormal(), member.accountName, money.bling,
                _runtime.money.blingWorth * money.bling / 100, charity.core));
        }
        return charityBlingInfos;
    }

    public void supportAdjust (int memberId, Currency currency, int delta)
        throws ServiceException
    {
        // support can modify coins, but only admin can modify other currencies
        MemberRecord mrec = (currency == Currency.COINS) ?
            requireSupportUser() : requireAdminUser();
        // additional safety checks in MoneyLogic
        _moneyLogic.supportAdjust(memberId, currency, delta, mrec.getName());
    }

    public void cashOutBling (int memberId, int blingAmount)
        throws ServiceException
    {
        requireSupportUser();
        _moneyLogic.cashOutBling(memberId, blingAmount);
    }

    public void charityCashOutBling (int memberId, int blingAmount)
        throws ServiceException
    {
        requireSupportUser();

        // Need to create a cash out request and fulfill it immediately.  Most of the billing
        // info will be blank, since we will have it on file.  This may change in the future.
        CashOutBillingInfo cashOutInfo = new CashOutBillingInfo("", "", "", "", "", "", "", "", "");
        _moneyLogic.requestCashOutBling(memberId, blingAmount/100, cashOutInfo);
        _moneyLogic.cashOutBling(memberId, blingAmount);
    }

    public void cancelCashOut (int memberId, String reason)
        throws ServiceException
    {
        // Normal user or support user can cancel
        MemberRecord authedUser = getAuthedUser();
        if (memberId != authedUser.memberId) {
            requireSupportUser();
        }

        _moneyLogic.cancelCashOutBling(memberId, reason);
    }

    /**
     * Not a public service method, Called by getTransactionHistory
     */
    protected BlingInfo getBlingInfo (int memberId)
        throws ServiceException
    {
        return _moneyLogic.getBlingInfo(memberId);
    }

    protected boolean isCharity (int memberId, List<CharityRecord> charities)
    {
        for (CharityRecord charity : charities) {
            if (charity.memberId == memberId) {
                return true;
            }
        }
        return false;
    }

    @Inject protected MailSender _mailer;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoyAuthenticator _authenticator;
    @Inject protected RuntimeConfig _runtime;
}
