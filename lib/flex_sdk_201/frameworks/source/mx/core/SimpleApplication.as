////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.core
{

import flash.display.MovieClip;

[Frame(factoryClass="mx.core.FlexApplicationBootstrap")]

[ExcludeClass]

/**
 *  @private
 *  SimpleApplication is nothing other than a base class to use when
 *  you need a trivial application bootstrapped by FlexApplicationBootstrap.
 */
public class SimpleApplication extends MovieClip
{
	include "../core/Version.as";
}

}
