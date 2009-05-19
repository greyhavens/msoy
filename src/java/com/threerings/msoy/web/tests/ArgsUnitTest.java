//
// $Id$

package com.threerings.msoy.web.tests;

import org.junit.Test;

import com.threerings.msoy.web.gwt.Args;

import static org.junit.Assert.*; 

/**
 * Unit tests for {@link Args}.
 */
public class ArgsUnitTest
{
    @Test public void testCompose ()
    {
        assertEquals(COMPOSED, Args.compose(RAW).toToken());
        assertEquals(FB_COMPOSED, Args.compose(FIRST_BLANK).toToken());
    }

    @Test public void testRecompose ()
    {
        Args args = Args.fromToken(COMPOSED);
        assertEquals(COMPOSED, args.recompose(0).toToken());
        assertEquals("c_d_e", args.recompose(2).toToken());
        assertEquals("", args.recompose(99).toToken());
    }

    @Test public void testRecomposeWithout ()
    {
        Args args = Args.fromToken(COMPOSED);
        assertEquals("a_c_d_e", args.recomposeWithout(1, 1).toToken());
        assertEquals("a_b_c", args.recomposeWithout(3, 99).toToken());
        assertEquals("a_b_c_d_e", args.recomposeWithout(99, 1).toToken());
        assertEquals("a_b_c_d_e", args.recomposeWithout(1, 0).toToken());
        assertEquals("", args.recomposeWithout(0, 99).toToken());
    }

    protected static final Object[] RAW = { "a", "b", "c", "d", "e" };
    protected static final String COMPOSED = "a_b_c_d_e";

    protected static final Object[] FIRST_BLANK = { "", "a", "b" };
    protected static final String FB_COMPOSED = "_a_b";
}
