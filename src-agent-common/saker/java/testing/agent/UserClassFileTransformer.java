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

import java.lang.instrument.ClassFileTransformer;
import java.nio.file.spi.FileSystemProvider;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

import saker.java.testing.agent.thirdparty.org.objectweb.asm.ClassReader;
import saker.java.testing.agent.thirdparty.org.objectweb.asm.ClassVisitor;
import saker.java.testing.agent.thirdparty.org.objectweb.asm.ClassWriter;
import saker.java.testing.agent.thirdparty.org.objectweb.asm.Opcodes;
import saker.java.testing.agent.thirdparty.org.objectweb.asm.Type;
import saker.java.testing.bootstrapagent.NioFileSystemProviderSakerProxy;

class UserClassFileTransformer implements ClassFileTransformer {
	public static final int ASM_API = Opcodes.ASM9;
	private static final String FILESYSTEMPROVIDER_INTERNAL_NAME = Type.getInternalName(FileSystemProvider.class);
	private static final String FILESYSTEMPROVIDER_DESCRIPTOR = "L" + FILESYSTEMPROVIDER_INTERNAL_NAME + ";";
	private static final String FILESYSTEMPROVIDER_PROXY_INTERNAL_NAME = Type
			.getInternalName(NioFileSystemProviderSakerProxy.class);

	private static final String IOFILESYSTEM_INTERNAL_NAME = "java/io/FileSystem";
	private static final String IOFILESYSTEM_ARGUMENT_INTERNAL_NAME = TestingInstrumentationAgent.JAVA_IO_FILESYSTEM_PROXY_ARGUMENT_INTERNAL_NAME;
	private static final String IOFILESYSTEM_ARGUMENT_DESCRIPTOR = "L" + IOFILESYSTEM_ARGUMENT_INTERNAL_NAME + ";";
	private static final String IOFILESYSTEM_PROXY_INTERNAL_NAME = TestingInstrumentationAgent.JAVA_IO_FILESYSTEM_PROXY_INTERNAL_NAME;

	private static final Set<ClassLoader> systemClassLoaders = new HashSet<>();
	{
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		while (cl != null) {
			systemClassLoaders.add(cl);
			cl = cl.getParent();
		}
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		if (className == null) {
			return null;
		}
		try {
			//do not transform bootstrap classes 
			//do not transform classes on the classpath e.g. saker classes
			boolean loginvocations = loader != null && !systemClassLoaders.contains(loader);
			boolean preventexit = loader != null && !systemClassLoaders.contains(loader);
			ClassReader cr = new ClassReader(classfileBuffer);
			ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
			ClassVisitor lastcv = cw;
			if (loginvocations) {
				lastcv = new MethodInvocationLoggerClassVisitor(lastcv, className);
			}
			if (preventexit) {
				lastcv = new ExitCallDisablerClassVisitor(lastcv);
			}
			if (!FILESYSTEMPROVIDER_PROXY_INTERNAL_NAME.equals(className)) {
				lastcv = new ClassMethodDelegateClassVisitor(lastcv, FILESYSTEMPROVIDER_DESCRIPTOR,
						FILESYSTEMPROVIDER_INTERNAL_NAME, FILESYSTEMPROVIDER_PROXY_INTERNAL_NAME);
			}
			lastcv = new ClassLoaderResourceDelegateClassVisitor(lastcv);
//			lastcv = new ClassMethodDelegateClassVisitor(lastcv, NIOFILESYSTEM_SIGNATURE, NIOFILESYSTEM_DESCRIPTOR, NIOFILESYSTEM_PROXY_DESCRIPTOR);
			if (!IOFILESYSTEM_PROXY_INTERNAL_NAME.equals(className)) {
				lastcv = new ClassMethodDelegateClassVisitor(lastcv, IOFILESYSTEM_ARGUMENT_DESCRIPTOR,
						IOFILESYSTEM_INTERNAL_NAME, IOFILESYSTEM_PROXY_INTERNAL_NAME);
			}
			if ("java/io/FileOutputStream".equals(className)) {
				lastcv = new ConstructorDelegateClassVisitor(lastcv, "(Ljava/io/File;Z)V",
						IOFILESYSTEM_PROXY_INTERNAL_NAME, "newFileOutputStream",
						new int[] { Opcodes.ALOAD, Opcodes.ILOAD });
			} else if ("java/io/FileInputStream".equals(className)) {
				lastcv = new ConstructorDelegateClassVisitor(lastcv, "(Ljava/io/File;)V",
						IOFILESYSTEM_PROXY_INTERNAL_NAME, "newFileInputStream", new int[] { Opcodes.ALOAD });
			} else if ("java/util/zip/ZipFile".equals(className)) {
				lastcv = new ConstructorDelegateClassVisitor(lastcv, "(Ljava/io/File;ILjava/nio/charset/Charset;)V",
						IOFILESYSTEM_PROXY_INTERNAL_NAME, "newZipFile",
						new int[] { Opcodes.ALOAD, Opcodes.ILOAD, Opcodes.ALOAD });
			} else if ("java/io/RandomAccessFile".equals(className)) {
				lastcv = new ConstructorDelegateClassVisitor(lastcv, "(Ljava/io/File;Ljava/lang/String;)V",
						IOFILESYSTEM_PROXY_INTERNAL_NAME, "newRandomAccessFile",
						new int[] { Opcodes.ALOAD, Opcodes.ALOAD });
			}
			cr.accept(lastcv, 0);
			return cw.toByteArray();
		} catch (Throwable e) {
			System.err.println("Failed to transform class bytes for " + className);
			System.err.println("Exiting...");
//			System.err.println("Bytes: " + Arrays.toString(classfileBuffer));
			e.printStackTrace();
			//exit as the class transformation failure is considered to be fatal. this also notifies the
			//testing task that the testing cannot be started.
			System.exit(-1);
		}
		return null;
	}
}
