package com.ilsid.bfa.script;

import com.ilsid.bfa.common.ClassNameUtil;

/**
 * Resolves variable types.
 * 
 * @author illia.sydorovych
 *
 */
public abstract class TypeValueResolver {

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
			return new InvalidTypeException(String.format("[%s] is not a value of type %s", value, typeName));
		}

		public PredefinedTypeResolver(String typeName) {
			super(typeName);
		}

	}

	static class IntegerResolver extends PredefinedTypeResolver {

		public IntegerResolver(String typeName) {
			super(typeName);
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

		public DoubleResolver(String typeName) {
			super(typeName);
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

		public StringResolver(String typeName) {
			super(typeName);
		}

		@Override
		public Object resolve(Object value) throws InvalidTypeException {
			return value.toString();
		}

	}

	static class EntityResolver extends TypeValueResolver {

		public EntityResolver(String typeName) {
			super(typeName);
		}

		@Override
		public Object resolve(Object value) throws InvalidTypeException {
			if (!value.getClass().getName().equals(typeName)) {
				throw new InvalidTypeException(String.format("[%s] is not a value of type %s", value,
						ClassNameUtil.getShortClassName(typeName)));
			}

			return value;
		}

	}

}
