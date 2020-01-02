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

import saker.build.thirdparty.org.objectweb.asm.ClassVisitor;
import saker.build.thirdparty.org.objectweb.asm.MethodVisitor;
import saker.build.thirdparty.org.objectweb.asm.Opcodes;

class ClassMethodDelegateClassVisitor extends ClassVisitor {
	private String forwardParameterDescriptor;
	private String subjectInternalName;
	private String targetInternalName;

	public ClassMethodDelegateClassVisitor(ClassVisitor classVisitor, String forwardParameterDescriptor,
			String subjectInternalName, String targetInternalName) {
		super(Opcodes.ASM7, classVisitor);
		this.forwardParameterDescriptor = forwardParameterDescriptor;
		this.subjectInternalName = subjectInternalName;
		this.targetInternalName = targetInternalName;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
			String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new CallReplacerMethodVisitor(api, mv);
	}

	private class CallReplacerMethodVisitor extends MethodVisitor {

		public CallReplacerMethodVisitor(int api, MethodVisitor methodVisitor) {
			super(api, methodVisitor);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			if (opcode == Opcodes.INVOKEVIRTUAL && !isIgnoredObjectMethod(name, descriptor)) {
				if (subjectInternalName.equals(owner)) {
					String modifieddescriptor = "(" + forwardParameterDescriptor + descriptor.substring(1);
					mv.visitMethodInsn(Opcodes.INVOKESTATIC, targetInternalName, name, modifieddescriptor, false);
					return;
				}
			}
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}

	private static boolean isIgnoredObjectMethod(String name, String descriptor) {
		switch (name) {
			case "hashCode": {
				return "()I".equals(descriptor);
			}
			case "toString": {
				return "()Ljava/lang/String;".equals(descriptor);
			}
			case "getClass": {
				return "()Ljava/lang/Class;".equals(descriptor);
			}
			case "equals": {
				return "(Ljava/lang/Object;)Z".equals(descriptor);
			}
			case "clone": {
				return "()Ljava/lang/Object;".equals(descriptor);
			}
			case "notify":
			case "notifyAll": {
				return "()V".equals(descriptor);
			}
			case "wait": {
				return "()V".equals(descriptor) || "(J)V".equals(descriptor) || "(JI)V".equals(descriptor);
			}
			case "finalize": {
				return "()V".equals(descriptor);
			}
		}
		return false;
	}
}
