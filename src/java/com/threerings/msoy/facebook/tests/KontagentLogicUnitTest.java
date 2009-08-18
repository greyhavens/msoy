//
// $Id$

package com.threerings.msoy.facebook.tests;

import junit.framework.Assert;

import org.junit.Test;

import com.threerings.msoy.facebook.server.KontagentLogic;

public class KontagentLogicUnitTest
{
    @Test public void testMessageUrl ()
    {
        String secrect = "unittest";
        String baseUrl = "x";
        Assert.assertEquals(baseUrl +
            "?ts=0&an_sig=69830813cd30a3b356dca6b1dc51eabd",
            KontagentLogic.buildMessageUrl(baseUrl, "0", secrect));
        Assert.assertEquals(baseUrl +
            "?ts=1&an_sig=880e3bebf583aa569ddef342382d6b9e",
            KontagentLogic.buildMessageUrl(baseUrl, "1", secrect));
        Assert.assertEquals(baseUrl + baseUrl +
            "?ts=0&an_sig=69830813cd30a3b356dca6b1dc51eabd",
            KontagentLogic.buildMessageUrl(baseUrl + baseUrl, "0", secrect));
        Assert.assertEquals(baseUrl +
            "?ts=0&an_sig=69830813cd30a3b356dca6b1dc51eabd",
            KontagentLogic.buildMessageUrl(baseUrl, "0", secrect, "null", null));
        Assert.assertEquals(baseUrl +
            "?ts=0&an_sig=69830813cd30a3b356dca6b1dc51eabd",
            KontagentLogic.buildMessageUrl(baseUrl, "0", secrect, "empty", ""));
        Assert.assertEquals(baseUrl +
            "?a=b&ts=0&an_sig=6d3242c81e3c100d590fedd782831c22",
            KontagentLogic.buildMessageUrl(baseUrl, "0", secrect, "a", "b"));
        Assert.assertEquals(baseUrl +
            "?a=b&ts=0&u=v&an_sig=11ba6565c824d00ab8969205b340d8dc",
            KontagentLogic.buildMessageUrl(baseUrl, "0", secrect, "a", "b", "u", "v"));
        Assert.assertEquals(baseUrl +
            "?a=b&ts=0&u=v&an_sig=11ba6565c824d00ab8969205b340d8dc",
            KontagentLogic.buildMessageUrl(baseUrl, "0", secrect, "u", "v", "a", "b"));
    }
}
