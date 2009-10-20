//
// $Id$

package com.threerings.msoy.applets.remixer {

import mx.validators.Validator;
import mx.validators.ValidationResult;

import com.threerings.util.XmlUtil;

public class XMLValidator extends Validator
{
    public function XMLValidator (ctx :RemixContext)
    {
        super();
        _ctx = ctx;
    }

    override protected function doValidation (value :Object) :Array
    {
        var results :Array = super.doValidation(value);

        try {
            XmlUtil.newXML(value);
        } catch (e :Error) {
            results.push(
                new ValidationResult(true, null, "notXML", _ctx.REMIX.get("e.notXML", e.message)));
        }

        return results;
    }

    protected var _ctx :RemixContext;
}
}
