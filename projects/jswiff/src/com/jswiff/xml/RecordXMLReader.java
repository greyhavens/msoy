/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2006 Ralf Terdic (contact@jswiff.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.jswiff.xml;

import com.jswiff.swfrecords.AlignmentZone;
import com.jswiff.swfrecords.AlphaBitmapData;
import com.jswiff.swfrecords.AlphaColorMapData;
import com.jswiff.swfrecords.BevelFilter;
import com.jswiff.swfrecords.BitmapData;
import com.jswiff.swfrecords.BitmapPixelData;
import com.jswiff.swfrecords.BlurFilter;
import com.jswiff.swfrecords.ButtonCondAction;
import com.jswiff.swfrecords.ButtonRecord;
import com.jswiff.swfrecords.CXform;
import com.jswiff.swfrecords.CXformWithAlpha;
import com.jswiff.swfrecords.ClipActionRecord;
import com.jswiff.swfrecords.ClipActions;
import com.jswiff.swfrecords.ClipEventFlags;
import com.jswiff.swfrecords.Color;
import com.jswiff.swfrecords.ColorMapData;
import com.jswiff.swfrecords.ColorMatrixFilter;
import com.jswiff.swfrecords.ConvolutionFilter;
import com.jswiff.swfrecords.CurvedEdgeRecord;
import com.jswiff.swfrecords.DropShadowFilter;
import com.jswiff.swfrecords.EnhancedStrokeStyle;
import com.jswiff.swfrecords.FillStyle;
import com.jswiff.swfrecords.FillStyleArray;
import com.jswiff.swfrecords.FocalGradient;
import com.jswiff.swfrecords.FocalMorphGradient;
import com.jswiff.swfrecords.GlowFilter;
import com.jswiff.swfrecords.GlyphEntry;
import com.jswiff.swfrecords.GradRecord;
import com.jswiff.swfrecords.Gradient;
import com.jswiff.swfrecords.GradientBevelFilter;
import com.jswiff.swfrecords.GradientGlowFilter;
import com.jswiff.swfrecords.LineStyle;
import com.jswiff.swfrecords.LineStyle2;
import com.jswiff.swfrecords.LineStyleArray;
import com.jswiff.swfrecords.Matrix;
import com.jswiff.swfrecords.MorphFillStyle;
import com.jswiff.swfrecords.MorphFillStyles;
import com.jswiff.swfrecords.MorphGradRecord;
import com.jswiff.swfrecords.MorphGradient;
import com.jswiff.swfrecords.MorphLineStyle;
import com.jswiff.swfrecords.MorphLineStyle2;
import com.jswiff.swfrecords.MorphLineStyles;
import com.jswiff.swfrecords.Pix15;
import com.jswiff.swfrecords.Pix24;
import com.jswiff.swfrecords.RGB;
import com.jswiff.swfrecords.RGBA;
import com.jswiff.swfrecords.Rect;
import com.jswiff.swfrecords.Shape;
import com.jswiff.swfrecords.ShapeRecord;
import com.jswiff.swfrecords.ShapeWithStyle;
import com.jswiff.swfrecords.SoundEnvelope;
import com.jswiff.swfrecords.SoundInfo;
import com.jswiff.swfrecords.StraightEdgeRecord;
import com.jswiff.swfrecords.StyleChangeRecord;
import com.jswiff.swfrecords.TextRecord;
import com.jswiff.swfrecords.ZlibBitmapData;
import com.jswiff.swfrecords.actions.Action;
import com.jswiff.swfrecords.actions.ActionBlock;
import com.jswiff.util.Base64;

import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


class RecordXMLReader {
  static boolean getBooleanAttribute(String name, Element parentElement) {
    // boolean attributes are written only for "true" values
    Attribute attribute = parentElement.attribute(name);
    return ((attribute != null) &&
    attribute.getValue().equalsIgnoreCase("true"));
  }

  static int getCharacterId(Element parentElement) {
    return getIntAttribute("charid", parentElement);
  }

  static byte[] getDataElement(String elementName, Element parentElement) {
    Element element = parentElement.element(elementName);
    if (element == null) {
      throw new MissingElementException(elementName, parentElement.getPath());
    }
    return Base64.decode(element.getText());
  }

  static double getDoubleAttribute(String attributeName, Element parentElement) {
    return Double.parseDouble(getStringAttribute(attributeName, parentElement));
  }

  static Element getElement(String elementName, Element parentElement) {
    Element result = parentElement.element(elementName);
    if (result == null) {
      throw new MissingElementException(elementName, parentElement.getPath());
    }
    return result;
  }

  static float getFloatAttribute(String attributeName, Element parentElement) {
    return Float.parseFloat(getStringAttribute(attributeName, parentElement));
  }

  static int getIntAttribute(String attributeName, Element parentElement) {
    return Integer.parseInt(getStringAttribute(attributeName, parentElement));
  }

  static long getLongAttribute(String attributeName, Element parentElement) {
    return Long.parseLong(getStringAttribute(attributeName, parentElement));
  }

  static short getShortAttribute(String attributeName, Element parentElement) {
    return Short.parseShort(getStringAttribute(attributeName, parentElement));
  }

  static String getStringAttribute(String attributeName, Element parentElement) {
    Attribute attribute = parentElement.attribute(attributeName);
    if (attribute == null) {
      throw new MissingAttributeException(
        attributeName, parentElement.getPath());
    }
    return attribute.getValue();
  }

  static void readActionBlock(ActionBlock actionBlock, Element parentElement) {
    Element blockElement = getElement("actionblock", parentElement);
    List actionElements  = blockElement.elements();
    for (Iterator it = actionElements.iterator(); it.hasNext();) {
      Element actionElement = (Element) it.next();
      Action action         = ActionXMLReader.readAction(actionElement);
      actionBlock.addAction(action);
    }
  }

  static AlignmentZone[] readAlignmentZones(Element parentElement) {
    Element zonesElement  = getElement("zones", parentElement);
    List zoneElements     = zonesElement.elements("zone");
    int zonesCount        = zoneElements.size();
    AlignmentZone[] zones = new AlignmentZone[zonesCount];
    for (int i = 0; i < zonesCount; i++) {
      Element zoneElement     = (Element) zoneElements.get(i);
      AlignmentZone zone      = new AlignmentZone();
      Attribute leftAttribute = zoneElement.attribute("left");
      if (leftAttribute != null) {
        float left  = Float.parseFloat(leftAttribute.getValue());
        float width = getFloatAttribute("width", zoneElement);
        zone.setX(left, width);
      }
      Attribute baselineAttribute = zoneElement.attribute("baseline");
      if (baselineAttribute != null) {
        float baseline = Float.parseFloat(baselineAttribute.getValue());
        float height   = getFloatAttribute("height", zoneElement);
        zone.setY(baseline, height);
      }
      zones[i] = zone;
    }
    return zones;
  }

  static ButtonCondAction[] readButtonCondActions(Element parentElement) {
    Element actionsElement = parentElement.element("actions");
    if (actionsElement != null) {
      List actions                   = actionsElement.elements();
      int arraySize                  = actions.size();
      ButtonCondAction[] actionArray = new ButtonCondAction[arraySize];
      for (int i = 0; i < arraySize; i++) {
        ButtonCondAction action = new ButtonCondAction();
        Element actionElement   = (Element) actions.get(i);
        if (getBooleanAttribute("outdowntoidle", actionElement)) {
          action.setOutDownToIdle();
        }
        if (getBooleanAttribute("outdowntooverdown", actionElement)) {
          action.setOutDownToOverDown();
        }
        if (getBooleanAttribute("idletooverdown", actionElement)) {
          action.setIdleToOverDown();
        }
        if (getBooleanAttribute("idletooverup", actionElement)) {
          action.setIdleToOverUp();
        }
        if (getBooleanAttribute("overdowntoidle", actionElement)) {
          action.setOverDownToIdle();
        }
        if (getBooleanAttribute("overdowntooutdown", actionElement)) {
          action.setOverDownToOutDown();
        }
        if (getBooleanAttribute("overdowntooverup", actionElement)) {
          action.setOverDownToOverUp();
        }
        if (getBooleanAttribute("overuptoidle", actionElement)) {
          action.setOverUpToIdle();
        }
        if (getBooleanAttribute("overuptooverdown", actionElement)) {
          action.setOverUpToOverDown();
        }
        Attribute keyPress = actionElement.attribute("keypress");
        if (keyPress != null) {
          action.setKeyPress(Byte.parseByte(keyPress.getValue()));
        }
        readActionBlock(action.getActions(), actionElement);
        actionArray[i] = action;
      }
      return actionArray;
    }
    return null;
  }

  static ButtonRecord[] readButtonRecords(Element parentElement) {
    Element charsElement   = getElement("chars", parentElement);
    List recordElements    = charsElement.elements();
    int arrayLength        = recordElements.size();
    ButtonRecord[] records = new ButtonRecord[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element recordElement  = (Element) recordElements.get(i);
      int charId             = getIntAttribute("charid", recordElement);
      int depth              = getIntAttribute("depth", recordElement);
      Element state          = getElement("state", recordElement);
      boolean up             = getBooleanAttribute("up", state);
      boolean down           = getBooleanAttribute("down", state);
      boolean over           = getBooleanAttribute("over", state);
      boolean hit            = getBooleanAttribute("hit", state);
      Matrix placeMatrix     = readMatrix("matrix", recordElement);
      ButtonRecord record    = new ButtonRecord(
          charId, depth, placeMatrix, up, over, down, hit);
      Element colorTransform = recordElement.element("cxformwithalpha");
      if (colorTransform != null) {
        record.setColorTransform(readCXformWithAlpha(colorTransform));
      }
      records[i] = record;
    }
    return records;
  }

  static CXform readCXform(Element element) {
    CXform colorTransform = new CXform();
    Element add           = element.element("add");
    if (add != null) {
      int redAdd   = getIntAttribute("r", add);
      int greenAdd = getIntAttribute("g", add);
      int blueAdd  = getIntAttribute("b", add);
      colorTransform.setAddTerms(redAdd, greenAdd, blueAdd);
    }
    Element mult = element.element("mult");
    if (mult != null) {
      int redMult   = getIntAttribute("r", mult);
      int greenMult = getIntAttribute("g", mult);
      int blueMult  = getIntAttribute("b", mult);
      colorTransform.setMultTerms(redMult, greenMult, blueMult);
    }
    return colorTransform;
  }

  static CXformWithAlpha readCXformWithAlpha(Element element) {
    CXformWithAlpha colorTransform = new CXformWithAlpha();
    Element add                    = element.element("add");
    if (add != null) {
      int redAdd   = getIntAttribute("r", add);
      int greenAdd = getIntAttribute("g", add);
      int blueAdd  = getIntAttribute("b", add);
      int alphaAdd = getIntAttribute("a", add);
      colorTransform.setAddTerms(redAdd, greenAdd, blueAdd, alphaAdd);
    }
    Element mult = element.element("mult");
    if (mult != null) {
      int redMult   = getIntAttribute("r", mult);
      int greenMult = getIntAttribute("g", mult);
      int blueMult  = getIntAttribute("b", mult);
      int alphaMult = getIntAttribute("a", mult);
      colorTransform.setMultTerms(redMult, greenMult, blueMult, alphaMult);
    }
    return colorTransform;
  }

  static ClipActions readClipActions(Element element) {
    ClipEventFlags eventFlags = readClipEventFlags(element);
    List recordElements       = element.elements("clipactionrecord");
    List records              = new ArrayList();
    for (Iterator it = recordElements.iterator(); it.hasNext();) {
      Element recordElement   = (Element) it.next();
      ClipActionRecord record = new ClipActionRecord(
          readClipEventFlags(recordElement));
      Attribute keyCode       = recordElement.attribute("keycode");
      if (keyCode != null) {
        record.setKeyCode(Short.parseShort(keyCode.getValue()));
      }
      readActionBlock(record.getActions(), recordElement);
      records.add(record);
    }
    return new ClipActions(eventFlags, records);
  }

  static Color readColor(Element element) {
    if (element.attribute("a") == null) {
      return readRGB(element);
    }
    return readRGBA(element);
  }

  static List readFilters(Element element) {
    List filterElements = element.elements();
    int filterCount     = filterElements.size();
    List filters        = new ArrayList(filterCount);
    for (int j = 0; j < filterCount; j++) {
      Element filterElement = (Element) filterElements.get(j);
      if (filterElement.getName().equals("colormatrix")) {
        List valElements = filterElement.elements("val");
        int valCount     = valElements.size();
        if (valCount != 20) {
          throw new MalformedElementException(
            "colormatrix must contain 20 values!");
        }
        float[] matrix = new float[20];
        for (int i = 0; i < 20; i++) {
          Element valElement = (Element) valElements.get(i);
          matrix[i] = Float.parseFloat(valElement.getText());
        }
        ColorMatrixFilter colorMatrixFilter = new ColorMatrixFilter(matrix);
        filters.add(colorMatrixFilter);
      } else if (filterElement.getName().equals("convolution")) {
        Element matrixElement = filterElement.element("matrix");
        List valElements      = matrixElement.elements("val");
        float[] matrix        = new float[valElements.size()];
        for (int i = 0; i < matrix.length; i++) {
          Element valElement = (Element) valElements.get(i);
          matrix[i] = Float.parseFloat(valElement.getText());
        }
        int matrixRows                      = getIntAttribute(
            "rows", matrixElement);
        ConvolutionFilter convolutionFilter = new ConvolutionFilter(
            matrix, matrixRows);
        convolutionFilter.setColor(
          readRGBA(getElement("color", filterElement)));
        convolutionFilter.setDivisor(
          getFloatAttribute("divisor", filterElement));
        convolutionFilter.setBias(getFloatAttribute("bias", filterElement));
        convolutionFilter.setClamp(getBooleanAttribute("clamp", filterElement));
        convolutionFilter.setPreserveAlpha(
          getBooleanAttribute("preservealpha", filterElement));
        filters.add(convolutionFilter);
      } else if (filterElement.getName().equals("blur")) {
        BlurFilter blurFilter = new BlurFilter(
            getDoubleAttribute("x", filterElement),
            getDoubleAttribute("y", filterElement));
        blurFilter.setQuality(getIntAttribute("quality", filterElement));
        filters.add(blurFilter);
      } else if (filterElement.getName().equals("dropshadow")) {
        DropShadowFilter dropShadowFilter = new DropShadowFilter();
        dropShadowFilter.setColor(readRGBA(getElement("color", filterElement)));
        dropShadowFilter.setX(getDoubleAttribute("x", filterElement));
        dropShadowFilter.setY(getDoubleAttribute("y", filterElement));
        dropShadowFilter.setAngle(getDoubleAttribute("angle", filterElement));
        dropShadowFilter.setDistance(
          getDoubleAttribute("distance", filterElement));
        dropShadowFilter.setStrength(
          getDoubleAttribute("strength", filterElement));
        dropShadowFilter.setQuality(getIntAttribute("quality", filterElement));
        dropShadowFilter.setInner(getBooleanAttribute("inner", filterElement));
        dropShadowFilter.setKnockout(
          getBooleanAttribute("knockout", filterElement));
        dropShadowFilter.setHideObject(
          getBooleanAttribute("hideobject", filterElement));
        filters.add(dropShadowFilter);
      } else if (filterElement.getName().equals("glow")) {
        GlowFilter glowFilter = new GlowFilter();
        glowFilter.setColor(readRGBA(getElement("color", filterElement)));
        glowFilter.setX(getDoubleAttribute("x", filterElement));
        glowFilter.setY(getDoubleAttribute("y", filterElement));
        glowFilter.setStrength(getDoubleAttribute("strength", filterElement));
        glowFilter.setQuality(getIntAttribute("quality", filterElement));
        glowFilter.setInner(getBooleanAttribute("inner", filterElement));
        glowFilter.setKnockout(getBooleanAttribute("knockout", filterElement));
        filters.add(glowFilter);
      } else if (filterElement.getName().equals("bevel")) {
        BevelFilter bevelFilter = new BevelFilter();
        bevelFilter.setHighlightColor(
          readRGBA(getElement("highlightcolor", filterElement)));
        bevelFilter.setShadowColor(
          readRGBA(getElement("shadowcolor", filterElement)));
        bevelFilter.setX(getDoubleAttribute("x", filterElement));
        bevelFilter.setY(getDoubleAttribute("y", filterElement));
        bevelFilter.setAngle(getDoubleAttribute("angle", filterElement));
        bevelFilter.setDistance(getDoubleAttribute("distance", filterElement));
        bevelFilter.setStrength(getDoubleAttribute("strength", filterElement));
        bevelFilter.setQuality(getIntAttribute("quality", filterElement));
        bevelFilter.setInner(getBooleanAttribute("inner", filterElement));
        bevelFilter.setKnockout(getBooleanAttribute("knockout", filterElement));
        bevelFilter.setOnTop(getBooleanAttribute("ontop", filterElement));
        filters.add(bevelFilter);
      } else if (filterElement.getName().equals("gradientglow")) {
        Element pointsElement  = getElement("controlpoints", filterElement);
        List pointElements     = pointsElement.elements("controlpoint");
        int controlPointsCount = pointElements.size();
        RGBA[] colors          = new RGBA[controlPointsCount];
        short[] ratios         = new short[controlPointsCount];
        for (int i = 0; i < controlPointsCount; i++) {
          Element pointElement = (Element) pointElements.get(i);
          colors[i]   = readRGBA(getElement("color", pointElement));
          ratios[i]   = getShortAttribute("ratio", pointElement);
        }
        GradientGlowFilter gradientGlowFilter = new GradientGlowFilter(
            colors, ratios);
        gradientGlowFilter.setX(getDoubleAttribute("x", filterElement));
        gradientGlowFilter.setY(getDoubleAttribute("y", filterElement));
        gradientGlowFilter.setAngle(getDoubleAttribute("angle", filterElement));
        gradientGlowFilter.setDistance(
          getDoubleAttribute("distance", filterElement));
        gradientGlowFilter.setStrength(
          getDoubleAttribute("strength", filterElement));
        gradientGlowFilter.setQuality(
          getIntAttribute("quality", filterElement));
        gradientGlowFilter.setInner(
          getBooleanAttribute("inner", filterElement));
        gradientGlowFilter.setKnockout(
          getBooleanAttribute("knockout", filterElement));
        gradientGlowFilter.setOnTop(
          getBooleanAttribute("ontop", filterElement));
        filters.add(gradientGlowFilter);
      } else if (filterElement.getName().equals("gradientbevel")) {
        Element pointsElement  = getElement("controlpoints", filterElement);
        List pointElements     = pointsElement.elements("controlpoint");
        int controlPointsCount = pointElements.size();
        RGBA[] colors          = new RGBA[controlPointsCount];
        short[] ratios         = new short[controlPointsCount];
        for (int i = 0; i < controlPointsCount; i++) {
          Element pointElement = (Element) pointElements.get(i);
          colors[i]   = readRGBA(getElement("color", pointElement));
          ratios[i]   = getShortAttribute("ratio", pointElement);
        }
        GradientBevelFilter gradientBevelFilter = new GradientBevelFilter(
            colors, ratios);
        gradientBevelFilter.setX(getDoubleAttribute("x", filterElement));
        gradientBevelFilter.setY(getDoubleAttribute("y", filterElement));
        gradientBevelFilter.setAngle(
          getDoubleAttribute("angle", filterElement));
        gradientBevelFilter.setDistance(
          getDoubleAttribute("distance", filterElement));
        gradientBevelFilter.setStrength(
          getDoubleAttribute("strength", filterElement));
        gradientBevelFilter.setQuality(
          getIntAttribute("quality", filterElement));
        gradientBevelFilter.setInner(
          getBooleanAttribute("inner", filterElement));
        gradientBevelFilter.setKnockout(
          getBooleanAttribute("knockout", filterElement));
        gradientBevelFilter.setOnTop(
          getBooleanAttribute("ontop", filterElement));
        filters.add(gradientBevelFilter);
      }
    }
    return filters;
  }

  static Matrix readMatrix(Element element) {
    Element translate  = getElement("translate", element);
    int translateX     = getIntAttribute("x", translate);
    int translateY     = getIntAttribute("y", translate);
    Matrix matrix      = new Matrix(translateX, translateY);
    Element rotateSkew = element.element("rotateskew");
    if (rotateSkew != null) {
      matrix.setRotateSkew(
        getDoubleAttribute("rs0", rotateSkew),
        getDoubleAttribute("rs1", rotateSkew));
    }
    Element scale = element.element("scale");
    if (scale != null) {
      matrix.setScale(
        getDoubleAttribute("x", scale), getDoubleAttribute("y", scale));
    }
    return matrix;
  }

  static Matrix readMatrix(String elementName, Element parentElement) {
    Element matrixElement = getElement(elementName, parentElement);
    return readMatrix(matrixElement);
  }

  static MorphFillStyle readMorphFillStyle(Element element) {
    Element startElement = getElement("start", element);
    Element endElement   = getElement("end", element);
    String type          = getStringAttribute("type", element);
    if (type.equals("solid")) {
      RGBA startColor = readRGBA(getElement("color", startElement));
      RGBA endColor   = readRGBA(getElement("color", endElement));
      return new MorphFillStyle(startColor, endColor);
    } else if (type.equals("lineargradient")) {
      return new MorphFillStyle(
        readMorphGradient(element), readMatrix("gradientmatrix", startElement),
        readMatrix("gradientmatrix", endElement),
        MorphFillStyle.TYPE_LINEAR_GRADIENT);
    } else if (type.equals("focalradialgradient")) {
      return new MorphFillStyle(
        readMorphGradient(element), readMatrix("gradientmatrix", startElement),
        readMatrix("gradientmatrix", endElement),
        MorphFillStyle.TYPE_FOCAL_RADIAL_GRADIENT);
    } else if (type.equals("radialgradient")) {
      return new MorphFillStyle(
        readMorphGradient(element), readMatrix("gradientmatrix", startElement),
        readMatrix("gradientmatrix", endElement),
        MorphFillStyle.TYPE_RADIAL_GRADIENT);
    } else if (type.equals("tiledbitmap")) {
      return new MorphFillStyle(
        getIntAttribute("bitmapid", element),
        readMatrix("bitmapmatrix", startElement),
        readMatrix("bitmapmatrix", endElement), MorphFillStyle.TYPE_TILED_BITMAP);
    } else if (type.equals("clippedbitmap")) {
      return new MorphFillStyle(
        getIntAttribute("bitmapid", element),
        readMatrix("bitmapmatrix", startElement),
        readMatrix("bitmapmatrix", endElement),
        MorphFillStyle.TYPE_CLIPPED_BITMAP);
    } else if (type.equals("nonsmoothedtiledbitmap")) {
      return new MorphFillStyle(
        getIntAttribute("bitmapid", element),
        readMatrix("bitmapmatrix", startElement),
        readMatrix("bitmapmatrix", endElement),
        MorphFillStyle.TYPE_NONSMOOTHED_TILED_BITMAP);
    } else if (type.equals("nonsmoothedclippedbitmap")) {
      return new MorphFillStyle(
        getIntAttribute("bitmapid", element),
        readMatrix("bitmapmatrix", startElement),
        readMatrix("bitmapmatrix", endElement),
        MorphFillStyle.TYPE_NONSMOOTHED_CLIPPED_BITMAP);
    } else {
      throw new IllegalArgumentException("Illegal morph fill type: " + type);
    }
  }

  static MorphFillStyles readMorphFillStyles(Element parentElement) {
    Element element        = getElement("morphfillstyles", parentElement);
    List styleElements     = element.elements();
    MorphFillStyles styles = new MorphFillStyles();
    for (Iterator it = styleElements.iterator(); it.hasNext();) {
      Element styleElement = (Element) it.next();
      styles.addStyle(readMorphFillStyle(styleElement));
    }
    return styles;
  }

  static MorphLineStyles readMorphLineStyles(Element parentElement) {
    Element element        = getElement("morphlinestyles", parentElement);
    MorphLineStyles styles = new MorphLineStyles();
    List styleElements     = element.elements();
    for (Iterator it = styleElements.iterator(); it.hasNext();) {
      Element styleElement = (Element) it.next();
      if (styleElement.getName().equals("morphlinestyle")) {
        styles.addStyle(readMorphLineStyle(styleElement));
      } else if (styleElement.getName().equals("morphlinestyle2")) {
        styles.addStyle(readMorphLineStyle2(styleElement));
      }
    }
    return styles;
  }

  static RGB readRGB(Element element) {
    short red   = getShortAttribute("r", element);
    short green = getShortAttribute("g", element);
    short blue  = getShortAttribute("b", element);
    return new RGB(red, green, blue);
  }

  static RGBA readRGBA(Element element) {
    short red   = getShortAttribute("r", element);
    short green = getShortAttribute("g", element);
    short blue  = getShortAttribute("b", element);
    short alpha = getShortAttribute("a", element);
    return new RGBA(red, green, blue, alpha);
  }

  static Rect readRect(Element element) {
    long xMin = getLongAttribute("xmin", element);
    long xMax = getLongAttribute("xmax", element);
    long yMin = getLongAttribute("ymin", element);
    long yMax = getLongAttribute("ymax", element);
    return new Rect(xMin, xMax, yMin, yMax);
  }

  static Shape readShape(Element element) {
    List shapeRecordElements   = element.elements();
    int arrayLength            = shapeRecordElements.size();
    ShapeRecord[] shapeRecords = new ShapeRecord[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element shapeRecordElement = (Element) shapeRecordElements.get(i);
      if (shapeRecordElement.getName().equals("straightedgerecord")) {
        shapeRecords[i] = readStraightEdgeRecord(shapeRecordElement);
      } else if (shapeRecordElement.getName().equals("curvededgerecord")) {
        shapeRecords[i] = readCurvedEdgeRecord(shapeRecordElement);
      } else if (shapeRecordElement.getName().equals("stylechangerecord")) {
        shapeRecords[i] = readStyleChangeRecord(shapeRecordElement);
      }
    }
    return new Shape(shapeRecords);
  }

  static ShapeWithStyle readShapeWithStyle(Element parentElement) {
    Element element            = getElement("shapewithstyle", parentElement);
    Element shape              = getElement("shape", element);
    List shapeRecordElements   = shape.elements();
    int arrayLength            = shapeRecordElements.size();
    ShapeRecord[] shapeRecords = new ShapeRecord[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element shapeRecordElement = (Element) shapeRecordElements.get(i);
      if (shapeRecordElement.getName().equals("straightedgerecord")) {
        shapeRecords[i] = readStraightEdgeRecord(shapeRecordElement);
      } else if (shapeRecordElement.getName().equals("curvededgerecord")) {
        shapeRecords[i] = readCurvedEdgeRecord(shapeRecordElement);
      } else if (shapeRecordElement.getName().equals("stylechangerecord")) {
        shapeRecords[i] = readStyleChangeRecord(shapeRecordElement);
      }
    }
    Element lineStyles = element.element("linestyles");
    Element fillStyles = element.element("fillstyles");
    return new ShapeWithStyle(
      readFillStyles(fillStyles), readLineStyles(lineStyles), shapeRecords);
  }

  static SoundInfo readSoundInfo(Element parentElement) {
    Element element     = getElement("soundinfo", parentElement);
    SoundInfo soundInfo = new SoundInfo();
    if (getBooleanAttribute("syncstop", element)) {
      soundInfo.setSyncStop();
    }
    if (getBooleanAttribute("syncnomultiple", element)) {
      soundInfo.setSyncNoMultiple();
    }
    Attribute loopCount = element.attribute("loopcount");
    if (loopCount != null) {
      soundInfo.setLoopCount(Integer.parseInt(loopCount.getValue()));
    }
    Attribute inPoint = element.attribute("inpoint");
    if (inPoint != null) {
      soundInfo.setInPoint(Integer.parseInt(inPoint.getValue()));
    }
    Element envelopeRecordsElement = element.element("enveloperecords");
    if (envelopeRecordsElement != null) {
      soundInfo.setEnvelopeRecords(
        readSoundEnvelopeRecords(envelopeRecordsElement));
    }
    return soundInfo;
  }

  static TextRecord[] readTextRecords(Element parentElement) {
    Element element      = getElement("textrecords", parentElement);
    List recordElements  = element.elements();
    int arrayLength      = recordElements.size();
    TextRecord[] records = new TextRecord[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element recordElement     = (Element) recordElements.get(i);
      GlyphEntry[] glyphEntries = readGlyphEntries(recordElement);
      TextRecord record         = new TextRecord(glyphEntries);
      Attribute fontId          = recordElement.attribute("fontid");
      if (fontId != null) {
        record.setFont(
          Integer.parseInt(fontId.getValue()),
          getIntAttribute("height", recordElement));
      }
      Attribute xOffset = recordElement.attribute("xoffset");
      if (xOffset != null) {
        record.setXOffset(Short.parseShort(xOffset.getValue()));
      }
      Attribute yOffset = recordElement.attribute("yoffset");
      if (yOffset != null) {
        record.setYOffset(Short.parseShort(yOffset.getValue()));
      }
      Element color = recordElement.element("color");
      if (color != null) {
        record.setTextColor(RecordXMLReader.readColor(color));
      }
      records[i] = record;
    }
    return records;
  }

  static ZlibBitmapData readZlibBitmapData(Element parentElement) {
    // transparency not supported
    Element element = parentElement.element("bitmapdata");
    if (element != null) {
      return new BitmapData(readBitmapPixelData(element));
    }
    element = parentElement.element("colormapdata");
    if (element != null) {
      Element colorTableElement = getElement("colortable", element);
      RGB[] colorTable          = readRGBArray(colorTableElement);
      Element pixelDataElement  = getElement("pixeldata", element);
      short[] pixelData         = Base64.decodeUnsigned(
          pixelDataElement.getText());
      return new ColorMapData(colorTable, pixelData);
    }
    throw new MissingNodeException(
      "Element " + parentElement.getPath() +
      " must contain either a bitmapdata or a colormapdata element!");
  }

  static ZlibBitmapData readZlibBitmapData2(Element parentElement) {
    // transparency supported
    Element element = parentElement.element("alphabitmapdata");
    if (element != null) {
      return new AlphaBitmapData(readRGBAArray(element));
    }
    element = parentElement.element("alphacolormapdata");
    if (element != null) {
      Element colorTableElement = getElement("colortable", element);
      RGBA[] colorTable         = readRGBAArray(colorTableElement);
      Element pixelDataElement  = getElement("pixeldata", element);
      short[] pixelData         = Base64.decodeUnsigned(
          pixelDataElement.getText());
      return new AlphaColorMapData(colorTable, pixelData);
    }
    throw new MissingNodeException(
      "Element " + parentElement.getPath() +
      " must contain either an alphabitmapdata or an alphacolormapdata element!");
  }

  private static BitmapPixelData[] readBitmapPixelData(Element parentElement) {
    Element element = parentElement.element("pix15array");
    if (element != null) {
      byte[] buffer   = Base64.decode(element.getText());
      int arrayLength = buffer.length / 3;
      Pix15[] array   = new Pix15[arrayLength];
      for (int i = 0; i < arrayLength; i++) {
        int j = i * 3;
        array[i] = new Pix15(buffer[j], buffer[j + 1], buffer[j + 2]);
      }
      return array;
    }
    element = parentElement.element("pix24array");
    if (element != null) {
      short[] buffer  = Base64.decodeUnsigned(element.getText());
      int arrayLength = buffer.length / 3;
      Pix24[] array   = new Pix24[arrayLength];
      for (int i = 0; i < arrayLength; i++) {
        int j = i * 3;
        array[i] = new Pix24(buffer[j], buffer[j + 1], buffer[j + 2]);
      }
      return array;
    }
    throw new MissingNodeException(
      "Element " + parentElement.getPath() +
      " must contain either a pix15array or a pix24array element!");
  }

  private static byte readCapStyle(String attrName, Element element) {
    String capStyle = getStringAttribute(attrName, element);
    if (capStyle.equals("none")) {
      return EnhancedStrokeStyle.CAPS_NONE;
    }
    if (capStyle.equals("round")) {
      return EnhancedStrokeStyle.CAPS_ROUND;
    }
    if (capStyle.equals("square")) {
      return EnhancedStrokeStyle.CAPS_SQUARE;
    }
    return -1;
  }

  private static ClipEventFlags readClipEventFlags(Element parentElement) {
    Element element               = getElement("clipeventflags", parentElement);
    ClipEventFlags clipEventFlags = new ClipEventFlags();
    if (getBooleanAttribute("keyup", element)) {
      clipEventFlags.setKeyUp();
    }
    if (getBooleanAttribute("keydown", element)) {
      clipEventFlags.setKeyDown();
    }
    if (getBooleanAttribute("keypress", element)) {
      clipEventFlags.setKeyPress();
    }
    if (getBooleanAttribute("mouseup", element)) {
      clipEventFlags.setMouseUp();
    }
    if (getBooleanAttribute("mousedown", element)) {
      clipEventFlags.setMouseDown();
    }
    if (getBooleanAttribute("mousemove", element)) {
      clipEventFlags.setMouseMove();
    }
    if (getBooleanAttribute("load", element)) {
      clipEventFlags.setLoad();
    }
    if (getBooleanAttribute("unload", element)) {
      clipEventFlags.setUnload();
    }
    if (getBooleanAttribute("enterframe", element)) {
      clipEventFlags.setEnterFrame();
    }
    if (getBooleanAttribute("dragover", element)) {
      clipEventFlags.setDragOver();
    }
    if (getBooleanAttribute("dragout", element)) {
      clipEventFlags.setDragOut();
    }
    if (getBooleanAttribute("rollover", element)) {
      clipEventFlags.setRollOver();
    }
    if (getBooleanAttribute("rollout", element)) {
      clipEventFlags.setRollOut();
    }
    if (getBooleanAttribute("releaseoutside", element)) {
      clipEventFlags.setReleaseOutside();
    }
    if (getBooleanAttribute("release", element)) {
      clipEventFlags.setRelease();
    }
    if (getBooleanAttribute("press", element)) {
      clipEventFlags.setPress();
    }
    if (getBooleanAttribute("initialize", element)) {
      clipEventFlags.setInitialize();
    }
    if (getBooleanAttribute("data", element)) {
      clipEventFlags.setData();
    }
    if (getBooleanAttribute("construct", element)) {
      clipEventFlags.setConstruct();
    }
    return clipEventFlags;
  }

  private static ShapeRecord readCurvedEdgeRecord(Element element) {
    int anchorDeltaX  = getIntAttribute("anchordx", element);
    int anchorDeltaY  = getIntAttribute("anchordy", element);
    int controlDeltaX = getIntAttribute("controldx", element);
    int controlDeltaY = getIntAttribute("controldy", element);
    return new CurvedEdgeRecord(
      controlDeltaX, controlDeltaY, anchorDeltaX, anchorDeltaY);
  }

  private static FillStyle readFillStyle(Element element) {
    String type = getStringAttribute("type", element);
    if (type.equals("solid")) {
      Color color = readColor(getElement("color", element));
      return new FillStyle(color);
    } else if (type.equals("lineargradient")) {
      return new FillStyle(
        readGradient(element), readMatrix("gradientmatrix", element),
        FillStyle.TYPE_LINEAR_GRADIENT);
    } else if (type.equals("focalradialgradient")) {
      return new FillStyle(
        readGradient(element), readMatrix("gradientmatrix", element),
        FillStyle.TYPE_FOCAL_RADIAL_GRADIENT);
    } else if (type.equals("radialgradient")) {
      return new FillStyle(
        readGradient(element), readMatrix("gradientmatrix", element),
        FillStyle.TYPE_RADIAL_GRADIENT);
    } else if (type.equals("tiledbitmap")) {
      return new FillStyle(
        getIntAttribute("bitmapid", element),
        readMatrix("bitmapmatrix", element), FillStyle.TYPE_TILED_BITMAP);
    } else if (type.equals("clippedbitmap")) {
      return new FillStyle(
        getIntAttribute("bitmapid", element),
        readMatrix("bitmapmatrix", element), FillStyle.TYPE_CLIPPED_BITMAP);
    } else if (type.equals("nonsmoothedtiledbitmap")) {
      return new FillStyle(
        getIntAttribute("bitmapid", element),
        readMatrix("bitmapmatrix", element),
        FillStyle.TYPE_NONSMOOTHED_TILED_BITMAP);
    } else if (type.equals("nonsmoothedclippedbitmap")) {
      return new FillStyle(
        getIntAttribute("bitmapid", element),
        readMatrix("bitmapmatrix", element),
        FillStyle.TYPE_NONSMOOTHED_CLIPPED_BITMAP);
    } else {
      throw new IllegalArgumentException("Illegal fill type: " + type);
    }
  }

  private static FillStyleArray readFillStyles(Element element) {
    List styleElements        = element.elements();
    FillStyleArray styleArray = new FillStyleArray();
    for (Iterator it = styleElements.iterator(); it.hasNext();) {
      Element styleElement = (Element) it.next();
      styleArray.addStyle(readFillStyle(styleElement));
    }
    return styleArray;
  }

  private static GlyphEntry[] readGlyphEntries(Element parentElement) {
    Element element      = getElement("glyphentries", parentElement);
    List entryElements   = element.elements();
    int arrayLength      = entryElements.size();
    GlyphEntry[] entries = new GlyphEntry[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element entryElement = (Element) entryElements.get(i);
      entries[i] = new GlyphEntry(
          getIntAttribute("index", entryElement),
          getIntAttribute("advance", entryElement));
    }
    return entries;
  }

  private static Gradient readGradient(Element parentElement) {
    Element element = parentElement.element("gradient");
    boolean focal   = false;
    if (element == null) {
      element   = getElement("focalgradient", parentElement);
      focal     = true;
    }
    List recordElements  = element.elements("gradrecord");
    int arrayLength      = recordElements.size();
    GradRecord[] records = new GradRecord[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element recordElement = (Element) recordElements.get(i);
      records[i] = new GradRecord(
          getShortAttribute("ratio", recordElement),
          readColor(getElement("color", recordElement)));
    }
    Gradient gradient;
    if (focal) {
      gradient = new FocalGradient(
          records, getDoubleAttribute("focalpointratio", element));
    } else {
      gradient = new Gradient(records);
    }
    gradient.setInterpolationMethod(readInterpolationMethod(element));
    gradient.setSpreadMethod(readSpreadMethod(element));
    return gradient;
  }

  private static byte readInterpolationMethod(Element element) {
    String interpolation = getStringAttribute("interpolation", element);
    if (interpolation.equals("linear-rgb")) {
      return Gradient.INTERPOLATION_LINEAR_RGB;
    } else if (interpolation.equals("rgb")) {
      return Gradient.INTERPOLATION_RGB;
    }
    return -1;
  }

  private static byte readJointStyle(Element element) {
    String jointStyle = getStringAttribute("joint", element);
    if (jointStyle.equals("round")) {
      return EnhancedStrokeStyle.JOINT_ROUND;
    }
    if (jointStyle.equals("miter")) {
      return EnhancedStrokeStyle.JOINT_ROUND;
    }
    if (jointStyle.equals("bevel")) {
      return EnhancedStrokeStyle.JOINT_ROUND;
    }
    return -1;
  }

  private static LineStyle readLineStyle(Element element) {
    int width   = getIntAttribute("width", element);
    Color color = readColor(getElement("color", element));
    return new LineStyle(width, color);
  }

  private static LineStyle2 readLineStyle2(Element element) {
    LineStyle2 lineStyle2 = new LineStyle2(getIntAttribute("width", element));
    byte jointStyle       = readJointStyle(element);
    lineStyle2.setJointStyle(jointStyle);
    if (jointStyle == EnhancedStrokeStyle.JOINT_MITER) {
      lineStyle2.setMiterLimit(getDoubleAttribute("miterlimit", element));
    }
    Element capStyleElement = getElement("capstyle", element);
    lineStyle2.setStartCapStyle(readCapStyle("start", capStyleElement));
    lineStyle2.setStartCapStyle(readCapStyle("end", capStyleElement));
    lineStyle2.setScaleStroke(readScaleStroke(element));
    lineStyle2.setClose(getBooleanAttribute("close", element));
    lineStyle2.setPixelHinting(getBooleanAttribute("pixelhinting", element));
    Element fillStyleElement = element.element("fillstyle");
    if (fillStyleElement == null) {
      Element colorElement = getElement("color", element);
      lineStyle2.setColor(readRGBA(colorElement));
    } else {
      lineStyle2.setFillStyle(readFillStyle(fillStyleElement));
    }
    return lineStyle2;
  }

  private static LineStyleArray readLineStyles(Element element) {
    LineStyleArray styleArray = new LineStyleArray();
    List styleElements        = element.elements();
    for (Iterator it = styleElements.iterator(); it.hasNext();) {
      Element styleElement = (Element) it.next();
      if (styleElement.getName().equals("linestyle")) {
        styleArray.addStyle(readLineStyle(styleElement));
      } else if (styleElement.getName().equals("linestyle2")) {
        styleArray.addStyle(readLineStyle2(styleElement));
      }
    }
    return styleArray;
  }

  private static MorphGradient readMorphGradient(Element parentElement) {
    Element element = parentElement.element("morphgradient");
    boolean focal   = false;
    if (element == null) {
      element   = getElement("focalmorphgradient", parentElement);
      focal     = true;
    }
    List recordElements       = element.elements("morphgradrecord");
    int arrayLength           = recordElements.size();
    MorphGradRecord[] records = new MorphGradRecord[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element recordElement = (Element) recordElements.get(i);
      Element start         = getElement("start", recordElement);
      short startRatio      = getShortAttribute("ratio", start);
      RGBA startColor       = readRGBA(getElement("color", start));
      Element end           = getElement("end", recordElement);
      short endRatio        = getShortAttribute("ratio", end);
      RGBA endColor         = readRGBA(getElement("color", end));
      records[i]            = new MorphGradRecord(
          startRatio, startColor, endRatio, endColor);
    }
    MorphGradient morphGradient;
    if (focal) {
      double startFocalpointratio = getDoubleAttribute(
          "startfocalpointratio", element);
      double endfocalpointratio   = getDoubleAttribute(
          "endfocalpointratio", element);
      morphGradient               = new FocalMorphGradient(
          records, startFocalpointratio, endfocalpointratio);
    } else {
      morphGradient = new MorphGradient(records);
    }
    morphGradient.setInterpolationMethod(readInterpolationMethod(element));
    morphGradient.setSpreadMethod(readSpreadMethod(element));
    return morphGradient;
  }

  private static MorphLineStyle readMorphLineStyle(Element element) {
    Element startElement = getElement("start", element);
    int startWidth       = getIntAttribute("width", startElement);
    RGBA startColor      = readRGBA(getElement("color", startElement));
    Element endElement   = getElement("end", element);
    int endWidth         = getIntAttribute("width", endElement);
    RGBA endColor        = readRGBA(getElement("color", endElement));
    return new MorphLineStyle(startWidth, startColor, endWidth, endColor);
  }

  private static MorphLineStyle2 readMorphLineStyle2(Element element) {
    Element startElement       = getElement("start", element);
    int startWidth             = getIntAttribute("width", startElement);
    Element endElement         = getElement("end", element);
    int endWidth               = getIntAttribute("width", endElement);
    MorphLineStyle2 lineStyle2;
    Element fillStyleElement   = element.element("morphfillstyle");
    if (fillStyleElement == null) {
      RGBA startColor = readRGBA(getElement("color", startElement));
      RGBA endColor   = readRGBA(getElement("color", endElement));
      lineStyle2      = new MorphLineStyle2(
          startWidth, startColor, endWidth, endColor);
    } else {
      MorphFillStyle fillStyle = readMorphFillStyle(fillStyleElement);
      lineStyle2 = new MorphLineStyle2(startWidth, endWidth, fillStyle);
    }
    byte jointStyle = readJointStyle(element);
    lineStyle2.setJointStyle(jointStyle);
    if (jointStyle == EnhancedStrokeStyle.JOINT_MITER) {
      lineStyle2.setMiterLimit(getDoubleAttribute("miterlimit", element));
    }
    Element capStyleElement = getElement("capstyle", element);
    lineStyle2.setStartCapStyle(readCapStyle("start", capStyleElement));
    lineStyle2.setStartCapStyle(readCapStyle("end", capStyleElement));
    lineStyle2.setScaleStroke(readScaleStroke(element));
    lineStyle2.setClose(getBooleanAttribute("close", element));
    lineStyle2.setPixelHinting(getBooleanAttribute("pixelhinting", element));
    return lineStyle2;
  }

  private static RGBA[] readRGBAArray(Element parentElement) {
    Element rgbaArrayElement = getElement("rgbaarray", parentElement);
    short[] buffer           = Base64.decodeUnsigned(
        rgbaArrayElement.getText());
    int arrayLength          = buffer.length / 4;
    RGBA[] rgbaArray         = new RGBA[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      int j = i * 4;
      rgbaArray[i] = new RGBA(
          buffer[j], buffer[j + 1], buffer[j + 2], buffer[j + 3]);
    }
    return rgbaArray;
  }

  private static RGB[] readRGBArray(Element parentElement) {
    Element rgbArrayElement = getElement("rgbarray", parentElement);
    short[] buffer          = Base64.decodeUnsigned(rgbArrayElement.getText());
    int arrayLength         = buffer.length / 3;
    RGB[] rgbArray          = new RGB[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      int j = i * 3;
      rgbArray[i] = new RGB(buffer[j], buffer[j + 1], buffer[j + 2]);
    }
    return rgbArray;
  }

  private static byte readScaleStroke(Element element) {
    String scaleStroke = getStringAttribute("scalestroke", element);
    if (scaleStroke.equals("none")) {
      return EnhancedStrokeStyle.SCALE_NONE;
    } else if (scaleStroke.equals("horizontal")) {
      return EnhancedStrokeStyle.SCALE_HORIZONTAL;
    } else if (scaleStroke.equals("vertical")) {
      return EnhancedStrokeStyle.SCALE_VERTICAL;
    } else if (scaleStroke.equals("both")) {
      return EnhancedStrokeStyle.SCALE_BOTH;
    }
    return -1;
  }

  private static SoundEnvelope[] readSoundEnvelopeRecords(
    Element parentElement) {
    List recordElements     = parentElement.elements();
    int arrayLength         = recordElements.size();
    SoundEnvelope[] records = new SoundEnvelope[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element recordElement = (Element) recordElements.get(i);
      long pos44            = getLongAttribute("pos44", recordElement);
      int leftLevel         = getIntAttribute("leftlevel", recordElement);
      int rightLevel        = getIntAttribute("rightlevel", recordElement);
      SoundEnvelope record  = new SoundEnvelope(pos44, leftLevel, rightLevel);
      records[i]            = record;
    }
    return records;
  }

  private static byte readSpreadMethod(Element element) {
    String spread = getStringAttribute("spread", element);
    if (spread.equals("pad")) {
      return Gradient.SPREAD_PAD;
    } else if (spread.equals("reflect")) {
      return Gradient.SPREAD_REFLECT;
    } else if (spread.equals("repeat")) {
      return Gradient.SPREAD_REPEAT;
    }
    return -1;
  }

  private static ShapeRecord readStraightEdgeRecord(Element element) {
    int deltaX = getIntAttribute("dx", element);
    int deltaY = getIntAttribute("dy", element);
    return new StraightEdgeRecord(deltaX, deltaY);
  }

  private static ShapeRecord readStyleChangeRecord(Element element) {
    StyleChangeRecord record = new StyleChangeRecord();
    Element moveTo           = element.element("moveto");
    if (moveTo != null) {
      record.setMoveTo(
        getIntAttribute("x", moveTo), getIntAttribute("y", moveTo));
    }
    Element styles = element.element("styles");
    if (styles != null) {
      Attribute line = styles.attribute("line");
      if (line != null) {
        record.setLineStyle(Integer.parseInt(line.getValue()));
      }
      Attribute fill0 = styles.attribute("fill0");
      if (fill0 != null) {
        record.setFillStyle0(Integer.parseInt(fill0.getValue()));
      }
      Attribute fill1 = styles.attribute("fill1");
      if (fill1 != null) {
        record.setFillStyle1(Integer.parseInt(fill1.getValue()));
      }
      Element newLineStyles = styles.element("linestyles");
      if (newLineStyles != null) {
        // new line styles always come together with new fill styles
        Element newFillStyles = getElement("fillstyles", styles);
        record.setNewStyles(
          readLineStyles(newLineStyles), readFillStyles(newFillStyles));
      }
    }
    return record;
  }
}
