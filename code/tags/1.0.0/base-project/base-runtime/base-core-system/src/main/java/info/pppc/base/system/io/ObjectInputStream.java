package info.pppc.base.system.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

/**
 * This simple (and rather slow) ObjectInputStream is capable of reading objects
 * that have been serialized with an ObjectOutputStream. It supports primitive
 * arrays, object arrays, primitive type wrapper arrays and objects with one of the
 * following types java.util.Hashtable, java.util.Stack, java.util.Vector and 
 * base.system.io.Serializable.
 * 
 * @author Marcus Handte
 */
public final class ObjectInputStream extends InputStream implements IObjectInput {

	/**
	 * An object vector used to replace object references for a
	 * general cycle detection.
	 */
	private Vector objects = new Vector();
	
	/**
	 * A counter that counts the reentrancies. Whenever the counter
	 * is decremented to 0, the replacement buffer will be cleared.
	 */
	private int reentrance = 0;

	/**
	 * The underlying DataInputStream to read from.
	 */
	private DataInputStream stream;

	/**
	 * Creates a new ObjectInputStream that uses the passed DataInputStream
	 * to read from.
	 * 
	 * @param stream The DataInputStream to read from.
	 */
	public ObjectInputStream(InputStream stream) {
		if (stream instanceof DataInputStream) {
			this.stream = (DataInputStream)stream;
		} else {
			this.stream = new DataInputStream(stream);	
		}
	}

	/**
	 * Reads a byte from the underlying DataInputStream.
	 * 
	 * @return The byte or -1 if the end of the stream has been reached.
	 * @throws IOException Thrown if the underlying stream throws an
	 * 	exception.
	 */
	public int read() throws IOException {
		return stream.read();
	}

	/**
	 * Reads an object from the underlying DataInputStream.
	 * 
	 * @return The object that has been read from the stream.
	 * @throws IOException Thrown if the object could not be read.
	 */
	public Object readObject() throws IOException {
		reentrance += 1;
		Object object = internalReadObject();
		reentrance -= 1;
		if (reentrance == 0) {
			objects.removeAllElements();
		}
		return object;
	}

	/**
	 * Reads an object from the underlying DataInputStream and does
	 * not perform any reference management.
	 * 
	 * @return The object that has been read from the stream.
	 * @throws IOException Thrown if the object could not be read.
	 */
	private Object internalReadObject() throws IOException {
		String cls = stream.readUTF();
		if (cls.equals(ObjectStreamTranslator.ABBREVIATION_REFERENCE)) {
			// read a cyclic reference 
			int position = stream.readInt();
			if (objects.size() > position) {
				return objects.elementAt(position);
			} else {
				throw new IOException("Found an illegal reference.");	
			}
		} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_NULL)) {
			// read a null value
			return null;
		} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_STRING)) { 
			String s = stream.readUTF();
			objects.addElement(s);
			return s;
		} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_OBJECT)) { 
			Object o = new Object();
			objects.addElement(o);
			return o;
		} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_HASHTABLE)) {
			int length = stream.readInt();
			Hashtable result = new Hashtable();
			objects.addElement(result);
			for (int i = length - 1; i >= 0 ; i--) {
				result.put(internalReadObject(), internalReadObject());
			}
			return result;
		} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_INTEGER)) {
			Integer i = new Integer(stream.readInt());
			objects.addElement(i);
			return i;
		} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_LONG)) {
			Long l = new Long(stream.readLong());
			objects.addElement(l);
			return l;
		} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_SHORT)) {
			Short s = new Short(stream.readShort());
			objects.addElement(s);
			return s;
		} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_BOOLEAN)) {
			Boolean b = new Boolean(stream.readBoolean());
			objects.addElement(b);
			return b;
		} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_BYTE)) {
			Byte b = new Byte(stream.readByte());
			objects.addElement(b);
			return b;
		} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_CHARACTER)) {
			Character c = new Character(stream.readChar());
			objects.addElement(c);
			return c;
		}  else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_STACK)) {
			int length = stream.readInt();
			Stack result = new Stack();
			objects.addElement(result);
			for (int i = length - 1; i >= 0; i--) {
				result.insertElementAt(internalReadObject(), 0);
			}
			return result;
		} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_VECTOR)) {
			int length = stream.readInt();
			Vector result = new Vector(length==0?1:length);
			objects.addElement(result);
			for (int i = length - 1; i >= 0; i--) {
				result.insertElementAt(internalReadObject(), 0);
			}
			return result;
		} else {
			String classname = ObjectStreamTranslator.getClassname(cls);
			if (classname.startsWith("[")) {
				int length = stream.readInt();
				if (cls.equals(ObjectStreamTranslator.ABBREVIATION_ARRAY_OBJECT)) {
					Object[] result = new Object[length];
					objects.addElement(result);
					for (int i = result.length - 1; i >= 0 ; i--) {
						result[i] = internalReadObject();
					}
					return result;
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_ARRAY_INTEGER)) {
					Integer[] result = new Integer[length];
					objects.addElement(result);
					for (int i = result.length - 1; i >= 0 ; i--) {
						result[i] = (Integer)internalReadObject();
					}
					return result;
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_ARRAY_SHORT)) {
					Short[] result = new Short[length];
					objects.addElement(result);
					for (int i = result.length - 1; i >= 0 ; i--) {
						result[i] = (Short)internalReadObject();
					}
					return result;
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_ARRAY_LONG)) {
					Long[] result = new Long[length];
					objects.addElement(result);
					for (int i = result.length - 1; i >= 0 ; i--) {
						result[i] = (Long)internalReadObject();
					}
					return result;
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_ARRAY_BOOLEAN)) {
					Boolean[] result = new Boolean[length];
					objects.addElement(result);
					for (int i = result.length - 1; i >= 0 ; i--) {
						result[i] = (Boolean)internalReadObject();
					}
					return result;
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_ARRAY_BYTE)) {
					Byte[] result = new Byte[length];
					objects.addElement(result);
					for (int i = result.length - 1; i >= 0 ; i--) {
						result[i] = (Byte)internalReadObject();
					}
					return result;
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_ARRAY_CHARACTER)) {
					Character[] result = new Character[length];
					objects.addElement(result);
					for (int i = result.length - 1; i >= 0 ; i--) {
						result[i] = (Character)internalReadObject();
					}
					return result;
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_ARRAY_STRING)) {
					String[] result = new String[length];
					objects.addElement(result);
					for (int i = result.length - 1; i >= 0 ; i--) {
						result[i] = (String)internalReadObject();
					}
					return result;
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_PRIMITIVE_INT)) {
					int[] intArray = new int[length];
			 		objects.addElement(intArray);
			 		for (int i = intArray.length - 1; i >= 0 ; i--) {
						intArray[i] = stream.readInt();
			 		}
			 		return intArray;
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_PRIMITIVE_LONG)) {
					long[] longArray = new long[length];
					objects.addElement(longArray);
					for (int i = longArray.length - 1; i >= 0 ; i--) {
						longArray[i] = stream.readLong();
					}
					return longArray;
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_PRIMITIVE_SHORT)) {
					short[] shortArray = new short[length];
					objects.addElement(shortArray);
					for (int i = shortArray.length - 1; i >= 0 ; i--) {
						shortArray[i] = stream.readShort();
					}
					return shortArray;
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_PRIMITIVE_BOOLEAN)) {
					boolean[] booleanArray = new boolean[length];
					objects.addElement(booleanArray);
					for (int i = booleanArray.length - 1; i >= 0 ; i--) {
						booleanArray[i] = stream.readBoolean();
					}
					return booleanArray;
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_PRIMITIVE_BYTE)) {
					byte[] byteArray = new byte[length];
					objects.addElement(byteArray);
					int total = stream.read(byteArray);
					while (total != length) {
						int read = stream.read(byteArray, total, length - total);
						if (read == -1) throw new IOException("End of stream");
						else total += read;
					}
					return byteArray;
				} else if (cls.equals(ObjectStreamTranslator.ABBREVIATION_PRIMITIVE_CHAR)) {
					char[] charArray = new char[length];
					objects.addElement(charArray);
					for (int i = charArray.length - 1; i >= 0 ; i--) {
						charArray[i] = stream.readChar();
					}
					return charArray;		
				} else {
					throw new IOException("Found an unknown array type (" + classname + ").");
				}
			} else {
				// read a compound object
				try {
					Class c = Class.forName(classname);
					Object o = c.newInstance();
					if (o instanceof ISerializable) { 
						ISerializable s = (ISerializable)o;
						objects.addElement(o);
						s.readObject(this);
						return s;
					} else if (o instanceof Throwable) {
						objects.addElement(o);
						// do nothing only deserialize type
						return o;	
					} else {
						throw new IOException("Type " + classname + " is not serializable.");
					}				
				} catch (ClassNotFoundException cnfe) {
					throw new IOException("Type " + classname + " cannot be found.");
				} catch (IllegalAccessException iae) {
					throw new IOException("Type " + classname + " cannot be accessed.");
				} catch (InstantiationException ie) {
					throw new IOException("Type " + classname + " cannot be instanciated.");
				}				
			}
		}
	}
	

	/**
	 * Reads a boolean.
	 *
	 * @return The next boolean.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public boolean readBoolean() throws IOException {
		return stream.readBoolean();
	}

	/**
	 * Reads a char.
	 *
	 * @return The next char.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public char readChar() throws IOException {
		return stream.readChar();
	}

	/**
	 * Reads a 32 bit integer.
	 *
	 * @return The next 32 bit integer.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public int readInt() throws IOException {
		return stream.readInt();
	}

	/**
	 * Reads a 64 bit integer.
	 *
	 * @return The next 64 bit integer.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public long readLong() throws IOException {
		return stream.readLong();
	}

	/**
	 * Reads a 16 bit integer.
	 *
	 * @return The next 16 bit integer.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public short readShort() throws IOException {
		return stream.readShort();
	}

	/**
	 * Reads a utf encoded string.
	 *
	 * @return The next utf encoded string.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public String readUTF() throws IOException {
		return stream.readUTF();
	}
	
	/**
	 * Reads a byte.
	 *
	 * @return The next byte.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public byte readByte() throws IOException {
		return stream.readByte();
	}

	/**
	 * Reads a number of bytes and writes them into the buffer.
	 * The number of bytes read is defined by the length of the
	 * buffer.
	 * 
	 * @param buffer The buffer to fill.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public void readBytes(byte[] buffer) throws IOException {
		readBytes(buffer, 0, buffer.length);
	}
	
	/**
	 * Reads a number of bytes and writes them into the buffer
	 * starting from the specified offset. The number of bytes
	 * read is defined by the length parameter.
	 * 
	 * @param buffer The buffer to fill.
	 * @param offset The offset to start from.
	 * @param length The number of bytes to fill.
	 * @throws IOException Thrown if a problem occurs.
	 */
	public void readBytes(byte[] buffer, int offset, int length) throws IOException {
		while (length > 0) {
			int read = stream.read(buffer, offset, length);
			if (read != -1) {
				offset += read;
				length -= read;				
			} else {
				throw new IOException("End of stream reached.");
			}
		}
	}
	
	/**
	 * Closes the underlying DataInputStream.
	 * 
	 * @throws IOException Thrown if closing the underlying stream caused an 
	 * 	exception.
	 */
	public void close() throws IOException {
		objects = null;
		stream.close();
	}

}
