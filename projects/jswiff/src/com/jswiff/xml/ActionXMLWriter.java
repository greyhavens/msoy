/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2005 Ralf Terdic (contact@jswiff.com)
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

import com.jswiff.swfrecords.RegisterParam;
import com.jswiff.swfrecords.actions.Action;
import com.jswiff.swfrecords.actions.ActionConstants;
import com.jswiff.swfrecords.actions.ConstantPool;
import com.jswiff.swfrecords.actions.DefineFunction;
import com.jswiff.swfrecords.actions.DefineFunction2;
import com.jswiff.swfrecords.actions.GetURL;
import com.jswiff.swfrecords.actions.GetURL2;
import com.jswiff.swfrecords.actions.GoToFrame;
import com.jswiff.swfrecords.actions.GoToFrame2;
import com.jswiff.swfrecords.actions.GoToLabel;
import com.jswiff.swfrecords.actions.If;
import com.jswiff.swfrecords.actions.Jump;
import com.jswiff.swfrecords.actions.Push;
import com.jswiff.swfrecords.actions.SetTarget;
import com.jswiff.swfrecords.actions.StoreRegister;
import com.jswiff.swfrecords.actions.Try;
import com.jswiff.swfrecords.actions.UnknownAction;
import com.jswiff.swfrecords.actions.WaitForFrame;
import com.jswiff.swfrecords.actions.WaitForFrame2;
import com.jswiff.swfrecords.actions.With;
import com.jswiff.util.Base64;
import com.jswiff.util.StringUtilities;

import org.dom4j.Element;

import java.util.Iterator;
import java.util.List;


/*
 * Writes SWF actions to XML.
 */
class ActionXMLWriter {
  static void writeAction(Element parentElement, Action action) {
    int actionCode  = action.getCode();
    Element element;
    switch (actionCode) {
      case ActionConstants.ADD:
        element = parentElement.addElement("add");
        break;
      case ActionConstants.ADD_2:
        element = parentElement.addElement("add2");
        break;
      case ActionConstants.AND:
        element = parentElement.addElement("and");
        break;
      case ActionConstants.ASCII_TO_CHAR:
        element = parentElement.addElement("asciitochar");
        break;
      case ActionConstants.BIT_AND:
        element = parentElement.addElement("bitand");
        break;
      case ActionConstants.BIT_L_SHIFT:
        element = parentElement.addElement("bitlshift");
        break;
      case ActionConstants.BIT_OR:
        element = parentElement.addElement("bitor");
        break;
      case ActionConstants.BIT_R_SHIFT:
        element = parentElement.addElement("bitrshift");
        break;
      case ActionConstants.BIT_U_R_SHIFT:
        element = parentElement.addElement("biturshift");
        break;
      case ActionConstants.BIT_XOR:
        element = parentElement.addElement("bitxor");
        break;
      case ActionConstants.CALL:
        element = parentElement.addElement("call");
        break;
      case ActionConstants.CALL_FUNCTION:
        element = parentElement.addElement("callfunction");
        break;
      case ActionConstants.CALL_METHOD:
        element = parentElement.addElement("callmethod");
        break;
      case ActionConstants.CAST_OP:
        element = parentElement.addElement("castop");
        break;
      case ActionConstants.CHAR_TO_ASCII:
        element = parentElement.addElement("chartoascii");
        break;
      case ActionConstants.CLONE_SPRITE:
        element = parentElement.addElement("clonesprite");
        break;
      case ActionConstants.CONSTANT_POOL:
        element = writeConstantPool(parentElement, (ConstantPool) action);
        break;
      case ActionConstants.DECREMENT:
        element = parentElement.addElement("decrement");
        break;
      case ActionConstants.DEFINE_FUNCTION:
        element = writeDefineFunction(parentElement, (DefineFunction) action);
        break;
      case ActionConstants.DEFINE_FUNCTION_2:
        element = writeDefineFunction2(parentElement, (DefineFunction2) action);
        break;
      case ActionConstants.DEFINE_LOCAL:
        element = parentElement.addElement("definelocal");
        break;
      case ActionConstants.DEFINE_LOCAL_2:
        element = parentElement.addElement("definelocal2");
        break;
      case ActionConstants.DELETE:
        element = parentElement.addElement("delete");
        break;
      case ActionConstants.DELETE_2:
        element = parentElement.addElement("delete2");
        break;
      case ActionConstants.DIVIDE:
        element = parentElement.addElement("divide");
        break;
      case ActionConstants.END_DRAG:
        element = parentElement.addElement("enddrag");
        break;
      case ActionConstants.ENUMERATE:
        element = parentElement.addElement("enumerate");
        break;
      case ActionConstants.ENUMERATE_2:
        element = parentElement.addElement("enumerate2");
        break;
      case ActionConstants.EQUALS:
        element = parentElement.addElement("equals");
        break;
      case ActionConstants.EQUALS_2:
        element = parentElement.addElement("equals2");
        break;
      case ActionConstants.EXTENDS:
        element = parentElement.addElement("extends");
        break;
      case ActionConstants.GET_MEMBER:
        element = parentElement.addElement("getmember");
        break;
      case ActionConstants.GET_PROPERTY:
        element = parentElement.addElement("getproperty");
        break;
      case ActionConstants.GET_TIME:
        element = parentElement.addElement("gettime");
        break;
      case ActionConstants.GET_URL:
        element = writeGetURL(parentElement, (GetURL) action);
        break;
      case ActionConstants.GET_URL_2:
        element = writeGetURL2(parentElement, (GetURL2) action);
        break;
      case ActionConstants.GET_VARIABLE:
        element = parentElement.addElement("getvariable");
        break;
      case ActionConstants.GO_TO_FRAME:
        element = writeGoToFrame(parentElement, (GoToFrame) action);
        break;
      case ActionConstants.GO_TO_FRAME_2:
        element = writeGoToFrame2(parentElement, (GoToFrame2) action);
        break;
      case ActionConstants.GO_TO_LABEL:
        element = writeGoToLabel(parentElement, (GoToLabel) action);
        break;
      case ActionConstants.GREATER:
        element = parentElement.addElement("greater");
        break;
      case ActionConstants.IF:
        element = writeIf(parentElement, (If) action);
        break;
      case ActionConstants.IMPLEMENTS_OP:
        element = parentElement.addElement("implementsop");
        break;
      case ActionConstants.INCREMENT:
        element = parentElement.addElement("increment");
        break;
      case ActionConstants.INIT_ARRAY:
        element = parentElement.addElement("initarray");
        break;
      case ActionConstants.INIT_OBJECT:
        element = parentElement.addElement("initobject");
        break;
      case ActionConstants.INSTANCE_OF:
        element = parentElement.addElement("instanceof");
        break;
      case ActionConstants.JUMP:
        element = writeJump(parentElement, (Jump) action);
        break;
      case ActionConstants.LESS:
        element = parentElement.addElement("less");
        break;
      case ActionConstants.LESS_2:
        element = parentElement.addElement("less2");
        break;
      case ActionConstants.M_B_ASCII_TO_CHAR:
        element = parentElement.addElement("mbasciitochar");
        break;
      case ActionConstants.M_B_CHAR_TO_ASCII:
        element = parentElement.addElement("mbchartoascii");
        break;
      case ActionConstants.M_B_STRING_EXTRACT:
        element = parentElement.addElement("mbstringextract");
        break;
      case ActionConstants.M_B_STRING_LENGTH:
        element = parentElement.addElement("mbstringlength");
        break;
      case ActionConstants.MODULO:
        element = parentElement.addElement("modulo");
        break;
      case ActionConstants.MULTIPLY:
        element = parentElement.addElement("multiply");
        break;
      case ActionConstants.NEW_METHOD:
        element = parentElement.addElement("newmethod");
        break;
      case ActionConstants.NEW_OBJECT:
        element = parentElement.addElement("newobject");
        break;
      case ActionConstants.NEXT_FRAME:
        element = parentElement.addElement("nextframe");
        break;
      case ActionConstants.NOT:
        element = parentElement.addElement("not");
        break;
      case ActionConstants.OR:
        element = parentElement.addElement("or");
        break;
      case ActionConstants.PLAY:
        element = parentElement.addElement("play");
        break;
      case ActionConstants.POP:
        element = parentElement.addElement("pop");
        break;
      case ActionConstants.PREVIOUS_FRAME:
        element = parentElement.addElement("previousframe");
        break;
      case ActionConstants.PUSH:
        element = writePush(parentElement, (Push) action);
        break;
      case ActionConstants.PUSH_DUPLICATE:
        element = parentElement.addElement("pushduplicate");
        break;
      case ActionConstants.RANDOM_NUMBER:
        element = parentElement.addElement("randomnumber");
        break;
      case ActionConstants.REMOVE_SPRITE:
        element = parentElement.addElement("removesprite");
        break;
      case ActionConstants.RETURN:
        element = parentElement.addElement("return");
        break;
      case ActionConstants.SET_MEMBER:
        element = parentElement.addElement("setmember");
        break;
      case ActionConstants.SET_PROPERTY:
        element = parentElement.addElement("setproperty");
        break;
      case ActionConstants.SET_TARGET:
        element = writeSetTarget(parentElement, (SetTarget) action);
        break;
      case ActionConstants.SET_TARGET_2:
        element = parentElement.addElement("settarget2");
        break;
      case ActionConstants.SET_VARIABLE:
        element = parentElement.addElement("setvariable");
        break;
      case ActionConstants.STACK_SWAP:
        element = parentElement.addElement("stackswap");
        break;
      case ActionConstants.START_DRAG:
        element = parentElement.addElement("startdrag");
        break;
      case ActionConstants.STOP:
        element = parentElement.addElement("stop");
        break;
      case ActionConstants.STOP_SOUNDS:
        element = parentElement.addElement("stopsounds");
        break;
      case ActionConstants.STORE_REGISTER:
        element = writeStoreRegister(parentElement, (StoreRegister) action);
        break;
      case ActionConstants.STRICT_EQUALS:
        element = parentElement.addElement("strictequals");
        break;
      case ActionConstants.STRING_ADD:
        element = parentElement.addElement("stringadd");
        break;
      case ActionConstants.STRING_EQUALS:
        element = parentElement.addElement("stringequals");
        break;
      case ActionConstants.STRING_EXTRACT:
        element = parentElement.addElement("stringextract");
        break;
      case ActionConstants.STRING_GREATER:
        element = parentElement.addElement("stringgreater");
        break;
      case ActionConstants.STRING_LENGTH:
        element = parentElement.addElement("stringlength");
        break;
      case ActionConstants.STRING_LESS:
        element = parentElement.addElement("stringless");
        break;
      case ActionConstants.SUBTRACT:
        element = parentElement.addElement("subtract");
        break;
      case ActionConstants.TARGET_PATH:
        element = parentElement.addElement("targetpath");
        break;
      case ActionConstants.THROW:
        element = parentElement.addElement("throw");
        break;
      case ActionConstants.TO_INTEGER:
        element = parentElement.addElement("tointeger");
        break;
      case ActionConstants.TO_NUMBER:
        element = parentElement.addElement("tonumber");
        break;
      case ActionConstants.TO_STRING:
        element = parentElement.addElement("tostring");
        break;
      case ActionConstants.TOGGLE_QUALITY:
        element = parentElement.addElement("togglequality");
        break;
      case ActionConstants.TRACE:
        element = parentElement.addElement("trace");
        break;
      case ActionConstants.TRY:
        element = writeTry(parentElement, (Try) action);
        break;
      case ActionConstants.TYPE_OF:
        element = parentElement.addElement("typeof");
        break;
      case ActionConstants.WAIT_FOR_FRAME:
        element = writeWaitForFrame(parentElement, (WaitForFrame) action);
        break;
      case ActionConstants.WAIT_FOR_FRAME_2:
        element = writeWaitForFrame2(parentElement, (WaitForFrame2) action);
        break;
      case ActionConstants.WITH:
        element = writeWith(parentElement, (With) action);
        break;
      default:
        element = writeUnknown(parentElement, (UnknownAction) action);
    }
    String label = action.getLabel();
    if (label != null) {
      element.addAttribute("label", label);
    }
  }

  private static Element writeConstantPool(
    Element parentElement, ConstantPool constantPool) {
    Element element = parentElement.addElement("constantpool");
    List constants  = constantPool.getConstants();
    int id          = 0;
    for (Iterator it = constants.iterator(); it.hasNext();) {
      String constant         = (String) it.next();
      Element constantElement = element.addElement("constant");
      constantElement.addAttribute("id", Integer.toString(id++));
      if (StringUtilities.containsIllegalChars(constant)) {
        constantElement.addElement("value").addAttribute("base64", "true")
                       .addText(Base64.encodeString(constant));
      } else {
        constantElement.addElement("value").addText(constant);
      }
    }
    return element;
  }

  private static Element writeDefineFunction(
    Element parentElement, DefineFunction defineFunction) {
    Element element = parentElement.addElement("definefunction");
    element.addAttribute("name", defineFunction.getName());
    String[] parameters       = defineFunction.getParameters();
    Element parametersElement = element.addElement("parameters");
    for (int i = 0; i < parameters.length; i++) {
      parametersElement.addElement("parameter").addAttribute(
        "name", parameters[i]);
    }
    RecordXMLWriter.writeActionBlock(element, defineFunction.getBody());
    return element;
  }

  private static Element writeDefineFunction2(
    Element parentElement, DefineFunction2 defineFunction2) {
    Element element = parentElement.addElement("definefunction2");
    element.addAttribute("name", defineFunction2.getName());
    element.addAttribute(
      "registercount", Short.toString(defineFunction2.getRegisterCount()));
    RegisterParam[] parameters = defineFunction2.getParameters();
    Element parametersElement  = element.addElement("parameters");
    for (int i = 0; i < parameters.length; i++) {
      RegisterParam parameter = parameters[i];
      Element paramElement    = parametersElement.addElement("registerparam");
      paramElement.addAttribute("name", parameter.getParamName());
      paramElement.addAttribute(
        "register", Short.toString(parameter.getRegister()));
    }
    Element preloadElement = element.addElement("preload");
    if (defineFunction2.preloadsArguments()) {
      preloadElement.addAttribute("arguments", "true");
    }
    if (defineFunction2.preloadsGlobal()) {
      preloadElement.addAttribute("global", "true");
    }
    if (defineFunction2.preloadsParent()) {
      preloadElement.addAttribute("parent", "true");
    }
    if (defineFunction2.preloadsRoot()) {
      preloadElement.addAttribute("root", "true");
    }
    if (defineFunction2.preloadsSuper()) {
      preloadElement.addAttribute("super", "true");
    }
    if (defineFunction2.preloadsThis()) {
      preloadElement.addAttribute("this", "true");
    }
    Element suppressElement = element.addElement("suppress");
    if (defineFunction2.suppressesArguments()) {
      suppressElement.addAttribute("arguments", "true");
    }
    if (defineFunction2.suppressesSuper()) {
      suppressElement.addAttribute("super", "true");
    }
    if (defineFunction2.suppressesThis()) {
      suppressElement.addAttribute("this", "true");
    }
    RecordXMLWriter.writeActionBlock(element, defineFunction2.getBody());
    return element;
  }

  private static Element writeGetURL(Element parentElement, GetURL getURL) {
    Element element = parentElement.addElement("geturl");
    element.addAttribute("url", getURL.getURL());
    element.addAttribute("target", getURL.getTarget());
    return element;
  }

  private static Element writeGetURL2(Element parentElement, GetURL2 getURL2) {
    Element element = parentElement.addElement("geturl2");
    switch (getURL2.getSendVarsMethod()) {
      case GetURL2.METHOD_GET:
        element.addAttribute("sendvarsmethod", "get");
        break;
      case GetURL2.METHOD_POST:
        element.addAttribute("sendvarsmethod", "post");
        break;
      default:
        element.addAttribute("sendvarsmethod", "none");
    }
    if (getURL2.isLoadTarget()) {
      element.addAttribute("loadtarget", "true");
    }
    if (getURL2.isLoadVariables()) {
      element.addAttribute("loadvariables", "true");
    }
    return element;
  }

  private static Element writeGoToFrame(
    Element parentElement, GoToFrame goToFrame) {
    Element element = parentElement.addElement("gotoframe");
    element.addAttribute("frame", Integer.toString(goToFrame.getFrame()));
    return element;
  }

  private static Element writeGoToFrame2(
    Element parentElement, GoToFrame2 goToFrame2) {
    Element element = parentElement.addElement("gotoframe2");
    element.addAttribute(
      "scenebias", Integer.toString(goToFrame2.getSceneBias()));
    if (goToFrame2.play()) {
      element.addAttribute("play", "true");
    }
    return element;
  }

  private static Element writeGoToLabel(
    Element parentElement, GoToLabel goToLabel) {
    Element element = parentElement.addElement("gotolabel");
    element.addAttribute("framelabel", goToLabel.getFrameLabel());
    return element;
  }

  private static Element writeIf(Element parentElement, If ifAction) {
    Element element = parentElement.addElement("if");
    element.addAttribute("branchlabel", ifAction.getBranchLabel());
    return element;
  }

  private static Element writeJump(Element parentElement, Jump jump) {
    Element element = parentElement.addElement("jump");
    element.addAttribute("branchlabel", jump.getBranchLabel());
    return element;
  }

  private static Element writePush(Element parentElement, Push push) {
    Element element = parentElement.addElement("push");
    List values     = push.getValues();
    for (Iterator it = values.iterator(); it.hasNext();) {
      Push.StackValue value = (Push.StackValue) it.next();
      switch (value.getType()) {
        case Push.StackValue.TYPE_BOOLEAN:
          element.addElement("boolean").addAttribute(
            "value", Boolean.toString(value.getBoolean()));
          break;
        case Push.StackValue.TYPE_CONSTANT_16:
          element.addElement("constant16").addAttribute(
            "id", Integer.toString(value.getConstant16()));
          break;
        case Push.StackValue.TYPE_CONSTANT_8:
          element.addElement("constant8").addAttribute(
            "id", Integer.toString(value.getConstant8()));
          break;
        case Push.StackValue.TYPE_DOUBLE:
          element.addElement("double").addAttribute(
            "value", Double.toString(value.getDouble()));
          break;
        case Push.StackValue.TYPE_FLOAT:
          element.addElement("float").addAttribute(
            "value", Float.toString(value.getFloat()));
          break;
        case Push.StackValue.TYPE_INTEGER:
          element.addElement("integer").addAttribute(
            "value", Long.toString(value.getInteger()));
          break;
        case Push.StackValue.TYPE_NULL:
          element.addElement("null");
          break;
        case Push.StackValue.TYPE_REGISTER:
          element.addElement("register").addAttribute(
            "number", Short.toString(value.getRegisterNumber()));
          break;
        case Push.StackValue.TYPE_STRING:
          element.addElement("string").addAttribute("value", value.getString());
          break;
        case Push.StackValue.TYPE_UNDEFINED:
          element.addElement("undefined");
          break;
      }
    }
    return element;
  }

  private static Element writeSetTarget(
    Element parentElement, SetTarget setTarget) {
    Element element = parentElement.addElement("settarget");
    element.addAttribute("name", setTarget.getName());
    return element;
  }

  private static Element writeStoreRegister(
    Element parentElement, StoreRegister storeRegister) {
    Element element = parentElement.addElement("storeregister");
    element.addAttribute("number", Short.toString(storeRegister.getNumber()));
    return element;
  }

  private static Element writeTry(Element parentElement, Try tryAction) {
    Element element = parentElement.addElement("try");
    if (tryAction.catchInRegister()) {
      element.addAttribute(
        "catchregister", Short.toString(tryAction.getCatchRegister()));
    } else {
      element.addAttribute("catchvariable", tryAction.getCatchVariable());
    }
    RecordXMLWriter.writeActionBlock(
      element.addElement("try"), tryAction.getTryBlock());
    if (tryAction.hasCatchBlock()) {
      RecordXMLWriter.writeActionBlock(
        element.addElement("catch"), tryAction.getCatchBlock());
    }
    if (tryAction.hasFinallyBlock()) {
      RecordXMLWriter.writeActionBlock(
        element.addElement("finally"), tryAction.getFinallyBlock());
    }
    return element;
  }

  private static Element writeUnknown(
    Element parentElement, UnknownAction action) {
    Element element = parentElement.addElement("unknownaction");
    element.addAttribute("code", Integer.toString(action.getCode()));
    element.addText(Base64.encode(action.getData()));
    return element;
  }

  private static Element writeWaitForFrame(
    Element parentElement, WaitForFrame waitForFrame) {
    Element element = parentElement.addElement("waitforframe");
    element.addAttribute("frame", Integer.toString(waitForFrame.getFrame()));
    element.addAttribute(
      "skipcount", Short.toString(waitForFrame.getSkipCount()));
    return element;
  }

  private static Element writeWaitForFrame2(
    Element parentElement, WaitForFrame2 waitForFrame2) {
    Element element = parentElement.addElement("waitforframe2");
    element.addAttribute(
      "skipcount", Short.toString(waitForFrame2.getSkipCount()));
    return element;
  }

  private static Element writeWith(Element parentElement, With with) {
    Element element = parentElement.addElement("with");
    RecordXMLWriter.writeActionBlock(element, with.getWithBlock());
    return element;
  }
}
