package com.threerings.msoy.game.data {

import flash.errors.IllegalOperationError;

import flash.net.registerClassAlias; // function import

import flash.utils.IExternalizable;
import flash.utils.Proxy;

import flash.utils.flash_proxy;

import mx.utils.ObjectUtil;

import com.threerings.util.ClassUtil;

use namespace flash_proxy;

public class GameData extends Proxy
{
    public function GameData (gameObject :FlashGameObject, obj :Object)
    {
        _gameObject = gameObject;
        _obj = obj;
    }

    public function hasOwnProperty (propName :String) :Boolean
    {
        // pass-through
        return _obj.hasOwnProperty(propName);
    }

    public function propertyIsEnumerable (propName :String) :Boolean
    {
        // pass-through
        return _obj.propertyIsEnumerable(propName);
    }

    public function setPropertyIsEnumerable (
        propName :String, isEnum :Boolean = true) :void
    {
        // pass-through
        _obj.setPropertyIsEnumerable(propName, isEnum);
    }

    override flash_proxy function callProperty (propName :*, ... rest) :*
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

    override flash_proxy function getProperty (propName :*) :*
    {
        // pass-through
        return _obj[propName];
    }

    override flash_proxy function hasProperty (propName :*) :Boolean
    {
        // pass-through
        return (_obj[propName] != undefined);
    }

    override flash_proxy function setProperty (propName :*, value :*) :void
    {
        if (propName == null) {
            throw new IllegalOperationError();

        } else if (value != null) {
            validateProperty(value);
        }
        _gameObject.requestPropertyChange(propName, value);
    }

    override flash_proxy function deleteProperty (propName :*) :Boolean
    {
        var hasProp :Boolean = hasProperty(propName);
        _gameObject.requestPropertyChange(propName, null);
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
        // the index is 1-based, so subtract one
        return (_propertyList[index - 1] as String);
    }

    override flash_proxy function nextValue (index :int) :*
    {
        return _obj[nextName(index)];
    }

    /**
     * Validate that the user is setting a reasonable property.
     */
    private function validateProperty (prop :Object) :void
    {
        if (prop.constructor == Object) {
            // make sure all sub-props are kosher
            for each (var subProp :Object in prop) {
                validateProperty(subProp);
            }

        } else if (prop is IExternalizable) {
            var name :String = ClassUtil.getClassName(prop);
            registerClassAlias(
                ClassUtil.getClassName(prop), ClassUtil.getClass(prop));


        } else if (!ObjectUtil.isSimple(prop)) {
            throw new IllegalOperationError("You may not add non-simple " +
                "object properties unless the class implements IExternalizable");
        }
    }

    protected var _gameObject :FlashGameObject;

    /** The object we're proxying. */
    protected var _obj :Object;

    /** Used temporarily while iterating over our names or values. */
    protected var _propertyList :Array;
}
}
