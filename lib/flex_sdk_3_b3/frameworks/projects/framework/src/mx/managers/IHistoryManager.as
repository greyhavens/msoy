////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
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
	[Deprecated(since="3.0.0")]
	function registered():void;
	[Deprecated(since="3.0.0")]
	function registerHandshake():void;
	[Deprecated(since="3.0.0")]
	function load(stateVars:Object):void;
	[Deprecated(since="3.0.0")]
	function loadInitialState():void;
}

}

