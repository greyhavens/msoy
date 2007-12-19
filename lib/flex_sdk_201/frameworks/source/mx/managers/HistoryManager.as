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

import mx.core.Singleton;
import mx.core.mx_internal;
import mx.managers.IHistoryManagerClient;

use namespace mx_internal;

/**
 *  History management lets users navigate through a Flex application
 *  using the web browser's Back and Forward navigation commands. 
 *  
 *  <p>History management is enabled by default for the Accordion and TabNavigator containers. 
 *  This means that if the user selects one of the panes in an Accordion control, 
 *  that user can return to the previous pane by using the browser's Back button or back 
 *  navigation command. History management is disabled by default for the ViewStack 
 *  navigator container.</p>
 *  
 *  <p>You can disable history management by setting the navigator container's 
 *  <code>historyManagementEnabled</code> property to <code>false</code>.</p>
 *  
 *  <p>You can also enable history management for other objects
 *  in an application by registering the objects with the HistoryManager. To register a component 
 *  with the HistoryManager class, you call the HistoryManager class's <code>register()</code> 
 *  method with a reference to a component instance that implements the IHistoryManagerClient interface.
 *  In the following example, the Application component (<code>this</code>) is registered with 
 *  the HistoryManager class when the Application is initialized:
 *  <pre>
 *  &lt;mx:Application xmlns:mx="http://www.adobe.com/2006/mxml" 
 *    implements="mx.managers.IHistoryManagerClient" 
 *    initialize="mx.managers.HistoryManager.register(this);"&gt;
 *  </pre>
 *  You must also implement the <code>saveState()</code> and <code>loadState()</code> methods of the 
 *  IHistoryManagerClient interface to complete the registration of the component. Components that extend 
 *  UIComponent automatically inherit the <code>loadState()</code> method.</p>
 *  
 *  <p>All methods and properties of the HistoryManager are static,
 *  so you do not need to create an instance of it.</p>
 *
 *  @see mx.managers.IHistoryManagerClient
 */
public class HistoryManager
{
    include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class variables
	//
	//--------------------------------------------------------------------------

    /**
     *  @private
     *  Linker dependency on implementation class.
     */
    private static var implClassDependency:HistoryManagerImpl;

	/**
	 *  @private
	 */
	private static var _impl:IHistoryManager;

	/**
	 *  @private
	 *  This needs to be a getter so the instance is created only after
	 *  initialization, on first access.
	 */
	private static function get impl():IHistoryManager
	{
	    if (!_impl)
		_impl = Singleton.getInstance("mx.managers::IHistoryManager") as IHistoryManager;
	    
	    return _impl;
	}
	//--------------------------------------------------------------------------
	//
	//  Class methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  DEPRECATED - Initializes the HistoryManager. In general, this does not need to be called
	 *  because any time you add a component with <code>historyManagementEnabled</code>, Flex
	 *  calls this method. However, the HistoryManager will not work correctly if it is 
	 *  not initialized from the top-level application. So, if your application does
	 *  not have any HistoryManager enabled components in it and loads other sub-applications
	 *  That do, you must call the <code>HistoryManager.initialize()</code> method in the 
	 *  main application, usually from an <code>initialize</code> event handler on the application.
	 *
	 *  @param sm SystemManager for this application.
	 */
    public static function initialize(sm:ISystemManager):void
    {
		// this code is handled in HistoryManagerImpl.getInstance() now
    }

	/**
	 *  Registers an object with the HistoryManager.
	 *  The object must implement the IHistoryManagerClient interface.
	 *
	 *  @param obj Object to register.
	 *
	 *  @see mx.managers.IHistoryManagerClient
	 */	
	public static function register(obj:IHistoryManagerClient):void
	{
	    impl.register(obj);
	}

	/**
	 *  Unregisters an object with the HistoryManager.
	 *
	 *  @param obj Object to unregister.
	 */
	public static function unregister(obj:IHistoryManagerClient):void
	{
	    impl.unregister(obj);
	}

	/**
	 *  Saves the application's current state so it can be restored later.
	 *  This method is automatically called by navigator containers
	 *  whenever their navigation state changes.
	 *  If you registered an interface with the HistoryManager,
	 *  you are responsible for calling the <code>save()</code> method
	 *  when the application state changes.
	 */	
	public static function save():void
	{
	    impl.save();
	}

	//--------------------------------------------------------------------------
	//
	//  Class LocalConnection handlers called by history.swf
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 *  Called by the register() method of the history.swf
	 *  to return the handshake we sent in initHandshake().
	 *  HistoryManager.save() doesn't nothing until this method is called,
	 *  since the history.swf must be ready to store the query parameters.
	 */
	mx_internal static function registered():void
	{
		impl.registered();
	}
	
	/**
	 *  @private
	 *  Called by the initHandshake() method of history.swf.
	 *  This method responds to its handshake by calling the
	 *  registered() method of history.swf.
	 *  history.swf will then call loadState() below.
	 */
	mx_internal static function registerHandshake():void
	{
		impl.registerHandshake();
	}
	
	/**
	 *  @private
	 *  Loads state information.
	 *  Called by the registered() method of history.swf
	 *  after we have returned its handshake.
	 *
	 *  @param stateVars State information.
	 */
	mx_internal static function load(stateVars:Object):void
	{
		impl.load(stateVars);
	}

	/**
	 *  @private
	 */
	mx_internal static function loadInitialState():void
	{
		impl.loadInitialState();		// Load up the initial application state.
	}
}

}

