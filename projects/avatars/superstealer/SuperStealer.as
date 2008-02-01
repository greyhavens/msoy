package {

import flash.display.Sprite;
import flash.display.Loader;

import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.KeyboardEvent;
import flash.events.ProgressEvent;
import flash.events.TextEvent;

import flash.text.TextField;
import flash.text.TextFieldType;

import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.SecurityDomain;

import flash.ui.Keyboard;


import com.whirled.AvatarControl;
import com.whirled.ControlEvent;

[SWF(width="600", height="450")]
public class SuperStealer extends Sprite
{
    public static const NEW_URL_ACTION :String = "Enter new URL";

    public static const QUERY_URL_MSG :String = "qURL";

    public static const NOTIFY_URL_MSG :String = "URL";

    public function SuperStealer ()
    {
        _ctrl = new AvatarControl(this);
        _ctrl.addEventListener(ControlEvent.ACTION_TRIGGERED, handleActionTriggered);
        _ctrl.addEventListener(ControlEvent.MESSAGE_RECEIVED, handleMessageReceived);
        _ctrl.addEventListener(ControlEvent.GOT_CONTROL, showInputField);

        _ctrl.registerActions(NEW_URL_ACTION);

        // whenever we start up, we broadcast an "Oy! What's my URL?" message
        _ctrl.sendMessage(QUERY_URL_MSG);

        // is this necessary?
        if (!_ctrl.hasControl()) {
            _ctrl.requestControl();
        }

        // see when we're added to the stage
        addEventListener(Event.ADDED_TO_STAGE, handleAddedToStage);
    }

    protected function handleAddedToStage (... ignored) :void
    {
        if (_url == null && _ctrl.hasControl()) {
            showInputField();
        }
    }

    protected function handleMessageReceived (event :ControlEvent) :void
    {
        switch (event.name) {
        case QUERY_URL_MSG:
            if (_ctrl.hasControl()) {
                if (_url != null) {
                    _ctrl.sendMessage(NOTIFY_URL_MSG, _url);

                } else {
                    showInputField();
                }
            }
            break;

        case NOTIFY_URL_MSG:
            loadUrl(event.value as String);
            break;
        }
    }

    protected function handleActionTriggered (event :ControlEvent) :void
    {
        switch (event.name) {
        case NEW_URL_ACTION:
            if (_ctrl.hasControl()) {
                showInputField();
            }
            break;
        }
    }

    protected function showInputField (... ignored) :void
    {
        if (!this.stage) {
            return;
        }

        if (_input == null) {
            _input = new TextField();
            _input.background = true;
            _input.width = 600;
            _input.height = 20;
            _input.type = TextFieldType.INPUT;
            _input.addEventListener(KeyboardEvent.KEY_DOWN, handleKeyDown)
            addChild(_input);
        }

        this.stage.focus = _input;
    }

    protected function loadUrl (url :String) :void
    {
        // avoid pointless dickery
        if (_url == url) {
            return;
        }

        // clean up any old loader
        if (_loader != null) {
            try {
                _loader.close();
            } catch (err :Error) {
                // ignore
            }
            _loader.unload();
            removeChild(_loader);
        }
        _hotSpotSet = false;

        // start loading the new one
        _url = url;
        _loader = new Loader();
        _loader.contentLoaderInfo.sharedEvents.addEventListener("controlConnect", handleConnect);
        _loader.contentLoaderInfo.addEventListener(ProgressEvent.PROGRESS, loadProgress);
        _loader.contentLoaderInfo.addEventListener(Event.INIT, loadProgress);
        _loader.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, loadError);
        _loader.load(new URLRequest(url),
            new LoaderContext(false, new ApplicationDomain(null), null));
        addChildAt(_loader, 0);
    }

    protected function handleKeyDown (event :KeyboardEvent) :void
    {
        if (event.keyCode != Keyboard.ENTER) {
            return;
        }

        var txt :String = _input.text;
        removeChild(_input);
        _input = null;

        if (txt != "") {
            // dispatch with all haste
            _ctrl.setState(null); // reset state
            _ctrl.setMoveSpeed(500); // reset to standard move speed
            _ctrl.sendMessage(NOTIFY_URL_MSG, txt);
        }
    }

    protected function loadProgress (... ignored) :void
    {
        // if the content has set the hotspot already, we don't
        if (_hotSpotSet) {
            return;
        }

        // check size, set the hotspot once it's known
        try {
            var w :Number = _loader.contentLoaderInfo.width;
            var h :Number = _loader.contentLoaderInfo.height;
            _ctrl.setHotSpot(w/2, h);

        } catch (err :Error) {
            // oh well!
        }
    }

    protected function loadError (event :IOErrorEvent) :void
    {
        trace("Got load error: " + event);
    }

    protected function handleConnect (cheese :Object) :void
    {
        var propName :String;
        var newProps :Object = {};

        // we have a few properties that NEED implementing, so if they're not there,
        // force them in
        var dummyFn :Function = function (... args) :void {};
        for each (propName in REQUIRED_CONTENT_PROPS) {
            if (!(propName in cheese.userProps)) {
                // force in a placeholder
                cheese.userProps[propName] = dummyFn;
            }
        }
        // replace all the content's functions with interceptors
        for (propName in cheese.userProps) {
            newProps[propName] = createContentReplacement(propName, cheese.userProps[propName]);
        }
        cheese.userProps = newProps;

        // dispatch it upwards
        this.root.loaderInfo.sharedEvents.dispatchEvent((cheese as Event).clone());

        if (cheese.hostProps != null) {
            newProps = {};
            for (propName in cheese.hostProps) {
                var fn :Function = (cheese.hostProps[propName] as Function);
                if (fn != null) {
                    newProps[propName] = createWhirledReplacement(propName, fn);
                } else {
                    newProps[propName] = cheese.hostProps[propName];
                }
            }

            cheese.hostProps = newProps;
        }
    }

    protected function createContentReplacement (name :String, orig :Function) :Function
    {
        switch (name) {
        default:
            return orig;

        case "messageReceived_v1":
            return function (... args) :* {
                var msgName :String = args[0] as String;
                var arg :Object = args[1];
                var isAction :Boolean = args[2];
                if (isAction && (msgName == NEW_URL_ACTION)) {
                    handleActionTriggered(new ControlEvent("", msgName, arg));

                } else if (!isAction &&
                        ((msgName == QUERY_URL_MSG) || (msgName == NOTIFY_URL_MSG))) {
                    handleMessageReceived(new ControlEvent("", msgName, arg));

                } else {
                    return orig.apply(null, args);
                }
            };

        case "getActions_v1":
            return function (... args) :* {
                var actions :Array = orig.apply(null, args) as Array;
                if (actions == null) {
                    actions = [];
                } else {
                    // make a copy
                    actions = actions.concat();
                }
                actions.unshift(NEW_URL_ACTION);
                return actions;
            };
        }
    }

    protected function createWhirledReplacement (name :String, orig :Function) :Function
    {
        switch (name) {
        default:
            return orig;

        case "setHotSpot_v1":
            return function (... args) :* {
                _hotSpotSet = true;
                return orig.apply(null, args);
            };
        }
    }

    protected var _ctrl :AvatarControl;

    protected var _inControl :Boolean;

    protected var _input :TextField;

    protected var _loader :Loader;

    protected var _url :String;

    protected var _hotSpotSet :Boolean = false;

    protected static const REQUIRED_CONTENT_PROPS :Array = [
        "messageReceived_v1", "getActions_v1"
    ];
}
}
