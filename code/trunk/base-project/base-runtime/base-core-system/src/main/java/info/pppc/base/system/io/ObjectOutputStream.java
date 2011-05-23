package info.pppc.base.system.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

/**
 * This simple (and rather slow) ObjectOutputStream is capable of serializing 
 * objects to a stream. It supports primitive arrays, object arrays, primitive 
 * type wrapper arrays and objects with one of the following types 
 * java.util.Hashtable, java.util.Stack, java.util.Vector and
 * base.system.io.Serializable.
 * 
 * @author Marcus Handte
 */
public final class ObjectOutputStream extends OutputStream implements IObjectOutput {

	/**
	 * The object that have been written so far, used for cycle
	 * detection.
	 */
	private Vector objects = new Vector();
	
	/**
	 * The reentrancy counter that is used to keep the number of
	 * reentrancies low and to clear the objects buffer.
	 */
	private int reentrance = 0;
	
	/**
	 * The underlying output stream to write to.
	 */
	private DataOutputStream stream;

	/**
	 * Creates a new ObjectOutputStream that writes to the specified 
	 * DataOutputStream.
	 * 
	 * @param stream The DataOutputStream to write to.
	 */
	public ObjectOutputStream(OutputStream stream) {
		if (stream instanceof DataOutputStream) {
			this.stream = (DataOutputStream)stream;
		} else {
			this.stream = new DataOutputStream(stream);	
		}
	}

	/**
	 * Writes an integer to the underlying output stream.
	 * 
	 * @param i The integer to write.
	 * @throws IOException Thrown if the underlying stream throws an 
	 * 	exception.
	 */
	public void write(int i) throws IOException {
		stream.write(i);
	}

	/**
	 * Flushes the underlying stream.
	 * 
	 * @throws IOException Thrown if the underlying stream throws an 
	 * 	exception.
	 */
	public void flush() throws IOException {
		stream.flush();
	}


	/**
	 * Closes the underlying stream.
	 * 
	 * @throws IOException Thrown if the underlying stream throws an
	 * 	exception. 
	 */
	public void close() throws IOException {
		objects = null;
		stream.close();
	}

	/**
	 * Writes the passed object to the underlying DataOutputStream and
	 * performs the necessary buffer management.
	 * 
	 * @param object The object to write.
	 * @throws IOException Thrown if an exception occurs while writing
	 * 	the object.
	 */	
	public void writeObject(Object object) throws IOException {
		reentrance += 1;
		internalWriteObject(object);
		reentrance -= 1;
		if (reentrance == 0) {
			objects.removeAllElements();
			stream.flush();
		}
	}
	
	/**
	 * Writes all objects that are not serializable. For serializable
	 * objects, only the type is written and the serialization of the
	 * content is postponed to keep the recursion depth to a minimum.
	 * 
	 * @param object The object to write.
	 * @throws IOException Thrown if the underlying stream fails or if
	 * 	the type could not be found.
	 */
	private void internalWriteObject(Object object) throws IOException {
		if (object == null) {
			stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_NULL);
		} else {
			// check if it is a cyclic reference
			for (int i = objects.size() - 1; i >= 0 ; i--) {
				if (objects.elementAt(i) == object) {
					stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_REFERENCE);
					stream.writeInt(i);
					return;
				}
			}
			// it is not a cyclic reference
			objects.addElement(object);
			if (object instanceof ISerializable) {
				stream.writeUTF(ObjectStreamTranslator.getAbbreviation(object.getClass().getName()));
				ISerializable serializable = (ISerializable)object;
				serializable.writeObject(this);
			} else if (object instanceof Object[]) {
				if (object instanceof Integer[]) {
					Integer[] array = (Integer[])object;
					stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_ARRAY_INTEGER);
					stream.writeInt(array.length);
					for (int i = array.length - 1; i >= 0; i--) {
						internalWriteObject(array[i]);
					}
				} else if (object instanceof Short[]) {
					Short[] array = (Short[])object;
					stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_ARRAY_SHORT);
					stream.writeInt(array.length);
					for (int i = array.length - 1; i >= 0; i--) {
						internalWriteObject(array[i]);
					}
				} else if (object instanceof Long[]) {
					Long[] array = (Long[])object;
					stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_ARRAY_LONG);
					stream.writeInt(array.length);
					for (int i = array.length - 1; i >= 0; i--) {
						internalWriteObject(array[i]);
					}
				} else if (object instanceof Byte[]) {
					Byte[] array = (Byte[])object;
					stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_ARRAY_BYTE);
					stream.writeInt(array.length);
					for (int i = array.length - 1; i >= 0; i--) {
						internalWriteObject(array[i]);
					}										
				} else if (object instanceof String[]) {
					String[] array = (String[])object;
					stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_ARRAY_STRING);
					stream.writeInt(array.length);
					for (int i = array.length - 1; i >= 0; i--) {
						internalWriteObject(array[i]);
					}
				} else if (object instanceof Character[]) {
					Character[] array = (Character[])object;
					stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_ARRAY_CHARACTER);
					stream.writeInt(array.length);
					for (int i = array.length - 1; i >= 0; i--) {
						internalWriteObject(array[i]);
					}
				} else if (object instanceof Boolean[]) {
					Boolean[] array = (Boolean[])object;
					stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_ARRAY_BOOLEAN);
					stream.writeInt(array.length);
					for (int i = array.length - 1; i >= 0; i--) {
						internalWriteObject(array[i]);
					}
				} else {
					String cls = ObjectStreamTranslator.getAbbreviation(object.getClass().getName());
					if (cls.equals(ObjectStreamTranslator.ABBREVIATION_ARRAY_OBJECT)) {
						Object[] array = (Object[])object;
						stream.writeUTF(cls);
						stream.writeInt(array.length);
						for (int i = array.length - 1; i >= 0; i--) {
							internalWriteObject(array[i]);
						}
					} else {
						throw new IOException("Found an unknown array type (" + cls + ")");
					}
				}
			} else if (object instanceof String) {
				stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_STRING);
				stream.writeUTF((String)object);
			} else if (object instanceof Integer) {
				stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_INTEGER);
				stream.writeInt(((Integer)object).intValue());
			} else if (object instanceof Long) {
				stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_LONG);
				stream.writeLong(((Long)object).longValue());
			} else if (object instanceof Byte) {
				stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_BYTE);
				stream.writeByte(((Byte)object).byteValue());
			} else if (object instanceof Short) {
				stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_SHORT);
				stream.writeShort(((Short)object).shortValue());
			} else if (object instanceof Boolean) {
				stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_BOOLEAN);
				stream.writeBoolean(((Boolean)object).booleanValue());
			} else if (object instanceof Character) {
				stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_CHARACTER);
				stream.writeChar(((Character)object).charValue());
			} else if (object instanceof Throwable) {
				stream.writeUTF(ObjectStreamTranslator.getAbbreviation
						(object.getClass().getName()));
			} else if (object instanceof int[]) {
				stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_PRIMITIVE_INT);
				int[] array = (int[])object;
				stream.writeInt(array.length);
				for (int i = array.length - 1; i >= 0; i--) {
					stream.writeInt(array[i]);
				}
			} else if (object instanceof long[]) {
				stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_PRIMITIVE_LONG);
				long[] array = (long[])object;
				stream.writeInt(array.length);
				for (int i = array.length - 1; i >= 0; i--) {
					stream.writeLong(array[i]);
				}
			} else if (object instanceof short[]) {
				stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_PRIMITIVE_SHORT);
				short[] array = (short[])object;
				stream.writeInt(array.length);
				for (int i = array.length - 1; i >= 0; i--) {
					stream.writeShort(array[i]);
				}
			} else if (object instanceof boolean[]) {
				stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_PRIMITIVE_BOOLEAN);
				boolean[] array = (boolean[])object;
				stream.writeInt(array.length);
				for (int i = array.length - 1; i >= 0; i--) {
					stream.writeBoolean(array[i]);
				}
			} else if (object instanceof byte[]) {
				stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_PRIMITIVE_BYTE);
				byte[] array = (byte[])object;
				stream.writeInt(array.length);
				stream.write(array);
			} else if (object instanceof char[]) {
				stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_PRIMITIVE_CHAR);
				char[] array = (char[])object;
				stream.writeInt(array.length);
				for (int i = array.length - 1; i >= 0; i--) {
					stream.writeChar(array[i]);
				}
			} else {
				String cls = ObjectStreamTranslator.getAbbreviation(object.getClass().getName());
				if (cls.equals(ObjectStreamTranslator.ABBREVIATION_STACK)) {
					stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_STACK);
					Stack stack = (Stack)object;
					stream.writeInt(stack.size());
					for (int i = stack.size() - 1; i >= 0; i--) {
						internalWriteObject(stack.elementAt(i));
					}
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_VECTOR)) {
					stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_VECTOR);
					Vector vector = (Vector)object;
					stream.writeInt(vector.size());
					for (int i = vector.size() - 1; i >= 0; i--) {
						internalWriteObject(vector.elementAt(i));
					}
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_HASHTABLE)) {
					stream.writeUTF(ObjectStreamTranslator.ABBREVIATION_HASHTABLE);
					Hashtable hashtable = (Hashtable)object;
					stream.writeInt(hashtable.size());
					Enumeration keys = hashtable.keys();
					while (keys.hasMoreElements()) {
						Object key = keys.nextElement();
						Object val = hashtable.get(key);
						internalWriteObject(key);
						internalWriteObject(val);
					}
				} else {
					throw new IOException("Found an unknown type (" + cls + ")");	
				}
			}
		}
	}

	/**
	 * Writes a boolean.
	 *
	 * @param val the boolean value to write
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeBoolean(boolean val) throws IOException {
		stream.writeBoolean(val);
	}

	/**
	 * Writes the specified 16-bit character.
	 *
	 * @param val The character to be written
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeChar(char val) throws IOException {
		stream.writeChar(val);
	}

	/**
	 * Writes a 32-bit integer. 
	 *
	 * @param val The integer to be written.
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeInt(int val) throws IOException {
		stream.writeInt(val);
	}

	/**
	 * Writes a 64-bit integer.
	 *
	 * @param val The 64-bit integer.
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeLong(long val) throws IOException {
		stream.writeLong(val);
	}

	/**
	 * Writes the specified 16-bit integer.
	 *
	 * @param val The 16-bit integer to be written.
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeShort(int val) throws IOException {
		stream.writeShort(val);
	}

	/**
	 * Writes the specified String out in UTF format.
	 *
	 * @param val The String to be written in UTF format.
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeUTF(String val) throws IOException {
		stream.writeUTF(val);
	}

	/**
	 * Writes a 8-bit byte.
	 *
	 * @param val The byte value to write
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeByte(byte val) throws IOException {
		stream.writeByte(val);
	}
	
	/**
	 * Writes a number of bytes from the byte array buffer. The number of 
	 * bytes is defined by the length of the array.
	 *
	 * @param buffer The buffer to be written
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeBytes(byte buffer[]) throws IOException {
		stream.write(buffer);
	}
	
	/**
	 * Writes a number of bytes from the byte array buffer starting at 
	 * the offset index. The number of bytes is defined by the length
	 * parameter.
	 *
	 * @param buffer The buffer to be written.
	 * @param offset The offset in buffer.
	 * @param length The number of bytes to write.
	 * @exception IOException Thrown if an problem occurs.
	 */
	public void writeBytes(byte buffer[], int offset, int length) throws IOException {
		stream.write(buffer, offset, length);
	}

}
