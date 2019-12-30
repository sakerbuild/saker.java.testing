package saker.java.testing.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import saker.build.thirdparty.org.objectweb.asm.ClassVisitor;
import saker.build.thirdparty.org.objectweb.asm.MethodVisitor;
import saker.build.thirdparty.org.objectweb.asm.Opcodes;
import saker.build.thirdparty.org.objectweb.asm.Type;
import saker.java.testing.bootstrapagent.InstrumentationData;

class MethodInvocationLoggerClassVisitor extends ClassVisitor {
	private static final String INSTRUMENTATIONDATA_INTERNAL_NAME = Type.getInternalName(InstrumentationData.class);
	private static final String CLASS_INTERNAL_NAME = "java/lang/Class";
	private static final String CLASSLOADER_INTERNAL_NAME = "java/lang/ClassLoader";
	private static final String FIELD_INTERNAL_NAME = "java/lang/reflect/Field";
	private static final String METHOD_INTERNAL_NAME = "java/lang/reflect/Method";
	private static final String EXECUTABLE_INTERNAL_NAME = "java/lang/reflect/Executable";
	private static final String ACCESSIBLEOBJECT_INTERNAL_NAME = "java/lang/reflect/AccessibleObject";
	private static final String ANNOTATEDELEMENT_INTERNAL_NAME = "java/lang/reflect/AnnotatedElement";
	private static final String MEMBER_INTERNAL_NAME = "java/lang/reflect/Member";
	private static final String GENERICDECLARATION_INTERNAL_NAME = "java/lang/reflect/GenericDeclaration";
	private static final String CONSTRUCTOR_INTERNAL_NAME = "java/lang/reflect/Constructor";
	private static final String PACKAGE_INTERNAL_NAME = "java/lang/Package";
	private static final String MODULE_INTERNAL_NAME = "java/lang/Module";
	private static final String ANNOTATION_INTERNAL_NAME = "java/lang/annotation/Annotation";

	private static final Type CLASS_TYPE = Type.getType(Class.class);
	private static final Type FIELD_TYPE = Type.getType(Field.class);
	private static final Type METHOD_TYPE = Type.getType(Method.class);
	private static final Type CONSTRUCTOR_TYPE = Type.getType(Constructor.class);

	private final String className;
	private final int classIndex;

	public MethodInvocationLoggerClassVisitor(ClassVisitor cv, String className) {
		super(Opcodes.ASM7, cv);
		this.className = className;
		this.classIndex = InstrumentationData.getClassIndex(className);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (classIndex < 0) {
			return mv;
		}
		return new LoggerMethodVisitor(api, mv);
	}

	private class LoggerMethodVisitor extends MethodVisitor {

		public LoggerMethodVisitor(int api, MethodVisitor mv) {
			super(api, mv);
		}

		@Override
		public void visitCode() {
			super.visitCode();

			putMarkerInstructions(classIndex);
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
			if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC || opcode == Opcodes.GETFIELD
					|| opcode == Opcodes.PUTFIELD) {
				if (!owner.equals(className)) {
					//if we are setting or reading a field from another class
					putMarkerInstructions(owner);
				}
			}
			super.visitFieldInsn(opcode, owner, name, descriptor);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			switch (owner) {
				case CLASS_INTERNAL_NAME: {
					if (opcode == Opcodes.INVOKESTATIC) {
						if ("forName".equals(name)) {
							switch (descriptor) {
								case "(Ljava/lang/String;)Ljava/lang/Class;": {
									super.visitInsn(Opcodes.DUP);
									super.visitMethodInsn(Opcodes.INVOKESTATIC, INSTRUMENTATIONDATA_INTERNAL_NAME,
											"addUsedClass", "(Ljava/lang/String;)V", false);
									break;
								}
								case "(Ljava/lang/Module;Ljava/lang/String;)Ljava/lang/Class;": {
									super.visitInsn(Opcodes.DUP);
									super.visitMethodInsn(Opcodes.INVOKESTATIC, INSTRUMENTATIONDATA_INTERNAL_NAME,
											"addUsedClass", "(Ljava/lang/String;)V", false);
									break;
								}
								case "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;": {
									//str - bool - cl
									super.visitInsn(Opcodes.DUP_X2);
									//cl - str - bool - cl
									super.visitInsn(Opcodes.POP);
									//cl - str - bool 
									super.visitInsn(Opcodes.DUP_X2);
									//bool - cl - str - bool
									super.visitInsn(Opcodes.POP);
									//bool - cl - str
									super.visitInsn(Opcodes.DUP_X2);
									//stsr - bool - cl - str
									super.visitMethodInsn(Opcodes.INVOKESTATIC, INSTRUMENTATIONDATA_INTERNAL_NAME,
											"addUsedClass", "(Ljava/lang/String;)V", false);
									break;
								}
								default: {
									//unknown forname call
									break;
								}
							}
						}
					}
					break;
				}
				case CLASSLOADER_INTERNAL_NAME: {
					if (opcode == Opcodes.INVOKEVIRTUAL) {
						if ("loadClass".equals(name)) {
							switch (descriptor) {
								case "(Ljava/lang/String;)Ljava/lang/Class;": {
									super.visitInsn(Opcodes.DUP);
									super.visitMethodInsn(Opcodes.INVOKESTATIC, INSTRUMENTATIONDATA_INTERNAL_NAME,
											"addUsedClass", "(Ljava/lang/String;)V", false);
									break;
								}
								case "(Ljava/lang/String;Z)Ljava/lang/Class;": {
									super.visitInsn(Opcodes.SWAP);
									super.visitInsn(Opcodes.DUP);
									super.visitMethodInsn(Opcodes.INVOKESTATIC, INSTRUMENTATIONDATA_INTERNAL_NAME,
											"addUsedClass", "(Ljava/lang/String;)V", false);
									super.visitInsn(Opcodes.SWAP);
									break;
								}
								default: {
									//unknown loadclass call
									break;
								}
							}
						}
					}
					break;
				}
//				case FIELD_INTERNAL_NAME: {
//					break;
//				}
//				case METHOD_INTERNAL_NAME: {
//					break;
//				}
//				case CONSTRUCTOR_INTERNAL_NAME: {
//					break;
//				}
				default: {
					break;
				}
			}

			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

//			Type rtype = Type.getMethodType(descriptor).getReturnType();
//			if (CLASS_TYPE.equals(rtype)) {
//				//a method returned a class
//				//    depend on the returned class
//				super.visitInsn(Opcodes.DUP);
//				super.visitMethodInsn(Opcodes.INVOKESTATIC, INSTRUMENTATIONDATA_INTERNAL_NAME, "addUsedClass", "(Ljava/lang/Class;)V", false);
//			}
		}

		@Override
		public void visitLdcInsn(Object value) {
			super.visitLdcInsn(value);
			if (value instanceof Type) {
				handleLdcType((Type) value);
			}
		}

		private void handleLdcType(Type t) {
			if (t == null) {
				return;
			}
			int sort = t.getSort();
			switch (sort) {
				case Type.VOID:
				case Type.BOOLEAN:
				case Type.CHAR:
				case Type.BYTE:
				case Type.SHORT:
				case Type.INT:
				case Type.LONG:
				case Type.FLOAT:
				case Type.DOUBLE: {
					//primitive type, unhandled
					break;
				}
				case Type.ARRAY: {
					handleLdcType(t.getElementType());
					break;
				}
				case Type.OBJECT: {
					putMarkerInstructions(t.getInternalName());
					break;
				}
				case Type.METHOD: {
					//what is exactly a method type?
					//    whatever, better handle the return types and the argument types than be sorry
					handleLdcType(t.getReturnType());
					for (Type atype : t.getArgumentTypes()) {
						handleLdcType(atype);
					}
					break;
				}
				default: {
					//unknown type. unhandled
					break;
				}
			}
		}

		private void putMarkerInstructions(String classname) {
			putMarkerInstructions(InstrumentationData.getClassIndex(classname));
		}

		private void putMarkerInstructions(int classindex) {
//			mv.visitFieldInsn(Opcodes.GETSTATIC, INSTRUMENTATIONDATA_INTERNAL_NAME, InstrumentationData.MARKER_ARRAY_VARIABLE_NAME,
//					BOOLEAN_ARRAY_TYPE_DESCRIPTOR);
//			mv.visitLdcInsn(classindex);
//			mv.visitLdcInsn(1);
//			mv.visitInsn(Opcodes.BASTORE);

			mv.visitLdcInsn(classindex);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, INSTRUMENTATIONDATA_INTERNAL_NAME, "setUsedIndex", "(I)V",
					false);
		}
	}

}