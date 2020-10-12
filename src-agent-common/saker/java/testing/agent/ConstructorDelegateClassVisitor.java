/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package saker.java.testing.agent;

import saker.java.testing.agent.thirdparty.org.objectweb.asm.ClassVisitor;
import saker.java.testing.agent.thirdparty.org.objectweb.asm.MethodVisitor;
import saker.java.testing.agent.thirdparty.org.objectweb.asm.Opcodes;

class ConstructorDelegateClassVisitor extends ClassVisitor {

	private String methodDescriptor;
	private String targetClassInternalName;
	private String targetMethodName;
	private int[] argumentOpcodes;

	public ConstructorDelegateClassVisitor(ClassVisitor classVisitor, String methodSignature,
			String targetClassDescriptor, String targetMethodName, int[] argumentOpcodes) {
		super(UserClassFileTransformer.ASM_API, classVisitor);
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
