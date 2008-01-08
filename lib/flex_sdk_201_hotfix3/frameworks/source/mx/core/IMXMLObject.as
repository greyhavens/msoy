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

/**
 *  The IMXMLObject interface defines the APIs that a non-visual component
 *  must implement in order to work properly with the MXML compiler.
 *  Currently, the only supported method is the <code>initialized()</code>
 *  method.
 */
public interface IMXMLObject
{
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

    /**
     *  Called after the implementing object has been created and all
	 *  component properties specified on the MXML tag have been initialized.
	 *
     *  @param document The MXML document that created this object.
	 *
     *  @param id The identifier used by <code>document</code> to refer
	 *  to this object.
	 *  If the object is a deep property on <code>document</code>,
	 *  <code>id</code> is null.
     */
    function initialized(document:Object, id:String):void;
}

}
