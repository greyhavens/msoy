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
        String token = Args.compose(RAW);
        assertEquals(COMPOSED, token);
    }

    @Test public void testRecompose ()
    {
        Args args = Args.fromToken(COMPOSED);
        assertEquals(COMPOSED, args.recompose(0));
        assertEquals("c_d_e", args.recompose(2));
        assertEquals("", args.recompose(99));
    }

    @Test public void testRecomposeWithout ()
    {
        Args args = Args.fromToken(COMPOSED);
        assertEquals("a_c_d_e", args.recomposeWithout(1, 1));
        assertEquals("a_b_c", args.recomposeWithout(3, 99));
        assertEquals("a_b_c_d_e", args.recomposeWithout(99, 1));
        assertEquals("a_b_c_d_e", args.recomposeWithout(1, 0));
        assertEquals("", args.recomposeWithout(0, 99));
    }

    protected static final Object[] RAW = { "a", "b", "c", "d", "e" };
    protected static final String COMPOSED = "a_b_c_d_e";
}
