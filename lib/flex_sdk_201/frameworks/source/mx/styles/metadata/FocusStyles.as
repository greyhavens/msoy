////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

/**
 *  Specifies the alpha transparency value of the focus skin.
 *  
 *  @default 0.4
 */
[Style(name="focusAlpha", type="Number", inherit="no")]

/**
 *  Specifies which corners of the focus rectangle should be rounded.
 *  This value is a space-separated String that can contain any
 *  combination of <code>"tl"</code>, <code>"tr"</code>, <code>"bl"</code>
 *  and <code>"br"</code>.
 *  For example, to specify that the right side corners should be rounded,
 *  but the left side corners should be square, use <code>"tr br"</code>.
 *  The <code>cornerRadius</code> style property specifies
 *  the radius of the rounded corners.
 *  The default value depends on the component class; if not overridden for
 *  the class, default value is <code>"tl tr bl br"</code>.
 */
[Style(name="focusRoundedCorners", type="String", inherit="no")]
