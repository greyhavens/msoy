package com.threerings.msoy.game.data {

import flash.errors.IllegalOperationError;

import flash.utils.Proxy;
import flash.utils.flash_proxy;

use namespace flash_proxy;

public class GameData extends Proxy
{
    public function GameData (gameObject :FlashGameObject, obj :Object)
    {
        _gameObject = gameObject;
        _obj = obj;
    }

    override flash_proxy function callProperty (name :*, ... rest) :*
    {
        // don't allow function calls
        throw new IllegalOperationError();
    }

    override flash_proxy function getDescendants (name :*) :*
    {
        // we don't need this XML business
        throw new IllegalOperationError();
    }

    override flash_proxy function isAttribute (name :*) :Boolean
    {
        // we don't need this XML business
        throw new IllegalOperationError();
    }

    override flash_proxy function getProperty (name :*) :*
    {
        return _obj[name];
        // TODO: sub-proxying for non-simple property values???
    }

    override flash_proxy function hasProperty (name :*) :Boolean
    {
        return (_obj[name] != undefined);
    }

    override flash_proxy function setProperty (name :*, value :*) :void
    {
        if (value is Function) {
            throw new IllegalOperationError();
        }
        _gameObject.requestPropertyChange(name, value);
    }

    override flash_proxy function deleteProperty (name :*) :Boolean
    {
        var hasProp :Boolean = hasProperty(name);
        _gameObject.requestPropertyChange(name, null);
        return hasProp;
    }

    override flash_proxy function nextNameIndex (index :int) :int
    {
        // possibly set up the property list on the first call
        if (index == 0) {
            _propertyList = [];
            for (var prop :String in _obj) {
                _propertyList.push(prop);
            }
        }

        // return a 1-based index to indicate that there is a property
        if (index < _propertyList.length) {
            return index + 1;

        } else {
            // we're done, clear the prop list
            _propertyList = null;
            return 0;
        }
    }

    override flash_proxy function nextName (index :int) :String
    {
        return (_propertyList[index - 1] as String);
    }

    override flash_proxy function nextValue (index :int) :*
    {
        return _obj[nextName(index)];
    }

    protected var _gameObject :FlashGameObject;

    /** The object we're proxying. */
    protected var _obj :Object;

    /** Used temporarily while iterating over our names or values. */
    protected var _propertyList :Array;
}
}
