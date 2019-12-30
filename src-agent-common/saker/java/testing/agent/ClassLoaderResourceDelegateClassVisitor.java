package saker.java.testing.agent;

import saker.build.thirdparty.org.objectweb.asm.ClassVisitor;
import saker.build.thirdparty.org.objectweb.asm.MethodVisitor;
import saker.build.thirdparty.org.objectweb.asm.Opcodes;
import saker.build.thirdparty.org.objectweb.asm.Type;
import saker.java.testing.bootstrapagent.TestFileRequestor;

class ClassLoaderResourceDelegateClassVisitor extends ClassVisitor {

	public ClassLoaderResourceDelegateClassVisitor(ClassVisitor classVisitor) {
		super(Opcodes.ASM7, classVisitor);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
			String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new CallReplacerMethodVisitor(api, mv);
	}

	private static class CallReplacerMethodVisitor extends MethodVisitor {

		private static final String TESTFILEREQUESTOR_INTERNAL_NAME = Type.getInternalName(TestFileRequestor.class);

		public CallReplacerMethodVisitor(int api, MethodVisitor methodVisitor) {
			super(api, methodVisitor);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			if (opcode == Opcodes.INVOKEVIRTUAL) {
				if ("java/lang/ClassLoader".equals(owner)) {
					//resources introduced in JDK9
					if ("getResource".equals(name) || "getResources".equals(name) || "getResourceAsStream".equals(name)
							|| "resources".equals(name)) {
						mv.visitInsn(Opcodes.DUP);
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, TESTFILEREQUESTOR_INTERNAL_NAME,
								"requestClassLoaderResource", "(Ljava/lang/String;)V", false);
					}
				} else if ("java/lang/Module".equals(owner)) {
					if ("getResourceAsStream".equals(name)) {
						mv.visitInsn(Opcodes.DUP);
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, TESTFILEREQUESTOR_INTERNAL_NAME,
								"requestClassLoaderResourceFromModule", "(Ljava/lang/String;)V", false);
					}
				} else if ("java/lang/Class".equals(owner)) {
					if ("getResource".equals(name) || "getResourceAsStream".equals(name)) {
						mv.visitInsn(Opcodes.DUP2);
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, TESTFILEREQUESTOR_INTERNAL_NAME,
								"requestClassLoaderResourceFromClass", "(Ljava/lang/Class;Ljava/lang/String;)V", false);
					}
				}
			}
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}
}
