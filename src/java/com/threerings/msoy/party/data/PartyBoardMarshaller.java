//
// $Id$

package com.threerings.msoy.party.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.party.client.PartyBoardService;

/**
 * Provides the implementation of the {@link PartyBoardService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from PartyBoardService.java.")
public class PartyBoardMarshaller extends InvocationMarshaller<ClientObject>
    implements PartyBoardService
{
    /**
     * Marshalls results to implementations of {@code PartyBoardService.JoinListener}.
     */
    public static class JoinMarshaller extends ListenerMarshaller
        implements JoinListener
    {
        /** The method id used to dispatch {@link #foundParty}
         * responses. */
        public static final int FOUND_PARTY = 1;

        // from interface JoinMarshaller
        public void foundParty (int arg1, String arg2, int arg3)
        {
            sendResponse(FOUND_PARTY, new Object[] { Integer.valueOf(arg1), arg2, Integer.valueOf(arg3) });
        }

        /** The method id used to dispatch {@link #priceUpdated}
         * responses. */
        public static final int PRICE_UPDATED = 2;

        // from interface JoinMarshaller
        public void priceUpdated (PriceQuote arg1)
        {
            sendResponse(PRICE_UPDATED, new Object[] { arg1 });
        }

        @Override // from InvocationMarshaller
        public void dispatchResponse (int methodId, Object[] args)
        {
            switch (methodId) {
            case FOUND_PARTY:
                ((JoinListener)listener).foundParty(
                    ((Integer)args[0]).intValue(), (String)args[1], ((Integer)args[2]).intValue());
                return;

            case PRICE_UPDATED:
                ((JoinListener)listener).priceUpdated(
                    (PriceQuote)args[0]);
                return;

            default:
                super.dispatchResponse(methodId, args);
                return;
            }
        }
    }

    /** The method id used to dispatch {@link #createParty} requests. */
    public static final int CREATE_PARTY = 1;

    // from interface PartyBoardService
    public void createParty (Currency arg1, int arg2, String arg3, int arg4, boolean arg5, PartyBoardService.JoinListener arg6)
    {
        PartyBoardMarshaller.JoinMarshaller listener6 = new PartyBoardMarshaller.JoinMarshaller();
        listener6.listener = arg6;
        sendRequest(CREATE_PARTY, new Object[] {
            arg1, Integer.valueOf(arg2), arg3, Integer.valueOf(arg4), Boolean.valueOf(arg5), listener6
        });
    }

    /** The method id used to dispatch {@link #getCreateCost} requests. */
    public static final int GET_CREATE_COST = 2;

    // from interface PartyBoardService
    public void getCreateCost (InvocationService.ResultListener arg1)
    {
        InvocationMarshaller.ResultMarshaller listener1 = new InvocationMarshaller.ResultMarshaller();
        listener1.listener = arg1;
        sendRequest(GET_CREATE_COST, new Object[] {
            listener1
        });
    }

    /** The method id used to dispatch {@link #getPartyBoard} requests. */
    public static final int GET_PARTY_BOARD = 3;

    // from interface PartyBoardService
    public void getPartyBoard (byte arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_PARTY_BOARD, new Object[] {
            Byte.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #getPartyDetail} requests. */
    public static final int GET_PARTY_DETAIL = 4;

    // from interface PartyBoardService
    public void getPartyDetail (int arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_PARTY_DETAIL, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #locateParty} requests. */
    public static final int LOCATE_PARTY = 5;

    // from interface PartyBoardService
    public void locateParty (int arg1, PartyBoardService.JoinListener arg2)
    {
        PartyBoardMarshaller.JoinMarshaller listener2 = new PartyBoardMarshaller.JoinMarshaller();
        listener2.listener = arg2;
        sendRequest(LOCATE_PARTY, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }
}
