//
// $Id$

package com.threerings.msoy.web.tests;

import java.util.List;

import com.google.common.collect.Lists;

import org.junit.Assert;
import org.junit.Test;

import com.threerings.msoy.web.gwt.SharedNaviUtil;

public class ShareNaviUtilUnitTest
{
    @Test public void testBuildRequestArray ()
    {
        testMethod(new BuildRequestMethod() {
            @Override public String call (String url, String... nameValuePairs) {
                return SharedNaviUtil.buildRequest(url, nameValuePairs);
            }
        });
    }

    @Test public void testBuildRequestList () {
        testMethod(new BuildRequestMethod() {
            @Override public String call (String url, String... nameValuePairs) {
                List<String> args = Lists.newArrayList(nameValuePairs);
                return SharedNaviUtil.buildRequest(url, args);
            }
        });
    }

    protected void testMethod (BuildRequestMethod m)
    {
        String base = "x";
        Assert.assertEquals(base, m.call(base));
        Assert.assertEquals(base + "?a=b", m.call(base, "a", "b"));
        Assert.assertEquals(base + "?a=b&c=d", m.call(base + "?a=b", "c", "d"));
        Assert.assertEquals(base + "?a=b&c=d", m.call(m.call(base, "a", "b"), "c", "d"));
        Assert.assertEquals(base + "?a=b&c=d", m.call(base, "a", "b", "c", "d"));

        try {
            m.call(base, "");
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }

    protected interface BuildRequestMethod
    {
        String call (String url, String... nameValuePairs);
    }
}
