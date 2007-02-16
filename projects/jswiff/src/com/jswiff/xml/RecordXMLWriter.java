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
import com.jswiff.swfrecords.BlendMode;
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
import com.jswiff.util.StringUtilities;

import org.dom4j.Element;

import java.util.Iterator;
import java.util.List;


/*
 * Writes SWF records (everything that's neither a tag nor an action) to XML.
 */
class RecordXMLWriter {
  static void writeActionBlock(Element parentElement, ActionBlock actionBlock) {
    List actions    = actionBlock.getActions();
    Element element = parentElement.addElement("actionblock");
    for (Iterator it = actions.iterator(); it.hasNext();) {
      Action action = (Action) it.next();
      ActionXMLWriter.writeAction(element, action);
    }
  }

  static void writeAlignmentZones(
    Element parentElement, AlignmentZone[] alignmentZones) {
    Element element = parentElement.addElement("zones");
    for (int i = 0; i < alignmentZones.length; i++) {
      AlignmentZone zone  = alignmentZones[i];
      Element zoneElement = element.addElement("zone");
      if (zone.hasX()) {
        zoneElement.addAttribute(
          "left", StringUtilities.doubleToString(zone.getLeft()));
        zoneElement.addAttribute(
          "width", StringUtilities.doubleToString(zone.getWidth()));
      }
      if (zone.hasY()) {
        zoneElement.addAttribute(
          "baseline", StringUtilities.doubleToString(zone.getBaseline()));
        zoneElement.addAttribute(
          "height", StringUtilities.doubleToString(zone.getHeight()));
      }
    }
  }

  static void writeButtonCondAction(
    Element parentElement, ButtonCondAction buttonCondAction) {
    Element element = parentElement.addElement("buttoncondaction");
    if (buttonCondAction.isOutDownToIdle()) {
      element.addAttribute("outdowntoidle", "true");
    }
    if (buttonCondAction.isOutDownToOverDown()) {
      element.addAttribute("outdowntooverdown", "true");
    }
    if (buttonCondAction.isIdleToOverDown()) {
      element.addAttribute("idletooverdown", "true");
    }
    if (buttonCondAction.isIdleToOverUp()) {
      element.addAttribute("idletooverup", "true");
    }
    if (buttonCondAction.isOverDownToIdle()) {
      element.addAttribute("overdowntoidle", "true");
    }
    if (buttonCondAction.isOverDownToOutDown()) {
      element.addAttribute("overdowntooutdown", "true");
    }
    if (buttonCondAction.isOverDownToOverUp()) {
      element.addAttribute("overdowntooverup", "true");
    }
    if (buttonCondAction.isOverUpToIdle()) {
      element.addAttribute("overuptoidle", "true");
    }
    if (buttonCondAction.isOverUpToOverDown()) {
      element.addAttribute("overuptooverdown", "true");
    }
    byte keyPress = buttonCondAction.getKeyPress();
    if (keyPress != 0) {
      element.addAttribute("keypress", Byte.toString(keyPress));
    }
    RecordXMLWriter.writeActionBlock(element, buttonCondAction.getActions());
  }

  static void writeButtonRecord(
    Element parentElement, ButtonRecord buttonRecord) {
    Element element = parentElement.addElement("buttonrecord");
    element.addAttribute(
      "charid", Integer.toString(buttonRecord.getCharacterId()));
    element.addAttribute(
      "depth", Integer.toString(buttonRecord.getPlaceDepth()));
    if (buttonRecord.hasBlendMode()) {
      element.addAttribute(
        "blendmode", BlendMode.getDescription(buttonRecord.getBlendMode()));
    }
    Element state = element.addElement("state");
    if (buttonRecord.isUpState()) {
      state.addAttribute("up", "true");
    }
    if (buttonRecord.isDownState()) {
      state.addAttribute("down", "true");
    }
    if (buttonRecord.isOverState()) {
      state.addAttribute("over", "true");
    }
    if (buttonRecord.isHitState()) {
      state.addAttribute("hit", "true");
    }
    writeMatrix(element, buttonRecord.getPlaceMatrix());
    CXformWithAlpha colorTransform = buttonRecord.getColorTransform();
    if (colorTransform != null) {
      writeCXFormWithAlpha(element, colorTransform);
    }
    if (buttonRecord.hasFilters()) {
      RecordXMLWriter.writeFilters(element, buttonRecord.getFilters());
    }
  }

  static void writeCXForm(Element parentElement, CXform cXform) {
    Element element = parentElement.addElement("cxform");
    if (cXform.hasAddTerms()) {
      Element add = element.addElement("add");
      add.addAttribute("r", Integer.toString(cXform.getRedAddTerm()));
      add.addAttribute("g", Integer.toString(cXform.getGreenAddTerm()));
      add.addAttribute("b", Integer.toString(cXform.getBlueAddTerm()));
    }
    if (cXform.hasMultTerms()) {
      Element mult = element.addElement("mult");
      mult.addAttribute("r", Integer.toString(cXform.getRedMultTerm()));
      mult.addAttribute("g", Integer.toString(cXform.getGreenMultTerm()));
      mult.addAttribute("b", Integer.toString(cXform.getBlueMultTerm()));
    }
  }

  static void writeCXFormWithAlpha(
    Element parentElement, CXformWithAlpha cXformWithAlpha) {
    Element element = parentElement.addElement("cxformwithalpha");
    if (cXformWithAlpha.hasAddTerms()) {
      Element add = element.addElement("add");
      add.addAttribute("r", Integer.toString(cXformWithAlpha.getRedAddTerm()));
      add.addAttribute(
        "g", Integer.toString(cXformWithAlpha.getGreenAddTerm()));
      add.addAttribute("b", Integer.toString(cXformWithAlpha.getBlueAddTerm()));
      add.addAttribute(
        "a", Integer.toString(cXformWithAlpha.getAlphaAddTerm()));
    }
    if (cXformWithAlpha.hasMultTerms()) {
      Element mult = element.addElement("mult");
      mult.addAttribute(
        "r", Integer.toString(cXformWithAlpha.getRedMultTerm()));
      mult.addAttribute(
        "g", Integer.toString(cXformWithAlpha.getGreenMultTerm()));
      mult.addAttribute(
        "b", Integer.toString(cXformWithAlpha.getBlueMultTerm()));
      mult.addAttribute(
        "a", Integer.toString(cXformWithAlpha.getAlphaMultTerm()));
    }
  }

  static void writeClipActions(Element parentElement, ClipActions clipActions) {
    Element element = parentElement.addElement("clipactions");
    writeClipEventFlags(element, clipActions.getEventFlags());
    List records = clipActions.getClipActionRecords();
    for (Iterator it = records.iterator(); it.hasNext();) {
      ClipActionRecord record   = (ClipActionRecord) it.next();
      Element recordElement     = element.addElement("clipactionrecord");
      ClipEventFlags eventFlags = record.getEventFlags();
      writeClipEventFlags(recordElement, eventFlags);
      if (eventFlags.isKeyPress()) {
        recordElement.addAttribute(
          "keycode", Short.toString(record.getKeyCode()));
      }
      writeActionBlock(recordElement, record.getActions());
    }
  }

  static void writeColor(
    Element parentElement, String elementName, Color color) {
    if (color instanceof RGB) {
      writeRGB(parentElement, elementName, (RGB) color);
    } else {
      writeRGBA(parentElement, elementName, (RGBA) color);
    }
  }

  static void writeFilters(Element parentElement, List filters) {
    Element element = parentElement.addElement("filters");
    for (int j = 0; j < filters.size(); j++) {
      Object filter = filters.get(j);
      if (filter instanceof ColorMatrixFilter) {
        ColorMatrixFilter colorMatrixFilter = (ColorMatrixFilter) filter;
        Element filterElement               = element.addElement("colormatrix");
        float[] matrix                      = colorMatrixFilter.getMatrix();
        for (int i = 0; i < matrix.length; i++) {
          filterElement.addElement("val").addText(
            StringUtilities.doubleToString(matrix[i]));
        }
      } else if (filter instanceof ConvolutionFilter) {
        ConvolutionFilter convolutionFilter = (ConvolutionFilter) filter;
        Element filterElement               = element.addElement("convolution");
        Element matrixElement               = filterElement.addElement(
            "matrix");
        matrixElement.addAttribute(
          "rows", Integer.toString(convolutionFilter.getMatrixRows()));
        float[] matrix = convolutionFilter.getMatrix();
        for (int i = 0; i < matrix.length; i++) {
          matrixElement.addElement("val").addText(
            StringUtilities.doubleToString(matrix[i]));
        }
        writeRGBA(filterElement, "color", convolutionFilter.getColor());
        filterElement.addAttribute(
          "divisor",
          StringUtilities.doubleToString(convolutionFilter.getDivisor()));
        filterElement.addAttribute(
          "bias", StringUtilities.doubleToString(convolutionFilter.getBias()));
        if (convolutionFilter.isClamp()) {
          filterElement.addAttribute("clamp", "true");
        }
        if (convolutionFilter.isPreserveAlpha()) {
          filterElement.addAttribute("preservealpha", "true");
        }
      } else if (filter instanceof BlurFilter) {
        BlurFilter blurFilter = (BlurFilter) filter;
        Element filterElement = element.addElement("blur");
        filterElement.addAttribute(
          "x", StringUtilities.doubleToString(blurFilter.getX()));
        filterElement.addAttribute(
          "y", StringUtilities.doubleToString(blurFilter.getY()));
        filterElement.addAttribute(
          "quality", Integer.toString(blurFilter.getQuality()));
      } else if (filter instanceof DropShadowFilter) {
        DropShadowFilter dropShadowFilter = (DropShadowFilter) filter;
        Element filterElement             = element.addElement("dropshadow");
        writeRGBA(filterElement, "color", dropShadowFilter.getColor());
        filterElement.addAttribute(
          "x", StringUtilities.doubleToString(dropShadowFilter.getX()));
        filterElement.addAttribute(
          "y", StringUtilities.doubleToString(dropShadowFilter.getY()));
        filterElement.addAttribute(
          "angle", StringUtilities.doubleToString(dropShadowFilter.getAngle()));
        filterElement.addAttribute(
          "distance",
          StringUtilities.doubleToString(dropShadowFilter.getDistance()));
        filterElement.addAttribute(
          "strength",
          StringUtilities.doubleToString(dropShadowFilter.getStrength()));
        filterElement.addAttribute(
          "quality", Integer.toString(dropShadowFilter.getQuality()));
        if (dropShadowFilter.isInner()) {
          filterElement.addAttribute("inner", "true");
        }
        if (dropShadowFilter.isKnockout()) {
          filterElement.addAttribute("knockout", "true");
        }
        if (dropShadowFilter.isHideObject()) {
          filterElement.addAttribute("hideobject", "true");
        }
      } else if (filter instanceof GlowFilter) {
        GlowFilter glowFilter = (GlowFilter) filter;
        Element filterElement = element.addElement("glow");
        writeRGBA(filterElement, "color", glowFilter.getColor());
        filterElement.addAttribute(
          "x", StringUtilities.doubleToString(glowFilter.getX()));
        filterElement.addAttribute(
          "y", StringUtilities.doubleToString(glowFilter.getY()));
        filterElement.addAttribute(
          "strength", StringUtilities.doubleToString(glowFilter.getStrength()));
        filterElement.addAttribute(
          "quality", Integer.toString(glowFilter.getQuality()));
        if (glowFilter.isInner()) {
          filterElement.addAttribute("inner", "true");
        }
        if (glowFilter.isKnockout()) {
          filterElement.addAttribute("knockout", "true");
        }
      } else if (filter instanceof BevelFilter) {
        BevelFilter bevelFilter = (BevelFilter) filter;
        Element filterElement   = element.addElement("bevel");
        writeRGBA(
          filterElement, "highlightcolor", bevelFilter.getHighlightColor());
        writeRGBA(filterElement, "shadowcolor", bevelFilter.getShadowColor());
        filterElement.addAttribute(
          "x", StringUtilities.doubleToString(bevelFilter.getX()));
        filterElement.addAttribute(
          "y", StringUtilities.doubleToString(bevelFilter.getY()));
        filterElement.addAttribute(
          "angle", StringUtilities.doubleToString(bevelFilter.getAngle()));
        filterElement.addAttribute(
          "distance", StringUtilities.doubleToString(bevelFilter.getDistance()));
        filterElement.addAttribute(
          "strength", StringUtilities.doubleToString(bevelFilter.getStrength()));
        filterElement.addAttribute(
          "quality", Integer.toString(bevelFilter.getQuality()));
        if (bevelFilter.isInner()) {
          filterElement.addAttribute("inner", "true");
        }
        if (bevelFilter.isKnockout()) {
          filterElement.addAttribute("knockout", "true");
        }
        if (bevelFilter.isOnTop()) {
          filterElement.addAttribute("ontop", "true");
        }
      } else if (filter instanceof GradientGlowFilter) {
        GradientGlowFilter gradientGlowFilter = (GradientGlowFilter) filter;
        Element filterElement                 = element.addElement(
            "gradientglow");
        RGBA[] colors                         = gradientGlowFilter.getColors();
        short[] ratios                        = gradientGlowFilter.getRatios();
        int controlPointsCount                = colors.length;
        Element pointsElement                 = filterElement.addElement(
            "controlpoints");
        for (int i = 0; i < controlPointsCount; i++) {
          Element pointElement = pointsElement.addElement("controlpoint");
          pointElement.addAttribute("ratio", Short.toString(ratios[i]));
          writeRGBA(pointElement, "color", colors[i]);
        }
        filterElement.addAttribute(
          "x", StringUtilities.doubleToString(gradientGlowFilter.getX()));
        filterElement.addAttribute(
          "y", StringUtilities.doubleToString(gradientGlowFilter.getY()));
        filterElement.addAttribute(
          "angle", StringUtilities.doubleToString(
            gradientGlowFilter.getAngle()));
        filterElement.addAttribute(
          "distance",
          StringUtilities.doubleToString(gradientGlowFilter.getDistance()));
        filterElement.addAttribute(
          "strength",
          StringUtilities.doubleToString(gradientGlowFilter.getStrength()));
        filterElement.addAttribute(
          "quality", Integer.toString(gradientGlowFilter.getQuality()));
        if (gradientGlowFilter.isInner()) {
          filterElement.addAttribute("inner", "true");
        }
        if (gradientGlowFilter.isKnockout()) {
          filterElement.addAttribute("knockout", "true");
        }
        if (gradientGlowFilter.isOnTop()) {
          filterElement.addAttribute("ontop", "true");
        }
      } else if (filter instanceof GradientBevelFilter) {
        GradientBevelFilter gradientBevelFilter = (GradientBevelFilter) filter;
        Element filterElement                   = element.addElement(
            "gradientbevel");
        RGBA[] colors                           = gradientBevelFilter.getColors();
        short[] ratios                          = gradientBevelFilter.getRatios();
        int controlPointsCount                  = colors.length;
        Element pointsElement                   = filterElement.addElement(
            "controlpoints");
        for (int i = 0; i < controlPointsCount; i++) {
          Element pointElement = pointsElement.addElement("controlpoint");
          pointElement.addAttribute("ratio", Short.toString(ratios[i]));
          writeRGBA(pointElement, "color", colors[i]);
        }
        filterElement.addAttribute(
          "x", StringUtilities.doubleToString(gradientBevelFilter.getX()));
        filterElement.addAttribute(
          "y", StringUtilities.doubleToString(gradientBevelFilter.getY()));
        filterElement.addAttribute(
          "angle",
          StringUtilities.doubleToString(gradientBevelFilter.getAngle()));
        filterElement.addAttribute(
          "distance",
          StringUtilities.doubleToString(gradientBevelFilter.getDistance()));
        filterElement.addAttribute(
          "strength",
          StringUtilities.doubleToString(gradientBevelFilter.getStrength()));
        filterElement.addAttribute(
          "quality", Integer.toString(gradientBevelFilter.getQuality()));
        if (gradientBevelFilter.isInner()) {
          filterElement.addAttribute("inner", "true");
        }
        if (gradientBevelFilter.isKnockout()) {
          filterElement.addAttribute("knockout", "true");
        }
        if (gradientBevelFilter.isOnTop()) {
          filterElement.addAttribute("ontop", "true");
        }
      }
    }
  }

  static void writeMatrix(Element parentElement, Matrix matrix) {
    writeMatrix(parentElement, "matrix", matrix);
  }

  static void writeMatrix(
    Element parentElement, String elementName, Matrix matrix) {
    Element element   = parentElement.addElement(elementName);
    Element translate = element.addElement("translate");
    translate.addAttribute("x", Integer.toString(matrix.getTranslateX()));
    translate.addAttribute("y", Integer.toString(matrix.getTranslateY()));
    if (matrix.hasRotateSkew()) {
      Element rotateSkew = element.addElement("rotateskew");
      rotateSkew.addAttribute(
        "rs0", StringUtilities.doubleToString(matrix.getRotateSkew0()));
      rotateSkew.addAttribute(
        "rs1", StringUtilities.doubleToString(matrix.getRotateSkew1()));
    }
    if (matrix.hasScale()) {
      Element scale = element.addElement("scale");
      scale.addAttribute(
        "x", StringUtilities.doubleToString(matrix.getScaleX()));
      scale.addAttribute(
        "y", StringUtilities.doubleToString(matrix.getScaleY()));
    }
  }

  static void writeMorphFillStyles(
    Element parentElement, MorphFillStyles fillStyles) {
    int size                  = fillStyles.getSize();
    Element fillStylesElement = parentElement.addElement("morphfillstyles");
    for (int i = 1; i <= size; i++) {
      MorphFillStyle fillStyle = fillStyles.getStyle(i);
      writeMorphFillStyle(fillStylesElement, fillStyle);
    }
  }

  static void writeMorphLineStyles(
    Element parentElement, MorphLineStyles lineStyles) {
    int size                  = lineStyles.getSize();
    Element lineStylesElement = parentElement.addElement("morphlinestyles");
    for (int i = 1; i <= size; i++) {
      Object style = lineStyles.getStyle(i);
      if (style instanceof MorphLineStyle) {
        MorphLineStyle lineStyle = (MorphLineStyle) style;
        Element lineStyleElement = lineStylesElement.addElement(
            "morphlinestyle");
        Element startElement     = lineStyleElement.addElement("start");
        startElement.addAttribute(
          "width", Integer.toString(lineStyle.getStartWidth()));
        writeRGBA(startElement, "color", lineStyle.getStartColor());
        Element endElement = lineStyleElement.addElement("end");
        endElement.addAttribute(
          "width", Integer.toString(lineStyle.getEndWidth()));
        writeRGBA(endElement, "color", lineStyle.getEndColor());
      } else {
        MorphLineStyle2 lineStyle2 = (MorphLineStyle2) style;
        Element lineStyle2Element  = lineStylesElement.addElement(
            "morphlinestyle2");
        Element startElement       = lineStyle2Element.addElement("start");
        Element end                = lineStyle2Element.addElement("end");
        startElement.addAttribute(
          "width", Integer.toString(lineStyle2.getStartWidth()));
        end.addAttribute("width", Integer.toString(lineStyle2.getEndWidth()));
        writeEnhancedStrokeStyles(
          lineStyle2Element, lineStyle2.getJointStyle(),
          lineStyle2.getStartCapStyle(), lineStyle2.getEndCapStyle(),
          lineStyle2.getMiterLimit(), lineStyle2.getScaleStroke());
        if (lineStyle2.isClose()) {
          parentElement.addAttribute("close", "true");
        }
        if (lineStyle2.isPixelHinting()) {
          parentElement.addAttribute("pixelhinting", "true");
        }
        MorphFillStyle fillStyle = lineStyle2.getFillStyle();
        if (fillStyle == null) {
          writeColor(startElement, "color", lineStyle2.getStartColor());
          writeColor(end, "color", lineStyle2.getEndColor());
        } else {
          writeMorphFillStyle(lineStyle2Element, fillStyle);
        }
      }
    }
  }

  static void writeRGB(Element parentElement, String elementName, RGB rgbColor) {
    Element element = parentElement.addElement(elementName);
    element.addAttribute("r", Integer.toString(rgbColor.getRed()));
    element.addAttribute("g", Integer.toString(rgbColor.getGreen()));
    element.addAttribute("b", Integer.toString(rgbColor.getBlue()));
  }

  static void writeRGBA(
    Element parentElement, String elementName, RGBA rgbaColor) {
    Element element = parentElement.addElement(elementName);
    element.addAttribute("r", Integer.toString(rgbaColor.getRed()));
    element.addAttribute("g", Integer.toString(rgbaColor.getGreen()));
    element.addAttribute("b", Integer.toString(rgbaColor.getBlue()));
    element.addAttribute("a", Integer.toString(rgbaColor.getAlpha()));
  }

  static void writeRect(Element parentElement, String elementName, Rect rect) {
    Element element = parentElement.addElement(elementName);
    element.addAttribute("xmin", Long.toString(rect.getXMin()));
    element.addAttribute("xmax", Long.toString(rect.getXMax()));
    element.addAttribute("ymin", Long.toString(rect.getYMin()));
    element.addAttribute("ymax", Long.toString(rect.getYMax()));
  }

  static void writeShape(Element parentElement, Shape shape) {
    Element element       = parentElement.addElement("shape");
    ShapeRecord[] records = shape.getShapeRecords();
    for (int i = 0; i < records.length; i++) {
      writeShapeRecord(element, records[i]);
    }
  }

  static void writeShapeRecord(Element parentElement, ShapeRecord shapeRecord) {
    if (shapeRecord instanceof CurvedEdgeRecord) {
      writeCurvedEdgeRecord(parentElement, (CurvedEdgeRecord) shapeRecord);
    } else if (shapeRecord instanceof StraightEdgeRecord) {
      writeStraightEdgeRecord(parentElement, (StraightEdgeRecord) shapeRecord);
    } else {
      writeStyleChangeRecord(parentElement, (StyleChangeRecord) shapeRecord);
    }
  }

  static void writeShapeWithStyle(
    Element parentElement, ShapeWithStyle shapeWithStyle) {
    Element element       = parentElement.addElement("shapewithstyle");
    Element shapeElement  = element.addElement("shape");
    ShapeRecord[] records = shapeWithStyle.getShapeRecords();
    for (int i = 0; i < records.length; i++) {
      writeShapeRecord(shapeElement, records[i]);
    }
    writeLineStyles(element, shapeWithStyle.getLineStyles());
    writeFillStyles(element, shapeWithStyle.getFillStyles());
  }

  static void writeSoundInfo(Element parentElement, SoundInfo soundInfo) {
    Element element = parentElement.addElement("soundinfo");
    if (soundInfo.isSyncStop()) {
      element.addAttribute("syncstop", "true");
    }
    if (soundInfo.isSyncNoMultiple()) {
      element.addAttribute("syncnomultiple", "true");
    }
    if (soundInfo.hasLoops()) {
      element.addAttribute(
        "loopcount", Integer.toString(soundInfo.getLoopCount()));
    }
    if (soundInfo.hasInPoint()) {
      element.addAttribute("inpoint", Long.toString(soundInfo.getInPoint()));
    }
    if (soundInfo.hasOutPoint()) {
      element.addAttribute("outpoint", Long.toString(soundInfo.getOutPoint()));
    }
    if (soundInfo.hasEnvelope()) {
      Element recordsElement  = element.addElement("enveloperecords");
      SoundEnvelope[] records = soundInfo.getEnvelopeRecords();
      for (int i = 0; i < records.length; i++) {
        RecordXMLWriter.writeSoundEnvelope(recordsElement, records[i]);
      }
    }
  }

  static void writeTextRecord(Element parentElement, TextRecord record) {
    Element element = parentElement.addElement("textrecord");
    if (record.hasFont()) {
      element.addAttribute("fontid", Integer.toString(record.getFontId()));
      element.addAttribute("height", Integer.toString(record.getTextHeight()));
    }
    if (record.hasXOffset()) {
      element.addAttribute("xoffset", Short.toString(record.getXOffset()));
    }
    if (record.hasYOffset()) {
      element.addAttribute("yoffset", Short.toString(record.getYOffset()));
    }
    if (record.hasColor()) {
      writeColor(element, "color", record.getTextColor());
    }
    GlyphEntry[] glyphEntries   = record.getGlyphEntries();
    Element glyphEntriesElement = element.addElement("glyphentries");
    for (int i = 0; i < glyphEntries.length; i++) {
      GlyphEntry glyphEntry     = glyphEntries[i];
      Element glyphEntryElement = glyphEntriesElement.addElement("glyphentry");
      glyphEntryElement.addAttribute(
        "index", Integer.toString(glyphEntry.getGlyphIndex()));
      glyphEntryElement.addAttribute(
        "advance", Integer.toString(glyphEntry.getGlyphAdvance()));
    }
  }

  static void writeZlibBitmapData(Element parentElement, ZlibBitmapData data) {
    if (data instanceof BitmapData) {
      writeBitmapData(parentElement, (BitmapData) data);
    } else if (data instanceof ColorMapData) {
      writeColorMapData(parentElement, (ColorMapData) data);
    } else if (data instanceof AlphaBitmapData) {
      writeAlphaBitmapData(parentElement, (AlphaBitmapData) data);
    } else {
      writeAlphaColorMapData(parentElement, (AlphaColorMapData) data);
    }
  }

  private static void writeAlphaBitmapData(
    Element parentElement, AlphaBitmapData data) {
    Element element  = parentElement.addElement("alphabitmapdata");
    RGBA[] pixelData = data.getBitmapPixelData();
    writeRGBAArray(element, pixelData);
  }

  private static void writeAlphaColorMapData(
    Element parentElement, AlphaColorMapData data) {
    Element element           = parentElement.addElement("alphacolormapdata");
    Element colorTableElement = element.addElement("colortable");
    RGBA[] colorTable         = data.getColorTableRGBA();
    writeRGBAArray(colorTableElement, colorTable);
    Element pixelDataElement = element.addElement("pixeldata");
    short[] pixelData        = data.getColorMapPixelData();
    pixelDataElement.addText(Base64.encodeUnsigned(pixelData));
  }

  private static void writeBitmapData(Element parentElement, BitmapData data) {
    Element element             = parentElement.addElement("bitmapdata");
    BitmapPixelData[] pixelData = data.getBitmapPixelData();
    if (pixelData[0] instanceof Pix15) { // array is homogeneous
      writePix15Array(element, (Pix15[]) pixelData);
    } else {
      writePix24Array(element, (Pix24[]) pixelData);
    }
  }

  private static void writeClipEventFlags(
    Element parentElement, ClipEventFlags flags) {
    Element element = parentElement.addElement("clipeventflags");
    if (flags.isKeyUp()) {
      element.addAttribute("keyup", "true");
    }
    if (flags.isKeyDown()) {
      element.addAttribute("keydown", "true");
    }
    if (flags.isKeyPress()) {
      element.addAttribute("keypress", "true");
    }
    if (flags.isMouseUp()) {
      element.addAttribute("mouseup", "true");
    }
    if (flags.isMouseDown()) {
      element.addAttribute("mousedown", "true");
    }
    if (flags.isMouseMove()) {
      element.addAttribute("mousemove", "true");
    }
    if (flags.isLoad()) {
      element.addAttribute("load", "true");
    }
    if (flags.isUnload()) {
      element.addAttribute("unload", "true");
    }
    if (flags.isEnterFrame()) {
      element.addAttribute("enterframe", "true");
    }
    if (flags.isDragOver()) {
      element.addAttribute("dragover", "true");
    }
    if (flags.isDragOut()) {
      element.addAttribute("dragout", "true");
    }
    if (flags.isRollOver()) {
      element.addAttribute("rollover", "true");
    }
    if (flags.isRollOut()) {
      element.addAttribute("rollout", "true");
    }
    if (flags.isReleaseOutside()) {
      element.addAttribute("releaseoutside", "true");
    }
    if (flags.isRelease()) {
      element.addAttribute("release", "true");
    }
    if (flags.isPress()) {
      element.addAttribute("press", "true");
    }
    if (flags.isInitialize()) {
      element.addAttribute("initialize", "true");
    }
    if (flags.isData()) {
      element.addAttribute("data", "true");
    }
    if (flags.isConstruct()) {
      element.addAttribute("construct", "true");
    }
  }

  private static void writeColorMapData(
    Element parentElement, ColorMapData data) {
    Element element           = parentElement.addElement("colormapdata");
    Element colorTableElement = element.addElement("colortable");
    RGB[] colorTable          = data.getColorTableRGB();
    writeRGBArray(colorTableElement, colorTable);
    Element pixelDataElement = element.addElement("pixeldata");
    short[] pixelData        = data.getColorMapPixelData();
    pixelDataElement.addText(Base64.encodeUnsigned(pixelData));
  }

  private static void writeCurvedEdgeRecord(
    Element parentElement, CurvedEdgeRecord curvedEdgeRecord) {
    Element element = parentElement.addElement("curvededgerecord");
    element.addAttribute(
      "anchordx", Integer.toString(curvedEdgeRecord.getAnchorDeltaX()));
    element.addAttribute(
      "anchordy", Integer.toString(curvedEdgeRecord.getAnchorDeltaY()));
    element.addAttribute(
      "controldx", Integer.toString(curvedEdgeRecord.getControlDeltaX()));
    element.addAttribute(
      "controldy", Integer.toString(curvedEdgeRecord.getControlDeltaY()));
  }

  private static void writeEnhancedStrokeStyles(
    Element parentElement, byte jointStyle, byte startCapStyle, byte endCapStyle,
    double miterLimit, byte scaleStroke) {
    switch (jointStyle) {
      case EnhancedStrokeStyle.JOINT_ROUND:
        parentElement.addAttribute("joint", "round");
        break;
      case EnhancedStrokeStyle.JOINT_MITER:
        parentElement.addAttribute("joint", "miter");
        parentElement.addAttribute(
          "miterlimit", StringUtilities.doubleToString(miterLimit));
        break;
      case EnhancedStrokeStyle.JOINT_BEVEL:
        parentElement.addAttribute("joint", "bevel");
        break;
    }
    Element capStyleElement = parentElement.addElement("capstyle");
    switch (startCapStyle) {
      case EnhancedStrokeStyle.CAPS_NONE:
        capStyleElement.addAttribute("start", "none");
        break;
      case EnhancedStrokeStyle.CAPS_ROUND:
        capStyleElement.addAttribute("start", "round");
        break;
      case EnhancedStrokeStyle.CAPS_SQUARE:
        capStyleElement.addAttribute("start", "square");
        break;
    }
    switch (endCapStyle) {
      case EnhancedStrokeStyle.CAPS_NONE:
        capStyleElement.addAttribute("end", "none");
        break;
      case EnhancedStrokeStyle.CAPS_ROUND:
        capStyleElement.addAttribute("end", "round");
        break;
      case EnhancedStrokeStyle.CAPS_SQUARE:
        capStyleElement.addAttribute("end", "square");
        break;
    }
    switch (scaleStroke) {
      case EnhancedStrokeStyle.SCALE_NONE:
        parentElement.addAttribute("scalestroke", "none");
        break;
      case EnhancedStrokeStyle.SCALE_HORIZONTAL:
        parentElement.addAttribute("scalestroke", "horizontal");
        break;
      case EnhancedStrokeStyle.SCALE_VERTICAL:
        parentElement.addAttribute("scalestroke", "vertical");
        break;
      case EnhancedStrokeStyle.SCALE_BOTH:
        parentElement.addAttribute("scalestroke", "both");
        break;
    }
  }

  private static void writeFillStyle(
    Element parentElement, FillStyle fillStyle) {
    Element fillStyleElement = parentElement.addElement("fillstyle");
    boolean isGradient       = false;
    boolean isFocal          = false;
    boolean isBitmap         = false;
    switch (fillStyle.getType()) {
      case FillStyle.TYPE_SOLID:
        fillStyleElement.addAttribute("type", "solid");
        writeColor(fillStyleElement, "color", fillStyle.getColor());
        break;
      case FillStyle.TYPE_LINEAR_GRADIENT:
        fillStyleElement.addAttribute("type", "lineargradient");
        isGradient = true;
        break;
      case FillStyle.TYPE_FOCAL_RADIAL_GRADIENT:
        fillStyleElement.addAttribute("type", "focalradialgradient");
        isGradient = true;
        isFocal = true;
        break;
      case FillStyle.TYPE_RADIAL_GRADIENT:
        fillStyleElement.addAttribute("type", "radialgradient");
        isGradient = true;
        break;
      case FillStyle.TYPE_TILED_BITMAP:
        fillStyleElement.addAttribute("type", "tiledbitmap");
        isBitmap = true;
        break;
      case FillStyle.TYPE_CLIPPED_BITMAP:
        fillStyleElement.addAttribute("type", "clippedbitmap");
        isBitmap = true;
        break;
      case FillStyle.TYPE_NONSMOOTHED_TILED_BITMAP:
        fillStyleElement.addAttribute("type", "nonsmoothedtiledbitmap");
        isBitmap = true;
        break;
      case FillStyle.TYPE_NONSMOOTHED_CLIPPED_BITMAP:
        fillStyleElement.addAttribute("type", "nonsmoothedclippedbitmap");
        isBitmap = true;
        break;
      default:
        throw new IllegalArgumentException(
          "Illegal fill type: " + fillStyle.getType());
    }
    if (isGradient) {
      if (isFocal) {
        writeFocalGradient(
          fillStyleElement.addElement("focalgradient"),
          (FocalGradient) fillStyle.getGradient());
      } else {
        writeGradient(
          fillStyleElement.addElement("gradient"), fillStyle.getGradient());
      }
      writeMatrix(
        fillStyleElement, "gradientmatrix", fillStyle.getGradientMatrix());
    } else if (isBitmap) {
      fillStyleElement.addAttribute(
        "bitmapid", Integer.toString(fillStyle.getBitmapId()));
      writeMatrix(
        fillStyleElement, "bitmapmatrix", fillStyle.getBitmapMatrix());
    }
  }

  private static void writeFillStyles(
    Element parentElement, FillStyleArray fillStyles) {
    int size                  = fillStyles.getSize();
    Element fillStylesElement = parentElement.addElement("fillstyles");
    for (int i = 1; i <= size; i++) {
      FillStyle fillStyle = fillStyles.getStyle(i);
      writeFillStyle(fillStylesElement, fillStyle);
    }
  }

  private static void writeFocalGradient(
    Element gradientElement, FocalGradient gradient) {
    writeGradient(gradientElement, gradient);
    gradientElement.addAttribute(
      "focalpointratio",
      StringUtilities.doubleToString(gradient.getFocalPointRatio()));
  }

  private static void writeFocalMorphGradient(
    Element gradientElement, FocalMorphGradient gradient) {
    writeMorphGradient(gradientElement, gradient);
    gradientElement.addAttribute(
      "startfocalpointratio",
      StringUtilities.doubleToString(gradient.getStartFocalPointRatio()));
    gradientElement.addAttribute(
      "endfocalpointratio",
      StringUtilities.doubleToString(gradient.getEndFocalPointRatio()));
  }

  private static void writeGradient(Element gradientElement, Gradient gradient) {
    switch (gradient.getInterpolationMethod()) {
      case Gradient.INTERPOLATION_LINEAR_RGB:
        gradientElement.addAttribute("interpolation", "linear-rgb");
        break;
      case Gradient.INTERPOLATION_RGB:
        gradientElement.addAttribute("interpolation", "rgb");
        break;
    }
    switch (gradient.getSpreadMethod()) {
      case Gradient.SPREAD_PAD:
        gradientElement.addAttribute("spread", "pad");
        break;
      case Gradient.SPREAD_REFLECT:
        gradientElement.addAttribute("spread", "reflect");
        break;
      case Gradient.SPREAD_REPEAT:
        gradientElement.addAttribute("spread", "repeat");
        break;
    }
    GradRecord[] records = gradient.getGradientRecords();
    for (int i = 0; i < records.length; i++) {
      GradRecord record     = records[i];
      Element recordElement = gradientElement.addElement("gradrecord");
      recordElement.addAttribute("ratio", Short.toString(record.getRatio()));
      writeColor(recordElement, "color", record.getColor());
    }
  }

  private static void writeLineStyles(
    Element parentElement, LineStyleArray lineStyles) {
    int size                  = lineStyles.getSize();
    Element lineStylesElement = parentElement.addElement("linestyles");
    for (int i = 1; i <= size; i++) {
      Object style = lineStyles.getStyle(i);
      if (style instanceof LineStyle) {
        LineStyle lineStyle      = (LineStyle) style;
        Element lineStyleElement = lineStylesElement.addElement("linestyle");
        lineStyleElement.addAttribute(
          "width", Integer.toString(lineStyle.getWidth()));
        writeColor(lineStyleElement, "color", lineStyle.getColor());
      } else {
        LineStyle2 lineStyle2     = (LineStyle2) style;
        Element lineStyle2Element = lineStylesElement.addElement("linestyle2");
        lineStyle2Element.addAttribute(
          "width", Integer.toString(lineStyle2.getWidth()));
        writeEnhancedStrokeStyles(
          lineStyle2Element, lineStyle2.getJointStyle(),
          lineStyle2.getStartCapStyle(), lineStyle2.getEndCapStyle(),
          lineStyle2.getMiterLimit(), lineStyle2.getScaleStroke());
        if (lineStyle2.isClose()) {
          parentElement.addAttribute("close", "true");
        }
        if (lineStyle2.isPixelHinting()) {
          parentElement.addAttribute("pixelhinting", "true");
        }
        FillStyle fillStyle = lineStyle2.getFillStyle();
        if (fillStyle == null) {
          writeColor(lineStyle2Element, "color", lineStyle2.getColor());
        } else {
          writeFillStyle(lineStyle2Element, fillStyle);
        }
      }
    }
  }

  private static void writeMorphFillStyle(
    Element parentElement, MorphFillStyle fillStyle) {
    Element fillStyleElement = parentElement.addElement("morphfillstyle");
    Element start            = fillStyleElement.addElement("start");
    Element end              = fillStyleElement.addElement("end");
    boolean isGradient       = false;
    boolean isFocal          = false;
    boolean isBitmap         = false;
    switch (fillStyle.getType()) {
      case FillStyle.TYPE_SOLID:
        fillStyleElement.addAttribute("type", "solid");
        writeRGBA(start, "color", fillStyle.getStartColor());
        writeRGBA(end, "color", fillStyle.getEndColor());
        break;
      case FillStyle.TYPE_LINEAR_GRADIENT:
        fillStyleElement.addAttribute("type", "lineargradient");
        isGradient = true;
        break;
      case FillStyle.TYPE_FOCAL_RADIAL_GRADIENT:
        fillStyleElement.addAttribute("type", "focalradialgradient");
        isGradient = true;
        isFocal = true;
        break;
      case FillStyle.TYPE_RADIAL_GRADIENT:
        fillStyleElement.addAttribute("type", "radialgradient");
        isGradient = true;
        break;
      case FillStyle.TYPE_TILED_BITMAP:
        fillStyleElement.addAttribute("type", "tiledbitmap");
        isBitmap = true;
        break;
      case FillStyle.TYPE_CLIPPED_BITMAP:
        fillStyleElement.addAttribute("type", "clippedbitmap");
        isBitmap = true;
        break;
      case FillStyle.TYPE_NONSMOOTHED_TILED_BITMAP:
        fillStyleElement.addAttribute("type", "nonsmoothedtiledbitmap");
        isBitmap = true;
        break;
      case FillStyle.TYPE_NONSMOOTHED_CLIPPED_BITMAP:
        fillStyleElement.addAttribute("type", "nonsmoothedclippedbitmap");
        isBitmap = true;
        break;
      default:
        throw new IllegalArgumentException(
          "Illegal morph fill type: " + fillStyle.getType());
    }
    if (isGradient) {
      if (isFocal) {
        writeFocalMorphGradient(
          fillStyleElement.addElement("focalmorphgradient"),
          (FocalMorphGradient) fillStyle.getGradient());
      } else {
        writeMorphGradient(
          fillStyleElement.addElement("morphgradient"), fillStyle.getGradient());
      }
      writeMatrix(start, "gradientmatrix", fillStyle.getStartGradientMatrix());
      writeMatrix(end, "gradientmatrix", fillStyle.getEndGradientMatrix());
    } else if (isBitmap) {
      fillStyleElement.addAttribute(
        "bitmapid", Integer.toString(fillStyle.getBitmapId()));
      writeMatrix(start, "bitmapmatrix", fillStyle.getStartBitmapMatrix());
      writeMatrix(end, "bitmapmatrix", fillStyle.getEndBitmapMatrix());
    }
  }

  private static void writeMorphGradient(
    Element gradientElement, MorphGradient gradient) {
    switch (gradient.getInterpolationMethod()) {
      case Gradient.INTERPOLATION_LINEAR_RGB:
        gradientElement.addAttribute("interpolation", "linear-rgb");
        break;
      case Gradient.INTERPOLATION_RGB:
        gradientElement.addAttribute("interpolation", "rgb");
        break;
    }
    switch (gradient.getSpreadMethod()) {
      case Gradient.SPREAD_PAD:
        gradientElement.addAttribute("spread", "pad");
        break;
      case Gradient.SPREAD_REFLECT:
        gradientElement.addAttribute("spread", "reflect");
        break;
      case Gradient.SPREAD_REPEAT:
        gradientElement.addAttribute("spread", "repeat");
        break;
    }
    MorphGradRecord[] records = gradient.getGradientRecords();
    for (int i = 0; i < records.length; i++) {
      MorphGradRecord record = records[i];
      Element recordElement  = gradientElement.addElement("morphgradrecord");
      Element start          = recordElement.addElement("start");
      start.addAttribute("ratio", Short.toString(record.getStartRatio()));
      writeRGBA(start, "color", record.getStartColor());
      Element end = recordElement.addElement("end");
      end.addAttribute("ratio", Short.toString(record.getEndRatio()));
      writeRGBA(end, "color", record.getEndColor());
    }
  }

  private static void writePix15Array(Element parentElement, Pix15[] data) {
    Element element = parentElement.addElement("pix15array");
    byte[] buffer   = new byte[data.length * 3];
    for (int i = 0; i < data.length; i++) {
      Pix15 pix15 = data[i];
      buffer[i * 3]         = pix15.getRed();
      buffer[(i * 3) + 1]   = pix15.getGreen();
      buffer[(i * 3) + 2]   = pix15.getBlue();
    }
    element.addText(Base64.encode(buffer));
  }

  private static void writePix24Array(Element parentElement, Pix24[] data) {
    Element element = parentElement.addElement("pix24array");
    short[] buffer  = new short[data.length * 3];
    for (int i = 0; i < data.length; i++) {
      Pix24 pix24 = data[i];
      buffer[i * 3]         = pix24.getRed();
      buffer[(i * 3) + 1]   = pix24.getGreen();
      buffer[(i * 3) + 2]   = pix24.getBlue();
    }
    element.addText(Base64.encodeUnsigned(buffer));
  }

  private static void writeRGBAArray(Element parentElement, RGBA[] data) {
    Element element = parentElement.addElement("rgbaarray");
    short[] buffer  = new short[data.length * 4];
    for (int i = 0; i < data.length; i++) {
      RGBA rgba = data[i];
      buffer[i * 4]         = rgba.getRed();
      buffer[(i * 4) + 1]   = rgba.getGreen();
      buffer[(i * 4) + 2]   = rgba.getBlue();
      buffer[(i * 4) + 3]   = rgba.getAlpha();
    }
    element.addText(Base64.encodeUnsigned(buffer));
  }

  private static void writeRGBArray(Element parentElement, RGB[] data) {
    Element element = parentElement.addElement("rgbarray");
    short[] buffer  = new short[data.length * 3];
    for (int i = 0; i < data.length; i++) {
      RGB rgb = data[i];
      buffer[i * 3]         = rgb.getRed();
      buffer[(i * 3) + 1]   = rgb.getGreen();
      buffer[(i * 3) + 2]   = rgb.getBlue();
    }
    element.addText(Base64.encodeUnsigned(buffer));
  }

  private static void writeSoundEnvelope(
    Element parentElement, SoundEnvelope soundEnvelope) {
    Element element = parentElement.addElement("soundenvelope");
    element.addAttribute("pos44", Long.toString(soundEnvelope.getPos44()));
    element.addAttribute(
      "leftlevel", Integer.toString(soundEnvelope.getLeftLevel()));
    element.addAttribute(
      "rightlevel", Integer.toString(soundEnvelope.getRightLevel()));
  }

  private static void writeStraightEdgeRecord(
    Element parentElement, StraightEdgeRecord straightEdgeRecord) {
    Element element = parentElement.addElement("straightedgerecord");
    element.addAttribute(
      "dx", Integer.toString(straightEdgeRecord.getDeltaX()));
    element.addAttribute(
      "dy", Integer.toString(straightEdgeRecord.getDeltaY()));
  }

  private static void writeStyleChangeRecord(
    Element parentElement, StyleChangeRecord styleChangeRecord) {
    Element element = parentElement.addElement("stylechangerecord");
    if (styleChangeRecord.hasMoveTo()) {
      Element moveTo = element.addElement("moveto");
      moveTo.addAttribute(
        "x", Integer.toString(styleChangeRecord.getMoveToX()));
      moveTo.addAttribute(
        "y", Integer.toString(styleChangeRecord.getMoveToY()));
    }
    if (
      styleChangeRecord.hasLineStyle() || styleChangeRecord.hasFillStyle0() ||
          styleChangeRecord.hasFillStyle1() ||
          styleChangeRecord.hasNewStyles()) {
      Element styles = element.addElement("styles");
      if (styleChangeRecord.hasLineStyle()) {
        styles.addAttribute(
          "line", Integer.toString(styleChangeRecord.getLineStyle()));
      }
      if (styleChangeRecord.hasFillStyle0()) {
        styles.addAttribute(
          "fill0", Integer.toString(styleChangeRecord.getFillStyle0()));
      }
      if (styleChangeRecord.hasFillStyle1()) {
        styles.addAttribute(
          "fill1", Integer.toString(styleChangeRecord.getFillStyle1()));
      }
      if (styleChangeRecord.hasNewStyles()) {
        writeLineStyles(styles, styleChangeRecord.getNewLineStyles());
        writeFillStyles(styles, styleChangeRecord.getNewFillStyles());
      }
    }
  }
}
