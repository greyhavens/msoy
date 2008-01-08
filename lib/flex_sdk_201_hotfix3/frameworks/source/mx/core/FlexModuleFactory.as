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

import flash.display.Loader;
import flash.display.MovieClip;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;
import flash.events.TimerEvent;
import flash.net.URLRequest;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.utils.Timer;
import flash.utils.getDefinitionByName;

[ExcludeClass]

/**
 *  @private
 */
public class FlexModuleFactory extends MovieClip implements IFlexModuleFactory
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 */
    private static const INIT_STATE:int = 0;
    
    /**
	 *  @private
	 */
	private static const RSL_LOAD_STATE:int = 1;
    
    /**
	 *  @private
	 */
	private static const APP_LOAD_STATE:int = 2;
    
    /**
	 *  @private
	 */
	private static const APP_START_STATE:int = 3;
    
    /**
	 *  @private
	 */
	private static const APP_RUNNING_STATE:int = 4;
    
    /**
	 *  @private
	 */
	private static const ERROR_STATE:int = 5;

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 */
	public function FlexModuleFactory()
    {
		super();

        rslList = info()["rsls"];
		mixinList = info()["mixins"];

		stop(); // Make sure to stop the playhead on the currentframe
        
		loaderInfo.addEventListener(Event.COMPLETE, moduleCompleteHandler);

	    var docFrame:int = (totalFrames == 1)? 0 : 1;

		addFrameScript(docFrame, docFrameHandler);

		// Frame 1: module factory
		// Frame 2: document class
		// Frame 3+: extra classes
	    for (var f:int = docFrame + 1; f < totalFrames; ++f)
	    {
		    addFrameScript(f, extraFrameHandler);
		}

		timer = new Timer(100);
		timer.addEventListener(TimerEvent.TIMER, timerHandler);
		timer.start();

        update();
    }

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 */
    private var rslList:Array;
    
    /**
	 *  @private
	 */
	private var mixinList:Array;

    /**
	 *  @private
	 */
    private var state:int = INIT_STATE;

    /**
	 *  @private
	 */
    private var rslLoader:Loader = null;
    
    /**
	 *  @private
	 */
	private var rslCurrentURL:String = null;
    
    /**
	 *  @private
	 */
	private var rslCurrentIndex:int = -1;
    
    /**
	 *  @private
	 */
	private var rslReady:Boolean = true;
    
    /**
	 *  @private
	 */
	private var appReady:Boolean = false;
    
    /**
	 *  @private
	 */
	private var appLoaded:Boolean = false;
    
    /**
	 *  @private
	 */
	private var timer:Timer = null;
    /**
	 *  @private
	 */
	private var nextFrameTimer:Timer = null;
    
    /**
	 *  @private
	 */
	private var errorMessage:String = null;

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

  	/**
   	 *  @private
   	 */
    public function create(... params):Object
    {
	    var mainClassName:String = info()["mainClassName"];
	    
		if (mainClassName == null)
	    {
            var url:String = loaderInfo.loaderURL;
            var dot:Number = url.lastIndexOf(".");
            var slash:Number = url.lastIndexOf("/");
            mainClassName = url.substring(slash+1, dot);
	    }
	   
        var mainClass:Class = Class(getDefinitionByName(mainClassName));

        return mainClass? new mainClass() : null;
    }

  	/**
   	 *  @private
   	 */
    public function info():Object
    {
        return {};
    }

   /**
    *  @inheritDoc
    */
    public function getDefinitionByName(name:String):Object
    {
       const domain:ApplicationDomain = info()["currentDomain"] as ApplicationDomain; 

       var definition:Object;
       if (domain.hasDefinition(name))
          definition = domain.getDefinition(name);

       return definition;
    }

    /**
	 *  @private
	 */
    private function update():void
    {
        switch (state)
        {
            case INIT_STATE:
			{
                if ((rslList == null) || (rslList.length == 0))
                    state = APP_LOAD_STATE;
                else
                    state = RSL_LOAD_STATE;
				break;
			}

            case RSL_LOAD_STATE:
			{
                if (rslReady)
                {
                    if (++rslCurrentIndex >= rslList.length)
                    {
                        state = APP_LOAD_STATE;
                    }
                    else
                    {
                        var url:String = rslList[rslCurrentIndex].url;
                        rslReady = false;
                        loadLibrary(rslList[rslCurrentIndex].url);
                    }
                }
                break;
			}

            case APP_LOAD_STATE:
			{
                if (appLoaded)
                {
                    deferredNextFrame();
                    state = APP_START_STATE;
                }
                break;
			}

            case APP_START_STATE:
			{
                if (appReady)
                {
            		if (mixinList && mixinList.length > 0)
            		{
            		    var n:int = mixinList.length;
            			for (var i:int = 0; i < n; i++)
            		    {
            		        var c:Class;
            		        try
            		        {
            		           c =	Class(getDefinitionByName(mixinList[i]));
            		           c["init"](this);
            		        }
            		        catch (e:Error)
            		        {}
            		    }
                    }

                    state = APP_RUNNING_STATE;
        			timer.removeEventListener(TimerEvent.TIMER, timerHandler);
        			// Stop the timer.
        			timer.reset();

                    dispatchEvent(new Event("ready"));
                }
                break;
			}

            case ERROR_STATE:
			{
                if (timer != null)
                {
        			timer.removeEventListener(TimerEvent.TIMER, timerHandler);
        			// stop the timer
        			timer.reset();
                }
                if (!rslReady)
                {
                    rslLoader.removeEventListener(Event.COMPLETE,
												  rslCompleteHandler);
                }
                var tf:TextField = new TextField();
                tf.text = errorMessage;
                tf.x = 0;
                tf.y = 0;
                tf.autoSize = TextFieldAutoSize.LEFT;
                addChild(tf);
                break;
			}
        }
    }

    /**
	 *  @private
	 */
    public function autorun():Boolean
    {
        return true;
    }

    /**
	 *  @private
	 */
    private function displayError(msg:String):void
    {
        errorMessage = msg;
        state = ERROR_STATE;
        update();
    }
	
    /**
	 *  @private
	 */
	private function docFrameHandler(event:Event = null):void
	{
        // Register singletons
        Singleton.registerClass("mx.managers::ICursorManager", Class(getDefinitionByName("mx.managers::CursorManagerImpl")));
        Singleton.registerClass("mx.managers::IDragManager", Class(getDefinitionByName("mx.managers::DragManagerImpl")));
        Singleton.registerClass("mx.managers::IHistoryManager", Class(getDefinitionByName("mx.managers::HistoryManagerImpl")));
        Singleton.registerClass("mx.managers::ILayoutManager", Class(getDefinitionByName("mx.managers::LayoutManager")));
        Singleton.registerClass("mx.managers::IPopUpManager", Class(getDefinitionByName("mx.managers::PopUpManagerImpl")));
        Singleton.registerClass("mx.styles::IStyleManager", Class(getDefinitionByName("mx.styles::StyleManagerImpl")));
        Singleton.registerClass("mx.managers::IToolTipManager", Class(getDefinitionByName("mx.managers::ToolTipManagerImpl")));

        appReady = true;
        update();
        if (currentFrame < totalFrames)
            deferredNextFrame();
    }
    /**
	 *  @private
	 */
    private function deferredNextFrame():void
    {
        if (currentFrame + 1 <= framesLoaded)
            nextFrame();
        else
        {
            // next frame isn't baked yet, we'll check back...
    		nextFrameTimer = new Timer(100);
		    nextFrameTimer.addEventListener(TimerEvent.TIMER, nextFrameTimerHandler);
		    nextFrameTimer.start();
        }
    }
    /**
	 *  @private
	 */
	private function extraFrameHandler(event:Event = null):void
	{
	    var frameList:Object = info()["frames"];

	    if (frameList && frameList[currentLabel])
	    {
	        var c:Class;
	        try
	        {
	            c = Class(getDefinitionByName(frameList[currentLabel]));
	            c["frame"](this);
	        }
	        catch (e:Error)
	        {}

	    }
	    if (currentFrame < totalFrames)
	    {
            deferredNextFrame();
        }
	}


    /**
	 *  @private
	 */
    private function loadLibrary(url:String):void
    {
        rslCurrentURL = url;

        rslLoader = new Loader();
		
		rslLoader.contentLoaderInfo.addEventListener(
			Event.COMPLETE, rslCompleteHandler);
        
		rslLoader.contentLoaderInfo.addEventListener(
			IOErrorEvent.IO_ERROR, rslErrorHandler);
        
		rslLoader.contentLoaderInfo.addEventListener(
			SecurityErrorEvent.SECURITY_ERROR, rslErrorHandler);
        
		var urlRequest:URLRequest = new URLRequest(url);
		var loaderContext:LoaderContext = new LoaderContext();
		loaderContext.applicationDomain = ApplicationDomain.currentDomain;
		rslLoader.load(urlRequest, loaderContext);
    }

	//--------------------------------------------------------------------------
	//
	//  Event handlers
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 */
    private function rslCompleteHandler(event:Event):void
    {
        rslReady = true;
        update();
    }

    /**
	 *  @private
	 */
    private function rslErrorHandler(event:Event):void
    {
        trace("RSL " + rslCurrentURL + " failed to load.");
        displayError("RSL " + rslCurrentURL + " failed to load.");
    }

    /**
	 *  @private
	 */
    private function moduleCompleteHandler(event:Event):void
    {
        appLoaded = true;
		update();
    }

    /**
	 *  @private
	 */
	private function timerHandler(event:TimerEvent):void
	{
	    if ((totalFrames > 2 && framesLoaded >= 2) || (framesLoaded == totalFrames))
            appLoaded = true;
		
		update();
    }

    /**
	 *  @private
	 */
	private function nextFrameTimerHandler(event:TimerEvent):void
	{
	    if (currentFrame + 1 <= framesLoaded)
	    {
	        nextFrame();
            nextFrameTimer.removeEventListener(TimerEvent.TIMER, nextFrameTimerHandler);
        	// stop the timer
        	nextFrameTimer.reset();
        }
    }
}

}

