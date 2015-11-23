package randoop.operation;

import java.io.ObjectStreamException;
import java.io.Serializable;

import randoop.util.Reflection;

public class SerializableArrayCreation implements Serializable {

  private static final long serialVersionUID = 4091673456327607771L;

  private final int length;
  private final String elementType;

  public SerializableArrayCreation(Class<?> elementType, int length) {
    this.elementType = elementType.getName();
    this.length = length;
  }

  private Object readResolve() throws ObjectStreamException {
    return new ArrayCreation(Reflection.classForName(elementType), length);
  }

}