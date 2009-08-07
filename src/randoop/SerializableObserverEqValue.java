package randoop;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;

import randoop.util.Reflection;

public class SerializableObserverEqValue implements Serializable {

  private static final long serialVersionUID = 20090716L;
  private final String observer;
  private final Variable var;
  private final Object value;

  public SerializableObserverEqValue (Method observer, Variable var,
                                      Object value) {
    this.observer = Reflection.getSignature(observer);
    this.var = var;
    this.value = value;
    // System.out.printf ("Serializing %s %s %s%n", this.observer, var, value);
  }

  private Object readResolve() throws ObjectStreamException {
    Method  m = Reflection.getMethodForSignature(observer);
    return ObserverEqValue.getObserverEqValue(m, var, value);
  }

}
