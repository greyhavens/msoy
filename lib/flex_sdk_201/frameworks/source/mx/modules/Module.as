////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.modules
{

import mx.core.LayoutContainer;

[Frame(factoryClass="mx.core.FlexModuleFactory")]

/**
 *  The base class for MXML-based dynamically-loadable modules. You extend this
 *  class by using the <code>&lt;mx:Module&gt;</code> tag in an MXML file, as the
 *  following example shows:
 *  
 *  <PRE>
 *  &lt;?xml version="1.0"?&gt;
 *  &lt;!-- This module loads an image. --&gt;
 *  &lt;mx:Module  width="100%" height="100%" xmlns:mx="http://www.adobe.com/2006/mxml"&gt;
 *  
 *    &lt;mx:Image source="trinity.gif"/&gt;
 *  
 *  &lt;/mx:Module&gt;
 *  </PRE>
 *  
 *  <p>If you want to create an ActionScript-based module, extend the ModuleBase class instead.</p>
 *  
 *  @see mx.modules.ModuleBase
 */
public class Module extends LayoutContainer
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
    public function Module()
    {
        super();
    }
}

}
