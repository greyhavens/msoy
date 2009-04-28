// $Id: PercentageTransformer.java 1377 2009-02-22 20:45:19Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.trans;

import java.text.DecimalFormat;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import com.threerings.panopticon.aggregator.PropertiesResultTransformer;
import com.threerings.panopticon.common.event.EventData;

/**
 * <p>Adds a fractional or percentage value, based on a fraction of two data columns.</p>
 *
 * <p>The user specifies the numerator and denominator columns. Also supported is a list of
 * numerators, in which case the output will be mapped to the appropriate output column.</p>
 *
 * Required config options:
 * <ul>
 *      <li><b>inputOver</b>: the numerator (or list of numerators).</li>
 *      <li><b>inputUnder</b>: the denominator.</li>
 * </ul>
 * Optional config options:
 * <ul>
 *       <li><b>outputField</b>: name of the output column (or list of columns); defaults to
 *           'percentage'.</li>
 *       <li><b>outputAsFraction<b>: if true, the output will be a short fraction
 *           string, eg. "0.34". The default value is false, which produces a percentile output, eg.
 *           "34.5%".</li>
 * </ul>
 */
public class PercentageTransformer
    implements PropertiesResultTransformer
{
    public void configure (Configuration config)
        throws ConfigurationException
    {
        _numerators = config.getStringArray("inputOver");
        if (_numerators == null) {
            throw new ConfigurationException(
                "Aggregator transform property 'inputOver' is required.");
        }
        _denominator = config.getString("inputUnder");
        if (_denominator == null) {
            throw new ConfigurationException(
                "Aggregator transform property 'inputUnder' is required.");
        }
        _outputs = config.getStringArray("outputField"); // potentially null
        if (_outputs != null && _outputs.length != _numerators.length) {
            throw new ConfigurationException(
                "Aggregator transform property 'outputField' is misconfigured.");
        }
        _format = config.getBoolean("outputAsFraction", false) ? FRACTION_FORMAT : PERCENT_FORMAT;
    }

    public boolean transform(EventData data)
    {
        float underNumber = new Float(data.get(_denominator).toString());
        for (int ii = 0; ii < _numerators.length; ii++)
        {
            float overNumber;
            if (data.containsKey(_numerators[ii])) {
                overNumber = new Float(data.get(_numerators[ii]).toString());
            } else {
                overNumber = 0f;
            }
            String outputField = (_outputs == null) ? "percentage" : _outputs[ii];

            if (underNumber == 0) {
                data.getData().put(outputField, "0");
            } else {
                try {
                    final String result = _format.format(overNumber / underNumber);
                    data.getData().put(outputField, result);
                } catch (NumberFormatException e) {
                    data.getData().put(outputField, "NaN");
                }
            }
        }

        return true;
    }

    protected String[] _numerators;
    protected String _denominator;
    protected String[] _outputs;
    protected DecimalFormat _format;

    protected static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#,##0.00%");
    protected static final DecimalFormat FRACTION_FORMAT = new DecimalFormat("#0.0000");
}
