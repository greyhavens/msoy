//
// $Id$

package com.threerings.msoy.money.server;

import java.util.List;

import com.google.inject.Inject;

import com.threerings.msoy.money.data.MoneyCodes;
import com.threerings.msoy.money.data.all.BlingExchangeResult;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.money.data.all.TransactionPageResult;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.msoy.server.persist.MemberRecord;
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
        // TODO: I guess someday we'll allow support+ to exchange for others
        if (mrec.memberId != memberId) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }
        try {
            return _moneyLogic.exchangeBlingForBars(memberId, blingAmount);
        } catch (NotEnoughMoneyException neme) {
            // TODO: return new balance, brah
            throw new ServiceException(MoneyCodes.E_INSUFFICIENT_BLING);
        }
    }
    
    public void requestCashOutBling (int memberId, int blingAmount)
        throws ServiceException
    {
        try {
            _moneyLogic.requestCashOutBling(memberId, blingAmount);
        } catch (NotEnoughMoneyException neme) {
            throw new ServiceException(MoneyCodes.E_INSUFFICIENT_BLING);
        } catch (AlreadyCashedOutException acoe) {
            throw new ServiceException(MoneyCodes.E_ALREADY_CASHED_OUT);
        } catch (BelowMinimumBlingException bmbe) {
            throw new ServiceException(MoneyCodes.E_BELOW_MINIMUM_BLING);
        }
    }
    
    /**
     * Not a public service method, Called by getTransactionHistory
     */
    protected BlingInfo getBlingInfo (int memberId)
        throws ServiceException
    {
        MemberMoney money = _moneyLogic.getMoneyFor(memberId);
        int blingWorth = _moneyLogic.getBlingWorth(money.bling);
        return new BlingInfo(money.bling, blingWorth);
    }

    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MoneyRepository _moneyRepo;
}
