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
import com.jswiff.swfrecords.actions.*;
import com.jswiff.util.Base64;

import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.Iterator;
import java.util.List;


class ActionXMLReader {
  static Action readAction(Element element) {
    String name   = element.getName();
    Action action;
    if (name.equals("add")) {
      action = new Add();
    } else if (name.equals("add2")) {
      action = new Add2();
    } else if (name.equals("and")) {
      action = new And();
    } else if (name.equals("asciitochar")) {
      action = new AsciiToChar();
    } else if (name.equals("bitand")) {
      action = new BitAnd();
    } else if (name.equals("bitlshift")) {
      action = new BitLShift();
    } else if (name.equals("bitor")) {
      action = new BitOr();
    } else if (name.equals("bitrshift")) {
      action = new BitRShift();
    } else if (name.equals("biturshift")) {
      action = new BitURShift();
    } else if (name.equals("bitxor")) {
      action = new BitXor();
    } else if (name.equals("call")) {
      action = new Call();
    } else if (name.equals("callfunction")) {
      action = new CallFunction();
    } else if (name.equals("callmethod")) {
      action = new CallMethod();
    } else if (name.equals("castop")) {
      action = new CastOp();
    } else if (name.equals("chartoascii")) {
      action = new CharToAscii();
    } else if (name.equals("clonesprite")) {
      action = new CloneSprite();
    } else if (name.equals("constantpool")) {
      action = readConstantPool(element);
    } else if (name.equals("decrement")) {
      action = new Decrement();
    } else if (name.equals("definefunction")) {
      action = readDefineFunction(element);
    } else if (name.equals("definefunction2")) {
      action = readDefineFunction2(element);
    } else if (name.equals("definelocal")) {
      action = new DefineLocal();
    } else if (name.equals("definelocal2")) {
      action = new DefineLocal2();
    } else if (name.equals("delete")) {
      action = new Delete();
    } else if (name.equals("delete2")) {
      action = new Delete2();
    } else if (name.equals("divide")) {
      action = new Divide();
    } else if (name.equals("enddrag")) {
      action = new EndDrag();
    } else if (name.equals("enumerate")) {
      action = new Enumerate();
    } else if (name.equals("enumerate2")) {
      action = new Enumerate2();
    } else if (name.equals("equals")) {
      action = new Equals();
    } else if (name.equals("equals2")) {
      action = new Equals2();
    } else if (name.equals("extends")) {
      action = new Extends();
    } else if (name.equals("getmember")) {
      action = new GetMember();
    } else if (name.equals("getproperty")) {
      action = new GetProperty();
    } else if (name.equals("gettime")) {
      action = new GetTime();
    } else if (name.equals("geturl")) {
      action = readGetURL(element);
    } else if (name.equals("geturl2")) {
      action = readGetURL2(element);
    } else if (name.equals("getvariable")) {
      action = new GetVariable();
    } else if (name.equals("gotoframe")) {
      action = readGoToFrame(element);
    } else if (name.equals("gotoframe2")) {
      action = readGoToFrame2(element);
    } else if (name.equals("gotolabel")) {
      action = readGoToLabel(element);
    } else if (name.equals("greater")) {
      action = new Greater();
    } else if (name.equals("if")) {
      action = readIf(element);
    } else if (name.equals("implementsop")) {
      action = new ImplementsOp();
    } else if (name.equals("increment")) {
      action = new Increment();
    } else if (name.equals("initarray")) {
      action = new InitArray();
    } else if (name.equals("initobject")) {
      action = new InitObject();
    } else if (name.equals("instanceof")) {
      action = new InstanceOf();
    } else if (name.equals("jump")) {
      action = readJump(element);
    } else if (name.equals("less")) {
      action = new Less();
    } else if (name.equals("less2")) {
      action = new Less2();
    } else if (name.equals("mbasciitochar")) {
      action = new MBAsciiToChar();
    } else if (name.equals("mbchartoascii")) {
      action = new MBCharToAscii();
    } else if (name.equals("mbstringextract")) {
      action = new MBStringExtract();
    } else if (name.equals("mbstringlength")) {
      action = new MBStringLength();
    } else if (name.equals("modulo")) {
      action = new Modulo();
    } else if (name.equals("multiply")) {
      action = new Multiply();
    } else if (name.equals("newmethod")) {
      action = new NewMethod();
    } else if (name.equals("newobject")) {
      action = new NewObject();
    } else if (name.equals("nextframe")) {
      action = new NextFrame();
    } else if (name.equals("not")) {
      action = new Not();
    } else if (name.equals("or")) {
      action = new Or();
    } else if (name.equals("play")) {
      action = new Play();
    } else if (name.equals("pop")) {
      action = new Pop();
    } else if (name.equals("previousframe")) {
      action = new PreviousFrame();
    } else if (name.equals("push")) {
      action = readPush(element);
    } else if (name.equals("pushduplicate")) {
      action = new PushDuplicate();
    } else if (name.equals("randomnumber")) {
      action = new RandomNumber();
    } else if (name.equals("removesprite")) {
      action = new RemoveSprite();
    } else if (name.equals("return")) {
      action = new Return();
    } else if (name.equals("setmember")) {
      action = new SetMember();
    } else if (name.equals("setproperty")) {
      action = new SetProperty();
    } else if (name.equals("settarget")) {
      action = readSetTarget(element);
    } else if (name.equals("settarget2")) {
      action = new SetTarget2();
    } else if (name.equals("setvariable")) {
      action = new SetVariable();
    } else if (name.equals("stackswap")) {
      action = new StackSwap();
    } else if (name.equals("startdrag")) {
      action = new StartDrag();
    } else if (name.equals("stop")) {
      action = new Stop();
    } else if (name.equals("stopsounds")) {
      action = new StopSounds();
    } else if (name.equals("storeregister")) {
      action = readStoreRegister(element);
    } else if (name.equals("strictequals")) {
      action = new StrictEquals();
    } else if (name.equals("stringadd")) {
      action = new StringAdd();
    } else if (name.equals("stringequals")) {
      action = new StringEquals();
    } else if (name.equals("stringextract")) {
      action = new StringExtract();
    } else if (name.equals("stringgreater")) {
      action = new StringGreater();
    } else if (name.equals("stringlength")) {
      action = new StringLength();
    } else if (name.equals("stringless")) {
      action = new StringLess();
    } else if (name.equals("subtract")) {
      action = new Subtract();
    } else if (name.equals("targetpath")) {
      action = new TargetPath();
    } else if (name.equals("throw")) {
      action = new Throw();
    } else if (name.equals("tointeger")) {
      action = new ToInteger();
    } else if (name.equals("tonumber")) {
      action = new ToNumber();
    } else if (name.equals("tostring")) {
      action = new ToString();
    } else if (name.equals("togglequality")) {
      action = new ToggleQuality();
    } else if (name.equals("trace")) {
      action = new Trace();
    } else if (name.equals("try")) {
      action = readTry(element);
    } else if (name.equals("typeof")) {
      action = new TypeOf();
    } else if (name.equals("waitforframe")) {
      action = readWaitForFrame(element);
    } else if (name.equals("waitforframe2")) {
      action = readWaitForFrame2(element);
    } else if (name.equals("with")) {
      action = readWith(element);
    } else if (name.equals("unknownaction")) {
      action = readUnknownAction(element);
    } else {
      throw new IllegalArgumentException(
        "Unexpected action record name: " + name);
    }
    Attribute label = element.attribute("label");
    if (label != null) {
      action.setLabel(label.getValue());
    }
    return action;
  }

  private static ConstantPool readConstantPool(Element element) {
    List constantElements     = element.elements();
    ConstantPool constantPool = new ConstantPool();
    List constants            = constantPool.getConstants();
    for (Iterator it = constantElements.iterator(); it.hasNext();) {
      Element constantElement = (Element) it.next();
      String content          = RecordXMLReader.getElement(
          "value", constantElement).getText();
      String constant         = (RecordXMLReader.getBooleanAttribute(
          "base64", constantElement)) ? Base64.decodeString(content) : content;
      constants.add(constant);
    }
    return constantPool;
  }

  private static DefineFunction readDefineFunction(Element element) {
    String name            = RecordXMLReader.getStringAttribute(
        "name", element);
    List parameterElements = RecordXMLReader.getElement("parameters", element)
                                            .elements();
    int arrayLength        = parameterElements.size();
    String[] parameters    = new String[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element parameterElement = (Element) parameterElements.get(i);
      parameters[i] = RecordXMLReader.getStringAttribute(
          "name", parameterElement);
    }
    DefineFunction defineFunction = new DefineFunction(name, parameters);
    RecordXMLReader.readActionBlock(defineFunction.getBody(), element);
    return defineFunction;
  }

  private static DefineFunction2 readDefineFunction2(Element element) {
    String name                = RecordXMLReader.getStringAttribute(
        "name", element);
    short registerCount        = RecordXMLReader.getShortAttribute(
        "registercount", element);
    List parameterElements     = RecordXMLReader.getElement(
        "parameters", element).elements();
    int arrayLength            = parameterElements.size();
    RegisterParam[] parameters = new RegisterParam[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element parameterElement = (Element) parameterElements.get(i);
      String paramName         = RecordXMLReader.getStringAttribute(
          "name", parameterElement);
      short register           = RecordXMLReader.getShortAttribute(
          "register", parameterElement);
      parameters[i]            = new RegisterParam(register, paramName);
    }
    DefineFunction2 defineFunction2 = new DefineFunction2(
        name, registerCount, parameters);
    Element preloadElement          = RecordXMLReader.getElement(
        "preload", element);
    if (RecordXMLReader.getBooleanAttribute("arguments", preloadElement)) {
      defineFunction2.preloadArguments();
    }
    if (RecordXMLReader.getBooleanAttribute("global", preloadElement)) {
      defineFunction2.preloadGlobal();
    }
    if (RecordXMLReader.getBooleanAttribute("parent", preloadElement)) {
      defineFunction2.preloadParent();
    }
    if (RecordXMLReader.getBooleanAttribute("root", preloadElement)) {
      defineFunction2.preloadRoot();
    }
    if (RecordXMLReader.getBooleanAttribute("super", preloadElement)) {
      defineFunction2.preloadSuper();
    }
    if (RecordXMLReader.getBooleanAttribute("this", preloadElement)) {
      defineFunction2.preloadThis();
    }
    Element suppressElement = RecordXMLReader.getElement("suppress", element);
    if (RecordXMLReader.getBooleanAttribute("arguments", suppressElement)) {
      defineFunction2.suppressArguments();
    }
    if (RecordXMLReader.getBooleanAttribute("super", suppressElement)) {
      defineFunction2.suppressSuper();
    }
    if (RecordXMLReader.getBooleanAttribute("this", suppressElement)) {
      defineFunction2.suppressThis();
    }
    RecordXMLReader.readActionBlock(defineFunction2.getBody(), element);
    return defineFunction2;
  }

  private static GetURL readGetURL(Element element) {
    String url    = RecordXMLReader.getStringAttribute("url", element);
    String target = RecordXMLReader.getStringAttribute("target", element);
    return new GetURL(url, target);
  }

  private static GetURL2 readGetURL2(Element element) {
    String sendVarsMethodString = RecordXMLReader.getStringAttribute(
        "sendvarsmethod", element);
    byte sendVarsMethod;
    if (sendVarsMethodString.equals("get")) {
      sendVarsMethod = GetURL2.METHOD_GET;
    } else if (sendVarsMethodString.equals("post")) {
      sendVarsMethod = GetURL2.METHOD_POST;
    } else if (sendVarsMethodString.equals("none")) {
      sendVarsMethod = GetURL2.METHOD_NONE;
    } else {
      throw new IllegalArgumentException(
        "Illegal sendvars method: " + sendVarsMethodString);
    }
    boolean loadTarget    = RecordXMLReader.getBooleanAttribute(
        "loadtarget", element);
    boolean loadVariables = RecordXMLReader.getBooleanAttribute(
        "loadvariables", element);
    return new GetURL2(sendVarsMethod, loadTarget, loadVariables);
  }

  private static GoToFrame readGoToFrame(Element element) {
    return new GoToFrame(RecordXMLReader.getIntAttribute("frame", element));
  }

  private static GoToFrame2 readGoToFrame2(Element element) {
    boolean play  = RecordXMLReader.getBooleanAttribute("play", element);
    int sceneBias = RecordXMLReader.getIntAttribute("scenebias", element);
    return new GoToFrame2(play, sceneBias);
  }

  private static GoToLabel readGoToLabel(Element element) {
    return new GoToLabel(
      RecordXMLReader.getStringAttribute("framelabel", element));
  }

  private static If readIf(Element element) {
    return new If(RecordXMLReader.getStringAttribute("branchlabel", element));
  }

  private static Jump readJump(Element element) {
    return new Jump(RecordXMLReader.getStringAttribute("branchlabel", element));
  }

  private static Push readPush(Element element) {
    List valueElements = element.elements();
    Push push          = new Push();
    for (Iterator it = valueElements.iterator(); it.hasNext();) {
      Element valueElement  = (Element) it.next();
      String type           = valueElement.getName();
      Push.StackValue value = new Push.StackValue();
      if (type.equals("boolean")) {
        value.setBoolean(
          RecordXMLReader.getBooleanAttribute("value", valueElement));
      } else if (type.equals("constant16")) {
        value.setConstant16(
          RecordXMLReader.getIntAttribute("id", valueElement));
      } else if (type.equals("constant8")) {
        value.setConstant8(
          RecordXMLReader.getShortAttribute("id", valueElement));
      } else if (type.equals("double")) {
        value.setDouble(
          RecordXMLReader.getDoubleAttribute("value", valueElement));
      } else if (type.equals("float")) {
        value.setFloat(
          RecordXMLReader.getFloatAttribute("value", valueElement));
      } else if (type.equals("integer")) {
        value.setInteger(
          RecordXMLReader.getIntAttribute("value", valueElement));
      } else if (type.equals("null")) {
        value.setNull();
      } else if (type.equals("register")) {
        value.setRegisterNumber(
          RecordXMLReader.getShortAttribute("number", valueElement));
      } else if (type.equals("string")) {
        value.setString(
          RecordXMLReader.getStringAttribute("value", valueElement));
      } else if (type.equals("undefined")) {
        value.setUndefined();
      } else {
        throw new IllegalArgumentException(
          "Unexpected stack value type: " + type);
      }
      push.addValue(value);
    }
    return push;
  }

  private static SetTarget readSetTarget(Element element) {
    return new SetTarget(RecordXMLReader.getStringAttribute("name", element));
  }

  private static StoreRegister readStoreRegister(Element element) {
    return new StoreRegister(
      RecordXMLReader.getShortAttribute("number", element));
  }

  private static Try readTry(Element element) {
    Try tryAction;
    Attribute catchRegister = element.attribute("catchregister");
    if (catchRegister != null) {
      tryAction = new Try(Short.parseShort(catchRegister.getValue()));
    } else {
      Attribute catchVariable = element.attribute("catchvariable");
      if (catchVariable != null) {
        tryAction = new Try(catchVariable.getValue());
      } else {
        throw new MissingNodeException(
          "Neither catch register nor catch variable specified within try action!");
      }
    }
    RecordXMLReader.readActionBlock(
      tryAction.getTryBlock(), RecordXMLReader.getElement("try", element));
    Element catchElement = element.element("catch");
    if (catchElement != null) {
      RecordXMLReader.readActionBlock(tryAction.getCatchBlock(), catchElement);
    }
    Element finallyElement = element.element("finally");
    if (finallyElement != null) {
      RecordXMLReader.readActionBlock(
        tryAction.getFinallyBlock(), finallyElement);
    }
    return tryAction;
  }

  private static UnknownAction readUnknownAction(Element element) {
    return new UnknownAction(
      RecordXMLReader.getShortAttribute("code", element),
      Base64.decode(element.getText()));
  }

  private static WaitForFrame readWaitForFrame(Element element) {
    int frame       = RecordXMLReader.getIntAttribute("frame", element);
    short skipCount = RecordXMLReader.getShortAttribute("skipcount", element);
    return new WaitForFrame(frame, skipCount);
  }

  private static WaitForFrame2 readWaitForFrame2(Element element) {
    return new WaitForFrame2(
      RecordXMLReader.getShortAttribute("skipcount", element));
  }

  private static With readWith(Element element) {
    With with = new With();
    RecordXMLReader.readActionBlock(with.getWithBlock(), element);
    return with;
  }
}
