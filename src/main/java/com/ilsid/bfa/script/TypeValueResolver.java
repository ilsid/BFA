package com.ilsid.bfa.script;

import java.io.IOException;

import com.ilsid.bfa.common.BooleanUtil;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.JsonUtil;
import com.ilsid.bfa.persistence.DynamicClassLoader;

/**
 * Resolves variable types.
 * 
 * @author illia.sydorovych
 *
 */
public abstract class TypeValueResolver {

	protected static final String INVALID_VALUE_MSG_TPLT = "[%s] is not a value of type %s";

	protected String typeName;

	public TypeValueResolver(String typeName) {
		this.typeName = typeName;
	}

	/**
	 * Returns an instance of a proper type for the given variable value.
	 * 
	 * @param value
	 *            variable value
	 * @return an instance of a proper type
	 * @throws InvalidTypeException
	 *             if an instance of a proper type can't be created for the given variable value
	 */
	public abstract Object resolve(Object value) throws InvalidTypeException;

	/**
	 * Returns a resolver for the given java type.
	 * 
	 * @param className
	 *            java class name
	 * @return {@link TypeValueResolver} instance
	 */
	public static TypeValueResolver getResolver(String className) {
		TypeValueResolver resolver = PredefinedTypes.getResolver(className);
		if (resolver == null) {
			resolver = new EntityResolver(className);
		}

		return resolver;
	}

	static abstract class PredefinedTypeResolver extends TypeValueResolver {

		protected InvalidTypeException createInvalidTypeException(Object value) {
			return new InvalidTypeException(String.format(INVALID_VALUE_MSG_TPLT, value, typeName));
		}

		public PredefinedTypeResolver(String typeName) {
			super(typeName);
		}

	}

	static class IntegerResolver extends PredefinedTypeResolver {

		public IntegerResolver() {
			super("Number");
		}

		@Override
		public Object resolve(Object value) throws InvalidTypeException {
			if (Integer.class.isInstance(value)) {
				return value;
			}

			Integer result;
			try {
				result = Integer.valueOf(value.toString());
			} catch (NumberFormatException e) {
				throw createInvalidTypeException(value);
			}

			return result;
		}

	}

	static class DoubleResolver extends PredefinedTypeResolver {

		public DoubleResolver() {
			super("Decimal");
		}

		@Override
		public Object resolve(Object value) throws InvalidTypeException {
			if (Double.class.isInstance(value)) {
				return value;
			}

			Double result;
			try {
				result = Double.valueOf(value.toString());
			} catch (NumberFormatException e) {
				throw createInvalidTypeException(value);
			}

			return result;
		}

	}

	static class StringResolver extends PredefinedTypeResolver {

		public StringResolver() {
			super("String");
		}

		@Override
		public Object resolve(Object value) throws InvalidTypeException {
			return value.toString();
		}

	}

	static class BooleanResolver extends PredefinedTypeResolver {

		public BooleanResolver() {
			super("Boolean");
		}

		@Override
		public Object resolve(Object value) throws InvalidTypeException {
			if (Boolean.class.isInstance(value)) {
				return value;
			}

			final String stringValue = value.toString();
			if (!BooleanUtil.isBoolean(stringValue)) {
				throw createInvalidTypeException(value);
			}

			return Boolean.valueOf(stringValue);
		}

	}

	static class EntityResolver extends TypeValueResolver {

		public EntityResolver(String typeName) {
			super(typeName);
		}

		@Override
		public Object resolve(Object value) throws InvalidTypeException {
			if (value.getClass().getName().equals(typeName)) {
				return value;
			} else {
				final String valueAsString = value.toString();
				if (JsonUtil.isValidJsonString(valueAsString)) {
					final Object actualValue = tryGetActualValue(valueAsString);
					return actualValue;
				}
			}

			throw new InvalidTypeException(
					String.format(INVALID_VALUE_MSG_TPLT, value, ClassNameUtil.getShortClassName(typeName)));
		}

		private Object tryGetActualValue(String stringValue) throws InvalidTypeException {
			Object result;
			try {
				Class<?> typeClass = DynamicClassLoader.getInstance().loadClass(typeName);
				result = JsonUtil.toObject(stringValue.toString(), typeClass);
			} catch (ClassNotFoundException | IllegalStateException | IOException e) {

				throw new InvalidTypeException(
						String.format(INVALID_VALUE_MSG_TPLT, stringValue, ClassNameUtil.getShortClassName(typeName)),
						e);
			}

			return result;
		}

	}

}
