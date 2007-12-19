////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.managers
{

import flash.net.LocalConnection;
import mx.core.mx_internal;

//-------------------------------------------------------------------------------
//
//  Private class: InitLocalConnection
//
//  This class should live inside HistoryManager.as, but can't at the moment due
//  to a player bug.
//
//-------------------------------------------------------------------------------

[ExcludeClass]

/**
 *  @private
 */
internal class InitLocalConnection extends LocalConnection
{
    include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function InitLocalConnection()
	{
		super();
	}

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	public function loadInitialState():void
	{
		HistoryManager.mx_internal::loadInitialState();
	}
}

}
