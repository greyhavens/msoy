////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.preloaders
{

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.MovieClip;
import flash.display.Sprite;
import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IEventDispatcher;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;
import flash.events.TimerEvent;
import flash.net.URLRequest;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.utils.Timer;
import mx.core.mx_internal;
import mx.events.FlexEvent;
import mx.events.RSLEvent;

/**
 *  The Preloader class is used by the SystemManager to monitor
 *  the download and initialization status of a Flex application.
 *  It is also responsible for downloading the runtime shared libraries (RSLs).
 *
 *  <p>The Preloader class instantiates a download progress bar, 
 *  which must implement the IPreloaderDisplay interface, and passes download
 *  and initialization events to the download progress bar.</p>
 *
 *  @see mx.preloaders.DownloadProgressBar
 *  @see mx.preloaders.Preloader
 */
public class Preloader extends Sprite
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *	Constructor
	 */
	public function Preloader()
	{
		super()
	}	
	
	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	private var displayClass:IPreloaderDisplay = null;
	
	/**
	 *  @private
	 */
	private var timer:Timer;
	
	/**
	 *  @private
	 */
	private var showDisplay:Boolean;
	
	/**
	 *  @private
	 */
	private var rslLibs:Array;
	
	/**
	 *  @private
	 */
	private var rslIndex:int = 0;
	
	/**
	 *  @private
	 */
	private var rslDone:Boolean = false;
	
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**  
	 *  Called by the SystemManager to initialize a Preloader object.
	 * 
	 *  @param showDisplay Determines if the display class should be displayed.
	 *
	 *  @param displayClassName The IPreloaderDisplay class to use
	 *  for displaying the preloader status.
	 *
	 *  @param backgroundColor Background color of the application.
	 *
	 *  @param backgroundAlpha Background alpha of the application.
	 *
	 *  @param backgroundImage Background image of the application.
	 *
	 *  @param backgroundSize Background size of the application.
	 *
	 *  @param displayWidth Width of the application.
	 *
	 *  @param displayHeight Height of the application.
	 *
	 *  @param libs Array of string URLs for the runtime shared libraries.
	 *
	 *  @param sizes Array of uint values containing the byte size for each URL
	 *  in the libs argument
	 */ 
	public function initialize(showDisplay:Boolean, 
							   displayClassName:Class,
							   backgroundColor:uint,
							   backgroundAlpha:Number,
							   backgroundImage:Object,
							   backgroundSize:String,
							   displayWidth:Number,
							   displayHeight:Number,
							   libs:Array = null,
							   sizes:Array = null):void
	{
		root.loaderInfo.addEventListener(IOErrorEvent.IO_ERROR, ioErrorHandler);
	
		// Store the RSL information.
		if (libs && libs.length > 0)
		{
			rslLibs = [];
			var n:int = libs.length;
			for (var i:int = 0; i < n; i++)
			{
			    var req:URLRequest = new URLRequest(libs[i]);
                var size:int = 0;
                if (sizes && sizes.length == libs.length)
                    size = sizes[i];
			    var node:RSLNode = new RSLNode(req, i, size, this);
				rslLibs.push(node);
			}
			
			// Start loading the RSLs.
			loadRSL(0);
		}
		else
		{
		    rslDone = true;
		}
		
		this.showDisplay = showDisplay;

		// Create the timer (really should be adding event listeners to root.LoaderInfo)	
		timer = new Timer(10);
		timer.addEventListener(TimerEvent.TIMER, timerHandler);
		timer.start();
		
		// Create a new instance of the display class and attach it to the stage
		if (showDisplay)
		{
			displayClass = new displayClassName(); 
			// Listen for when the displayClass no longer needs to be on the stage
			displayClass.addEventListener(Event.COMPLETE,
										  displayClassCompleteHandler);
			
			// Add the display class as a child of the Preloader
			addChild(DisplayObject(displayClass));
						
			displayClass.backgroundColor = backgroundColor;
			displayClass.backgroundAlpha = backgroundAlpha;
			displayClass.backgroundImage = backgroundImage;
			displayClass.backgroundSize = backgroundSize;
			displayClass.stageWidth = displayWidth;
			displayClass.stageHeight = displayHeight;
			displayClass.initialize();  
			displayClass.preloader = this;
		}	
	}
	
	/**
	 *  Called by the SystemManager after it has finished instantiating
	 *  an instance of the application class. Flex calls this method; you 
	 *  do not call it yourself.
	 *
	 *  @param app The application object.
	 */
	public function registerApplication(app:IEventDispatcher):void
	{
		// Listen for events from the application.
		
		app.addEventListener("validatePropertiesComplete",
							 appProgressHandler);
		
		app.addEventListener("validateSizeComplete",
							 appProgressHandler);
		
		app.addEventListener("validateDisplayListComplete",
							 appProgressHandler);
		
		app.addEventListener(FlexEvent.CREATION_COMPLETE,
							 appCreationCompleteHandler);
	}
	
	/**
	 *  @private
	 *  Called to load the currently index RSL. 
	 */
	private function loadRSL(index:int):void
	{
		var rslNode:RSLNode;

		if (index < rslLibs.length)
		{
			rslNode = rslLibs[index];
			var loader:Loader = new Loader();
			
			// The RSLNode needs to listen to certain events.
			
			loader.contentLoaderInfo.addEventListener(
				ProgressEvent.PROGRESS, rslNode.progressHandler);
			
			loader.contentLoaderInfo.addEventListener(
				Event.COMPLETE, rslNode.completeHandler);
			
			loader.contentLoaderInfo.addEventListener(
				IOErrorEvent.IO_ERROR, rslNode.errorHandler);
			
			loader.contentLoaderInfo.addEventListener(
				SecurityErrorEvent.SECURITY_ERROR, rslNode.errorHandler);
			
			var loaderContext:LoaderContext = new LoaderContext();
			loaderContext.applicationDomain = ApplicationDomain.currentDomain;
			loader.load(rslNode.url, loaderContext);
		}
		else
		{
			// Let the preloader know we are done with all of the RSLs
			/*
			var done:Boolean = true;
			var n:int = rslLibs.length;
			for (var i:int = 0; i < n; i++)
			{
				if (!rslLibs[i].completed && rslLibs[i].errorText != "")
				{
					done = false;
					break;
				}
			}
			
			rslDone = done;
			*/
			rslDone = true;
		}
	}
	
	/**
	 *  @private
	 *  Return the number of bytes loaded and total for the SWF and any RSLs.
	 */
	private function getByteValues():Object
	{
		var li:LoaderInfo = root.loaderInfo;
		var loaded:int = li.bytesLoaded;
		var total:int = li.bytesTotal;
		
		// Look up the rsl bytes and include those
		var n:int = rslLibs ? rslLibs.length : 0;
		for (var i:int = 0; i < n; i++)
		{
			loaded += rslLibs[i].loaded;
			total += rslLibs[i].total;
		}
		
		return { loaded: loaded, total: total };
	}
	
	/**
	 *  @private
	 */
	private function dispatchAppEndEvent(event:Object = null):void
	{
		// Dispatch the application initialization end event
		dispatchEvent(new FlexEvent(FlexEvent.INIT_COMPLETE));
		
		if (!showDisplay)
			displayClassCompleteHandler(null);
	}
	
	/**
	 *  @private
	 *  We don't listen for the events directly
	 *  because we don't know which RSL is sending the event.
	 *  So we have the RSLNode listen to the events
	 *  and then pass them along to the Preloader.
	 */ 
	mx_internal function rslProgressHandler(event:ProgressEvent, rsl:RSLNode):void
	{
		var rslEvent:RSLEvent = new RSLEvent(RSLEvent.RSL_PROGRESS);
		rslEvent.bytesLoaded = event.bytesLoaded;
		rslEvent.bytesTotal = event.bytesTotal;
		rslEvent.rslIndex = rsl.index;
		rslEvent.rslTotal = rslLibs.length;
		rslEvent.url = rsl.url;
		dispatchEvent(rslEvent);
	}
	
	/**
	 *  @private
	 *  Load the next RSL in the list and dispatch an event.
	 */
	mx_internal function rslCompleteHandler(event:Event, rsl:RSLNode):void
	{
		var rslEvent:RSLEvent = new RSLEvent(RSLEvent.RSL_COMPLETE);
		rslEvent.bytesLoaded = rsl.total;
		rslEvent.bytesTotal = rsl.total;
		rslEvent.rslIndex = rsl.index;
		rslEvent.rslTotal = rslLibs.length;
		rslEvent.url = rsl.url;
		dispatchEvent(rslEvent);

		loadRSL(++rslIndex);
	}
	
	/**
	 *  @private
	 */
	mx_internal function rslErrorHandler(event:ErrorEvent, rsl:RSLNode):void
	{
		// send an error event
		var rslEvent:RSLEvent = new RSLEvent(RSLEvent.RSL_ERROR);
		rslEvent.bytesLoaded = 0;
		rslEvent.bytesTotal = 0;
		rslEvent.rslIndex = rsl.index;
		rslEvent.rslTotal = rslLibs.length;
		rslEvent.url = rsl.url;
		rslEvent.errorText = event.text;
		dispatchEvent(rslEvent);
	}

	//--------------------------------------------------------------------------
	//
	//  Event handlers
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 *  Listen or poll for progress events and dispatch events
	 *  describing the current state of the download
	 */
	private function timerHandler(event:TimerEvent):void
	{
		// loaded swfs may not have root right away
		if (!root)
			return;

		var bytes:Object = getByteValues();
		var loaded:int = bytes.loaded;
		var total:int = bytes.total;
		
		// Dispatch a progress event (later we might conditionalize this
		// so that it isn't sent on a cache load).
		dispatchEvent(new ProgressEvent(ProgressEvent.PROGRESS,
										false, false, loaded, total));

		// Check if we are finished
		if (rslDone &&
			((loaded >= total && total > 0) || (total == 0 && loaded > 0) || (root is MovieClip && (MovieClip(root).totalFrames > 2) && (MovieClip(root).framesLoaded >= 2)) ))
		{
			timer.removeEventListener(TimerEvent.TIMER, timerHandler);
			
			// Stop the timer.
			timer.reset();
			
			// Dispatch a complete event.
			dispatchEvent(new Event(Event.COMPLETE));
			
			// Dispatch an initProgress event.
			dispatchEvent(new FlexEvent(FlexEvent.INIT_PROGRESS));
		}
	}

	/**
	 *  @private
	 */
	private function ioErrorHandler(event:IOErrorEvent):void
	{
		// Ignore the event
	}

	/**
	 *  @private
	 *	Called when the displayClass has finished animating
	 *  and no longer needs to be displayed.
	 */
	private function displayClassCompleteHandler(event:Event):void
	{
		// Send an event to the SystemManager that we are completely finished
		dispatchEvent(new FlexEvent(FlexEvent.PRELOADER_DONE));
	}
		
	/**
	 *  @private
	 */
	private function appCreationCompleteHandler(event:FlexEvent):void
	{		
		dispatchAppEndEvent();
	}
	
	/**
	 *  @private
	 */
	private function appProgressHandler(event:Event):void
	{		
		dispatchEvent(new FlexEvent(FlexEvent.INIT_PROGRESS));
	}
}

}

////////////////////////////////////////////////////////////////////////////////
//
//  Helper class: RSLNode
//
////////////////////////////////////////////////////////////////////////////////

import flash.events.Event;
import flash.events.ErrorEvent;
import flash.events.ProgressEvent;
import flash.net.URLRequest;
import mx.core.mx_internal;
import mx.effects.EffectInstance;
import mx.preloaders.Preloader;

/**
 *  @private
 *	RSL Node Class
 */
class RSLNode
{
	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function RSLNode(url:URLRequest, index:int,
							total:int, owner:Preloader)
	{
		super();

		this.url = url;
		this.index = index;
		this.total = total;
		this.owner = owner;
	}

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	public var url:URLRequest;
	
	/**
	 *  @private
	 */
	public var index:int;
	
	/**
	 *  @private
	 */
	public var total:uint = 0;
	
	/**
	 *  @private
	 */
	public var owner:Preloader;

	/**
	 *  @private
	 */
	public var loaded:uint = 0;
	
	/**
	 *  @private
	 */
	public var errorText:String;
	
	/**
	 *  @private
	 */
	public var completed:Boolean = false;
	
	//--------------------------------------------------------------------------
	//
	//  Event handlers
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	public function progressHandler(event:ProgressEvent):void
	{
		// Update the loaded and total properties.
		loaded = event.bytesLoaded;
		total = event.bytesTotal;
		
		// Notify the preloader.
		owner.mx_internal::rslProgressHandler(event, this); 
	}

	/**
	 *  @private
	 */
	public function completeHandler(event:Event):void
	{
		completed = true;
		
		// Notify the preloader.
		owner.mx_internal::rslCompleteHandler(event, this);
	}

	/**
	 *  @private
	 */
	public function errorHandler(event:ErrorEvent):void
	{
		errorText = event.text;
		completed = true;
		loaded = 0;
		total = 0;
		
		// Notify the preloader.
		owner.mx_internal::rslErrorHandler(event, this);
	}
}
