////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.utils
{

import flash.xml.XMLDocument;

/**
 *  The XMLUtil class is an all-static class
 *  with methods for working with XML within Flex.
 *  You do not create instances of XMLUtil;
 *  instead you simply call static methods such as
 *  the <code>XMLUtil.qnamesEqual()</code> method.
 */
public class XMLUtil
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class methods
	//
	//--------------------------------------------------------------------------

    /**
     *  Creates XML out of the specified string, ignoring whitespace.
     *  This method is used to create objects defined in
	 *  <code>&lt;mx:XML&gt;</code> tags and WebService requests,
	 *  although they, in turn, get the <code>firstChild</code>
	 *  of the structure.
	 *
     *  @param str XML string.
	 *
     *  @return New XML object that ignored whitespace.
     */
    public static function createXMLDocument(str:String):XMLDocument
    {
	    var xml:XMLDocument = new XMLDocument();
        xml.ignoreWhite = true;
		xml.parseXML(str);
		return xml;
    }

    /**
	 *  Returns <code>true</code> if the two QName parameters have identical
	 *  <code>uri</code> and <code>localName</code> properties.
	 *
	 *  @param qname1 First QName object.
	 *
	 *  @param qname2 Second QName object.
	 *
	 *  @return <code>true</code> if the two QName parameters have identical
	 *  <code>uri</code> and <code>localName</code> properties.
	 */
	public static function qnamesEqual(qname1:QName, qname2:QName):Boolean
    {
        return qname1.uri == qname2.uri &&
			   qname1.localName == qname2.localName;
    }

    /**
	 *  Returns the concatenation of a Qname object's
	 *  <code>uri</code> and <code>localName</code> properties,
	 *  separated by a colon.
	 *  If the object does not have a <code>uri</code> property,
	 *  or the value of <code>uri</code> is the empty string,
	 *  returns the <code>localName</code> property.
	 *
	 *  @param qname QName object.
	 *
	 *  @return Concatenation of a Qname object's
	 *  <code>uri</code> and <code>localName</code> properties,
	 *  separated by a colon.
	 */
    public static function qnameToString(qname:QName):String
    {
        return qname.uri && qname.uri != "" ?
			   qname.uri + ":" + qname.localName :
			   qname.localName;
    }
}

}
