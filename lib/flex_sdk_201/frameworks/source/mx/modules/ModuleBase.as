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
import flash.events.EventDispatcher;

[Frame(factoryClass="mx.core.FlexModuleFactory")]

/**
 *  The base class for ActionScript-based dynamically loadable modules.
 *  If you write an ActionScript-only module, you should extend this class.
 *  If you write an MXML-based module by using the <code>&lt;mx:Module&gt;</code> 
 *  tag in an MXML file, you instead extend the Module class.
 *  
 *  @see mx.modules.Module
 */
public class ModuleBase extends EventDispatcher
{
    include "../core/Version.as";
}

}
