//
// $Id$

package com.threerings.msoy.money.server;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.money.data.MoneyCodes;
import com.threerings.msoy.money.data.all.BlingExchangeResult;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.CashOutEntry;
import com.threerings.msoy.money.data.all.CashOutInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.ExchangeStatusData;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.money.data.all.TransactionPageResult;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.MailSender;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
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

        List<MoneyTransaction> page = _moneyLogic.getTransactions(
            memberId, report.transactions, report.currency, from, count, true, mrec.isSupport());
        int total = _moneyLogic.getTransactionCount(memberId,
            report.transactions, report.currency);
        return new TransactionPageResult(total, page, getBlingInfo(memberId));
    }
    
    public BlingExchangeResult exchangeBlingForBars (int memberId, int blingAmount)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        // TODO: I guess someday we'll allow support+ to exchange for others, but for now
        // the memberId parameter is sorta worthless...
        if (mrec.memberId != memberId) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }
        try {
            return _moneyLogic.exchangeBlingForBars(memberId, blingAmount);
        } catch (NotEnoughMoneyException neme) {
            throw neme.toServiceException();
        }
    }
    
    public BlingInfo requestCashOutBling (int memberId, int blingAmount, String password, 
        CashOutBillingInfo info) throws ServiceException
    {
        // Re-auth the user before allowing them to continue.
        MemberRecord mrec = requireAuthedUser();
        if (mrec.memberId != memberId) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }
        _authenticator.authenticateSession(mrec.accountName, password);
        
        BlingInfo result;
        try {
            result = _moneyLogic.requestCashOutBling(memberId, blingAmount, info);
        } catch (NotEnoughMoneyException neme) {
            throw neme.toServiceException();
        } catch (AlreadyCashedOutException acoe) {
            throw new ServiceException(MoneyCodes.E_ALREADY_CASHED_OUT);
        } catch (BelowMinimumBlingException bmbe) {
            throw new ServiceException(MoneyCodes.E_BELOW_MINIMUM_BLING);
        }

        // Spam the cash out mailing list
        _mailer.sendTemplateEmail(CASHOUT_NOTIFY_EMAIL, "no-reply@whirled.com",
            "blingCashOutNotice", "memberId", mrec.memberId, "name", mrec.name,
            "url", DeploymentConfig.serverURL + "#adminz-cashout"); // TODO: A more meaningful URL

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
        
        // Transform the list into cash out entries.
        return Lists.newArrayList(Iterables.transform(blingMap.entrySet(), 
            new Function<Map.Entry<Integer, CashOutInfo>, CashOutEntry>() {
                public CashOutEntry apply (Entry<Integer, CashOutInfo> entry) {
                    MemberRecord member = names.get(entry.getKey());
                    return new CashOutEntry(entry.getKey(), 
                        member.getName().getNormal(), entry.getValue(), member.accountName);
                }
        }));
    }

    public void supportAdjust (int memberId, Currency currency, int delta)
        throws ServiceException
    {
        MemberRecord mrec = requireSupportUser();

        try {
            // Additional safety checks in MoneyLogic
            _moneyLogic.supportAdjust(memberId, currency, delta, mrec.getName());
        } catch (NotEnoughMoneyException neme) {
            // TODO: use InsufficientFundsException?
            throw new ServiceException(MoneyCodes.E_MONEY_OVERDRAWN);
        }
    }
    
    public void cashOutBling (int memberId, int blingAmount)
        throws ServiceException
    {
        requireSupportUser();
        
        try {
            _moneyLogic.cashOutBling(memberId, blingAmount);
        } catch (NotEnoughMoneyException e) {
            // TODO: use InsufficientFundsException?
            throw new ServiceException(MoneyCodes.E_MONEY_OVERDRAWN);
        }
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

    public ExchangeStatusData getExchangeStatus (int start, int count)
        throws ServiceException
    {
        requireAdminUser();

        return _moneyLogic.getExchangeStatus(start, count);
    }

    /**
     * Not a public service method, Called by getTransactionHistory
     */
    protected BlingInfo getBlingInfo (int memberId)
        throws ServiceException
    {
        return _moneyLogic.getBlingInfo(memberId);
    }

    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyAuthenticator _authenticator;
    @Inject protected MailSender _mailer;

    protected static final String CASHOUT_NOTIFY_EMAIL = "blingcashout@threerings.net";
}
