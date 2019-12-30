package saker.java.testing.agent;

import saker.build.thirdparty.org.objectweb.asm.ClassVisitor;
import saker.build.thirdparty.org.objectweb.asm.MethodVisitor;
import saker.build.thirdparty.org.objectweb.asm.Opcodes;

class ConstructorDelegateClassVisitor extends ClassVisitor {

	private String methodDescriptor;
	private String targetClassInternalName;
	private String targetMethodName;
	private int[] argumentOpcodes;

	public ConstructorDelegateClassVisitor(ClassVisitor classVisitor, String methodSignature,
			String targetClassDescriptor, String targetMethodName, int[] argumentOpcodes) {
		super(Opcodes.ASM7, classVisitor);
		this.methodDescriptor = methodSignature;
		this.targetClassInternalName = targetClassDescriptor;
		this.targetMethodName = targetMethodName;
		this.argumentOpcodes = argumentOpcodes;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
			String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		if ("<init>".equals(name)) {
			if (methodDescriptor.equals(descriptor)) {
				return new CallReplacerMethodVisitor(api, mv);
			}
		}
		return mv;
	}

	private class CallReplacerMethodVisitor extends MethodVisitor {
		public CallReplacerMethodVisitor(int api, MethodVisitor methodVisitor) {
			super(api, methodVisitor);
		}

		@Override
		public void visitCode() {
			super.visitCode();
			for (int i = 0; i < argumentOpcodes.length; i++) {
				//load the parameters
				mv.visitIntInsn(argumentOpcodes[i], i + 1);
			}
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, targetClassInternalName, targetMethodName, methodDescriptor,
					false);
		}

	}
}
