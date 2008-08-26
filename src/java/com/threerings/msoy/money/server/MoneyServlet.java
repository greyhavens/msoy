//
// $Id$

package com.threerings.msoy.money.server;

import java.util.List;

import com.google.inject.Inject;

import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
//import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.data.all.MoneyType;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.server.persist.MoneyRepository;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link MoneyService}.
 */
public class MoneyServlet extends MsoyServiceServlet
    implements MoneyService
{
    public List<MoneyHistory> getTransactionHistory (int memberId)
        throws ServiceException
    {
        // TODO: Access control
        // TODO: Need to be able to pass MoneyType.ALL or null or something
        return _moneyLogic.getLog(memberId, MoneyType.COINS, 0, 10, true);
    }

    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MoneyRepository _moneyRepo;
}
