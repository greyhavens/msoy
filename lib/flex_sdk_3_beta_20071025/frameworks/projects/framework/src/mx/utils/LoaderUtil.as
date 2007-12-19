////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2007 Adobe Systems Incorporated.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.utils
{

import flash.display.LoaderInfo;
import flash.system.Security;

	public class LoaderUtil
	{
	/*
	The root url of a cross-domain RSL has some special text appended to the end of the url. 
	This method will normalize the url specified in the specified loaderInfo to removed the appeneded text, if present. 
	Classes accessing LoaderInfo.url should call this method to normalize the url before using it.
	
	@param loaderInfo a LoaderInfo object
	
	@return: a normalized LoaderInfo.url
	*/
	public static function normalizeURL(loaderInfo:LoaderInfo):String
	{
		var url:String = loaderInfo.url;
		var results:Array = url.split("/[[DYNAMIC]]/");
		
		return results[0];
	}

	}
}