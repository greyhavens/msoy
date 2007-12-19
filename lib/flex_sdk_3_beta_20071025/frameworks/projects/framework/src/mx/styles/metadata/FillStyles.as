////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

/**
 *  Specifies the alpha transparency values used for the background fill of components.
 *  You should set this to an Array of either two or four numbers.
 *  Elements 0 and 1 specify the start and end values for
 *  an alpha gradient.
 *  If elements 2 and 3 exist, they are used instead of elements 0 and 1
 *  when the component is in a mouse-over state.
 *  The global default value is <code>[ 0.60, 0.40, 0.75, 0.65 ]</code>.
 *  Some components, such as the ApplicationControlBar container,
 *  have a different default value. For the ApplicationControlBar container, 
 *  the default value is <code>[ 0.0, 0.0 ]</code>.
 */
[Style(name="fillAlphas", type="Array", arrayType="Number", inherit="no")]

/**
 *  Specifies the colors used to tint the background fill of the component.
 *  You should set this to an Array of either two or four uint values
 *  that specify RGB colors.
 *  Elements 0 and 1 specify the start and end values for
 *  a color gradient.
 *  If elements 2 and 3 exist, they are used instead of elements 0 and 1
 *  when the component is in a mouse-over state.
 *  For a flat-looking control, set the same color for elements 0 and 1
 *  and for elements 2 and 3,
 *  The default value is
 *  <code>[ 0xFFFFFF, 0xCCCCCC, 0xFFFFFF, 0xEEEEEE ]</code>.
 *  <p>Some components, such as the ApplicationControlBar container,
 *  have a different default value. For the ApplicationControlBar container, 
 *  the default value is <code>[ 0xFFFFFF, 0xFFFFFF ]</code>.</p>
 */
[Style(name="fillColors", type="Array", arrayType="uint", format="Color", inherit="no")]
