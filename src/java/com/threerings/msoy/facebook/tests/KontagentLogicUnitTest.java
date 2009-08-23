//
// $Id$

package com.threerings.msoy.facebook.tests;

import junit.framework.Assert;

import org.junit.Test;

import com.threerings.msoy.facebook.server.KontagentLogic;
import com.threerings.msoy.facebook.server.KontagentLogic.LinkType;

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

    @Test public void testGenUUID ()
    {
        String uuid1 = KontagentLogic.genUUID(0);
        String uuid2 = KontagentLogic.genUUID(0);
        Assert.assertTrue(!uuid1.equals(uuid2));
        Assert.assertTrue(uuid1.matches("[0-9a-f]{16}"));
        Assert.assertTrue(uuid2.matches("[0-9a-f]{16}"));
    }

    @Test public void testSentLinks ()
    {
        KontagentLogic.SentLink call1, call2;
        for (LinkType type : LinkType.values()) {
            call1 = new KontagentLogic.SentLink(type, "subtype", 0);
            call2 = new KontagentLogic.SentLink(call1.composeTrackingId());
            Assert.assertEquals(call1.type, call2.type);
            Assert.assertEquals(call1.subtype, call2.subtype);
            Assert.assertEquals(call1.uuid, call2.uuid);
        }

        try {
            new KontagentLogic.SentLink("inva");
            Assert.fail("not enough components should throw");
        } catch (IllegalArgumentException ex) { }

        try {
            new KontagentLogic.SentLink("inva-X-Y-0000000000000000");
            Assert.fail("too many components should throw");
        } catch (IllegalArgumentException ex) { }

        try {
            new KontagentLogic.SentLink("not.a.type-0000000000000000");
            Assert.fail("invalid link type should throw");
        } catch (IllegalArgumentException ex) { }

        try {
            new KontagentLogic.SentLink("inva-not.a.uuid");
            Assert.fail("invalid uuid should throw");
        } catch (IllegalArgumentException ex) { }

        try {
            new KontagentLogic.SentLink("inva-xxx-not.a.uuid");
            Assert.fail("invalid uuid type should throw");
        } catch (IllegalArgumentException ex) { }

        new KontagentLogic.SentLink("inva-xxx-0000000000000000");
        new KontagentLogic.SentLink("inva--0000000000000000");
        new KontagentLogic.SentLink("inva-0000000000000000");
    }
}
