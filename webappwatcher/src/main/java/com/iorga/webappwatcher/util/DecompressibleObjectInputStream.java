package com.iorga.webappwatcher.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.WriteAbortedException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// from http://stackoverflow.com/a/1816711/535203 and http://stackoverflow.com/a/20547036/535203
public class DecompressibleObjectInputStream extends ObjectInputStream {
	private static Logger logger = LoggerFactory.getLogger(DecompressibleObjectInputStream.class);

	public DecompressibleObjectInputStream(final InputStream in) throws IOException {
		super(in);

		try {
			// activating override on readObject thanks to http://stackoverflow.com/a/3301720/535203
			final Field enableOverrideField = ObjectInputStream.class.getDeclaredField("enableOverride");

			enableOverrideField.setAccessible(true);

			final Field fieldModifiersField = Field.class.getDeclaredField("modifiers");
			fieldModifiersField.setAccessible(true);
			fieldModifiersField.setInt(enableOverrideField, enableOverrideField.getModifiers() & ~Modifier.FINAL);

			enableOverrideField.set(this, true);
		} catch (final NoSuchFieldException e) {
			warnCantOverride(e);
		} catch (final SecurityException e) {
			warnCantOverride(e);
		} catch (final IllegalArgumentException e) {
			warnCantOverride(e);
		} catch (final IllegalAccessException e) {
			warnCantOverride(e);
		}
	}

	private void warnCantOverride(final Exception e) {
		logger.warn("Couldn't enable readObject override, won't be able to avoid ClassNotFoundException while reading InputStream", e);
	}

	@Override
	protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
		ObjectStreamClass resultClassDescriptor = super.readClassDescriptor(); // initially streams descriptor
		Class<?> localClass; // the class in the local JVM that this descriptor represents.
		try {
			localClass = Class.forName(resultClassDescriptor.getName());
		} catch (final ClassNotFoundException e) {
			logger.warn("No local class for " + resultClassDescriptor.getName(), e);
			return resultClassDescriptor;
		}
		final ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookup(localClass);
		if (localClassDescriptor != null) { // only if class implements serializable
			final long localSUID = localClassDescriptor.getSerialVersionUID();
			final long streamSUID = resultClassDescriptor.getSerialVersionUID();
			if (streamSUID != localSUID) { // check for serialVersionUID mismatch.
				final StringBuffer s = new StringBuffer("Overriding serialized class version mismatch: ");
				s.append("local serialVersionUID = ").append(localSUID);
				s.append(" stream serialVersionUID = ").append(streamSUID);
				final Exception e = new InvalidClassException(s.toString());
				logger.warn("Potentially Fatal Deserialization Operation.", e);
				resultClassDescriptor = localClassDescriptor; // Use local class descriptor for deserialization
			}
		}
		return resultClassDescriptor;
	}

	@Override
	public void defaultReadObject() throws IOException, ClassNotFoundException {
		try {
			super.defaultReadObject();
		} catch (final ClassNotFoundException e) {
			logger.warn("Potentially Fatal Deserialization Operation.", e);
		} catch (final WriteAbortedException e) {
			logger.warn("Potentially Fatal Deserialization Operation.", e);
			try {
				callObjectInputStreamMethod("clear", new Class<?>[] {});
			} catch (final Throwable e1) {
				logger.error("Problem while clearing internal structures.", e1);
			}
		}
	}

	@Override
	protected Object readObjectOverride() throws IOException, ClassNotFoundException {
        // copy of JDK 7 code avoiding the ClassNotFoundException to be thrown :
		/*
			// if nested read, passHandle contains handle of enclosing object
	        int outerHandle = passHandle;
	        try {
	            Object obj = readObject0(false);
	            handles.markDependency(outerHandle, passHandle);
	            ClassNotFoundException ex = handles.lookupException(passHandle);
	            if (ex != null) {
	                throw ex;
	            }
	            if (depth == 0) {
	                vlist.doCallbacks();
	            }
	            return obj;
	        } finally {
	            passHandle = outerHandle;
	            if (closed && depth == 0) {
	                clear();
	            }
	        }
		 */
		try {
	        final int outerHandle = getObjectInputStreamFieldValue("passHandle");
	        final int depth = getObjectInputStreamFieldValue("depth");
	        try {
	            final Object obj = callObjectInputStreamMethod("readObject0", new Class<?>[] {boolean.class}, false);
	            final Object handles = getObjectInputStreamFieldValue("handles");
	            final Object passHandle = getObjectInputStreamFieldValue("passHandle");
	            callMethod(handles, "markDependency", new Class<?>[] {int.class, int.class}, outerHandle, passHandle);

	            final ClassNotFoundException ex = callMethod(handles, "lookupException", new Class<?>[] {int.class},  passHandle);

	            if (ex != null) {
	                logger.warn("Avoiding exception", ex);
	            }
	            if (depth == 0) {
	            	callMethod(getObjectInputStreamFieldValue("vlist"), "doCallbacks", new Class<?>[] {});
	            }
	            return obj;
	        } finally {
	        	getObjectInputStreamField("passHandle").setInt(this, outerHandle);
	        	final boolean closed = getObjectInputStreamFieldValue("closed");
	            if (closed && depth == 0) {
	                callObjectInputStreamMethod("clear", new Class<?>[] {});
	            }
	        }
		} catch (final NoSuchFieldException e) {
			throw createCantMimicReadObject(e);
		} catch (final SecurityException e) {
			throw createCantMimicReadObject(e);
		} catch (final IllegalArgumentException e) {
			throw createCantMimicReadObject(e);
		} catch (final IllegalAccessException e) {
			throw createCantMimicReadObject(e);
		} catch (final InvocationTargetException e) {
			throw createCantMimicReadObject(e);
		} catch (final NoSuchMethodException e) {
			throw createCantMimicReadObject(e);
		} catch (final Throwable t) {
			if (t instanceof RuntimeException) {
				throw (RuntimeException) t;
			}
			if (t instanceof IOException) {
				throw (IOException)t;
			}
			throw createCantMimicReadObject(t);
		}
	}

	private IllegalStateException createCantMimicReadObject(final Throwable t) {
		return new IllegalStateException("Can't mimic JDK readObject method", t);
	}

	@SuppressWarnings("unchecked")
	private <T> T getObjectInputStreamFieldValue(final String fieldName) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		final Field declaredField = getObjectInputStreamField(fieldName);
		return (T) declaredField.get(this);
	}

	private Field getObjectInputStreamField(final String fieldName) throws NoSuchFieldException {
		final Field declaredField = ObjectInputStream.class.getDeclaredField(fieldName);
		declaredField.setAccessible(true);
		return declaredField;
	}

	@SuppressWarnings("unchecked")
	private <T> T callObjectInputStreamMethod(final String methodName, final Class<?>[] parameterTypes, final Object... args) throws Throwable {
		final Method declaredMethod = ObjectInputStream.class.getDeclaredMethod(methodName, parameterTypes);
		declaredMethod.setAccessible(true);
		try {
			return (T) declaredMethod.invoke(this, args);
		} catch (final InvocationTargetException e) {
			throw e.getCause();
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T callMethod(final Object object, final String methodName, final Class<?>[] parameterTypes, final Object... args) throws Throwable {
		final Method declaredMethod = object.getClass().getDeclaredMethod(methodName, parameterTypes);
		declaredMethod.setAccessible(true);
		try {
			return (T) declaredMethod.invoke(object, args);
		} catch (final InvocationTargetException e) {
			throw e.getCause();
		}
	}
}
