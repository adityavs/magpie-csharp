package com.stuffwithstuff.magpie.interpreter;

import java.util.Map.Entry;

import com.stuffwithstuff.magpie.ast.*;

public abstract class NativeMethod implements Invokable {
  public abstract Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg);
  
  // TODO(bob): Many of these just use dynamic in their type signature. Would be
  // good to change to something more specific when possible.
  
  // Native methods:
  
  // Bool methods:
  
  public static class BoolNot extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createBool(!thisObj.asBool());
    }
    
    public Expr getParamType() { return new NameExpr("Nothing"); }
    public Expr getReturnType() { return new NameExpr("Bool"); }
  }
  
  public static class BoolToString extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString(Boolean.toString(thisObj.asBool()));
    }
    
    public Expr getParamType() { return new NameExpr("Nothing"); }
    public Expr getReturnType() { return new NameExpr("String"); }
  }
  
  // Class methods:
  
  public static class ClassAddMethod extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj method = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;
      classObj.addMethod(name, method);
      
      return interpreter.nothing();
    }
    
    // TODO(bob): Should be tuple.
    public Expr getParamType() { return new NameExpr("Dynamic"); }
    public Expr getReturnType() { return new NameExpr("Nothing"); }
  }
  
  public static class ClassAddSharedMethod extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String name = arg.getTupleField(0).asString();
      FnObj method = (FnObj)arg.getTupleField(1);
      
      ClassObj classObj = (ClassObj)thisObj;
      ClassObj metaclass = classObj.getClassObj();
      metaclass.addMethod(name, method);

      return interpreter.nothing();
    }
    
    // TODO(bob): Should be tuple.
    public Expr getParamType() { return new NameExpr("Dynamic"); }
    public Expr getReturnType() { return new NameExpr("Nothing"); }
  }
  
  public static class ClassGetName extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = (ClassObj)thisObj;
      
      return interpreter.createString(classObj.getName());
    }

    public Expr getParamType() { return new NameExpr("Nothing"); }
    public Expr getReturnType() { return new NameExpr("String"); }
  }
  
  public static class ClassGetParent extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = (ClassObj)thisObj;
      
      ClassObj parent = classObj.getParent();
      
      // If a class has no parent, its parent is implicitly Object.
      if (parent == null) return interpreter.getObjectType();
      
      return parent;
    }

    public Expr getParamType() { return new NameExpr("Nothing"); }
    public Expr getReturnType() { return new NameExpr("Class"); }
  }
  
  public static class ClassSetParent extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = (ClassObj)thisObj;
      
      if (!(arg instanceof ClassObj)) {
        interpreter.runtimeError(
            "Cannot assign %s as the base class for %s because it is not a class.",
            arg, thisObj);
        
        return interpreter.nothing();
      }
      
      classObj.setParent((ClassObj)arg);
      return arg;
    }

    public Expr getParamType() { return new NameExpr("Class"); }
    public Expr getReturnType() { return new NameExpr("Class"); }
  }

  public static class ClassFieldGetter extends NativeMethod {
    public ClassFieldGetter(String name, Expr type) {
      mName = name;
      mType = type;
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return thisObj.getField(mName);
    }
    
    public Expr getParamType() { return new NameExpr("Nothing"); }
    public Expr getReturnType() { return mType; }

    private final String mName;
    private final Expr mType;
  }

  public static class ClassFieldSetter extends NativeMethod {
    public ClassFieldSetter(String name, Expr type) {
      mName = name;
      mType = type;
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      thisObj.setField(mName, arg);
      return arg;
    }
    
    public Expr getParamType() { return mType; }
    public Expr getReturnType() { return mType; }

    private final String mName;
    private final Expr mType;
  }
  
  // TODO(bob): This is pretty much temp.
  public static class ClassNew extends NativeMethod {
    public ClassNew(String className) {
      mClassName = className;
    }
    
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      ClassObj classObj = (ClassObj)thisObj;
      
      Obj obj = classObj.instantiate();
      
      // Create a fresh context for evaluating the field initializers so that
      // they can't erroneously access stuff around where the object is being
      // constructed.
      EvalContext fieldContext = interpreter.createTopLevelContext();
      
      // Initialize its fields.
      for (Entry<String, Expr> field : classObj.getFieldInitializers().entrySet()) {
        Obj value = interpreter.evaluate(field.getValue(), fieldContext);
        obj.setField(field.getKey(), value);
      }
      
      // Find and call the constructor (if any).
      Invokable constructor = classObj.getConstructor();
      if (constructor != null) {
        constructor.invoke(interpreter, obj, arg);
      }
      
      return obj;
    }
    
    // TODO(bob): Should be typed.
    public Expr getParamType() { return new NameExpr("Dynamic"); }
    public Expr getReturnType() { return new NameExpr(mClassName); }
    
    private final String mClassName;
  }
  
  // Function methods:
  
  public static class FunctionApply extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      FnObj function = (FnObj)thisObj;
      
      return function.invoke(interpreter, interpreter.nothing(), arg);
    }
    
    public Expr getParamType() { return new NameExpr("Int"); }
    public Expr getReturnType() { return new NameExpr("Int"); }
  }
  
  // Int methods:
  
  public static class IntPlus extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left + right);
    }
    
    public Expr getParamType() { return new NameExpr("Int"); }
    public Expr getReturnType() { return new NameExpr("Int"); }
  }

  public static class IntMinus extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left - right);
    }
    
    public Expr getParamType() { return new NameExpr("Int"); }
    public Expr getReturnType() { return new NameExpr("Int"); }
  }
  
  public static class IntMultiply extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left * right);
    }
    
    public Expr getParamType() { return new NameExpr("Int"); }
    public Expr getReturnType() { return new NameExpr("Int"); }
  }
  
  public static class IntDivide extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createInt(left / right);
    }
    
    public Expr getParamType() { return new NameExpr("Int"); }
    public Expr getReturnType() { return new NameExpr("Int"); }
  }
  
  public static class IntEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left == right);
    }
    
    public Expr getParamType() { return new NameExpr("Int"); }
    public Expr getReturnType() { return new NameExpr("Bool"); }
  }
  
  public static class IntNotEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left != right);
    }
    
    public Expr getParamType() { return new NameExpr("Int"); }
    public Expr getReturnType() { return new NameExpr("Bool"); }
  }
  
  public static class IntLessThan extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left < right);
    }
    
    public Expr getParamType() { return new NameExpr("Int"); }
    public Expr getReturnType() { return new NameExpr("Bool"); }
  }
  
  public static class IntGreaterThan extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left > right);
    }
    
    public Expr getParamType() { return new NameExpr("Int"); }
    public Expr getReturnType() { return new NameExpr("Bool"); }
  }
  
  public static class IntLessThanOrEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left <= right);
    }
    
    public Expr getParamType() { return new NameExpr("Int"); }
    public Expr getReturnType() { return new NameExpr("Bool"); }
  }
  
  public static class IntGreaterThanOrEqual extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      int left = thisObj.asInt();
      int right = arg.asInt();
      
      return interpreter.createBool(left >= right);
    }
    
    public Expr getParamType() { return new NameExpr("Int"); }
    public Expr getReturnType() { return new NameExpr("Bool"); }
  }
  
  public static class IntToString extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString(Integer.toString(thisObj.asInt()));
    }
    
    public Expr getParamType() { return new NameExpr("Nothing"); }
    public Expr getReturnType() { return new NameExpr("String"); }
  }
  
  // Nothing methods:
  
  public static class NothingToString extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return interpreter.createString("()");
    }
    
    public Expr getParamType() { return new NameExpr("Nothing"); }
    public Expr getReturnType() { return new NameExpr("String"); }
  }
  
  // Object methods:
  
  public static class ObjectGetType extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return thisObj.getClassObj();
    }

    public Expr getParamType() { return new NameExpr("Nothing"); }
    public Expr getReturnType() { return new NameExpr("Class"); }
  }

  // String methods:

  public static class StringPlus extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      String left = thisObj.asString();
      String right = arg.asString();
      
      return interpreter.createString(left + right);
    }
    
    public Expr getParamType() { return new NameExpr("String"); }
    public Expr getReturnType() { return new NameExpr("String"); }
  }

  public static class StringPrint extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      interpreter.print(thisObj.asString());
      return interpreter.nothing();
    }
    
    public Expr getParamType() { return new NameExpr("Nothing"); }
    public Expr getReturnType() { return new NameExpr("Nothing"); }
  }
  
  // Tuple methods:

  public static class TupleGetField extends NativeMethod {
    @Override
    public Obj invoke(Interpreter interpreter, Obj thisObj, Obj arg) {
      return thisObj.getTupleField(arg.asInt());
    }
    
    public Expr getParamType() { return new NameExpr("Int"); }
    public Expr getReturnType() { return new NameExpr("Dynamic"); }
  }
}
