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

import mx.managers.IHistoryManagerClient;

[ExcludeClass]

/**
 *  @private
 */
public interface IHistoryManager
{
	function register(obj:IHistoryManagerClient):void;
	function unregister(obj:IHistoryManagerClient):void;
	function save():void;
	function registered():void;
	function registerHandshake():void;
	function load(stateVars:Object):void;
	function loadInitialState():void;
}

}

