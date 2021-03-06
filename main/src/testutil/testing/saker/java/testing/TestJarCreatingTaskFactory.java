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
package testing.saker.java.testing;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.attribute.FileTime;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import saker.build.file.ByteArraySakerFile;
import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;
import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.file.provider.SakerPathFiles;
import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.ParameterizableTask;
import saker.build.task.Task;
import saker.build.task.TaskContext;
import saker.build.task.TaskFactory;
import saker.build.task.utils.annot.SakerInput;
import saker.build.task.utils.dependencies.WildcardFileCollectionStrategy;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;

public class TestJarCreatingTaskFactory implements TaskFactory<SakerPath>, Externalizable {
	private static final long serialVersionUID = 1L;

	private static final FileTime DEFAULT_FILE_TIME = FileTime.fromMillis(0);

	/**
	 * For {@link Externalizable}.
	 */
	public TestJarCreatingTaskFactory() {
	}

	@Override
	public Task<? extends SakerPath> createTask(ExecutionContext executioncontext) {
		return new ParameterizableTask<SakerPath>() {

			@SakerInput("Directory")
			public SakerPath directory;

			@SakerInput("Resources")
			public WildcardPath resources;

			@Override
			public SakerPath run(TaskContext taskcontext) throws Exception {
				SakerPath basedir = directory;
				if (basedir == null) {
					basedir = taskcontext.getTaskWorkingDirectoryPath();
				}
				NavigableMap<SakerPath, SakerFile> files = taskcontext.getTaskUtilities()
						.collectFilesReportInputFileAndAdditionDependency(null,
								WildcardFileCollectionStrategy.create(directory, resources));
				byte[] jarbytes;
				try (UnsyncByteArrayOutputStream baos = new UnsyncByteArrayOutputStream()) {
					try (JarOutputStream jaros = new JarOutputStream(baos)) {
						for (Entry<SakerPath, SakerFile> entry : files.entrySet()) {
							if (entry.getValue() instanceof SakerDirectory) {
								continue;
							}
							JarEntry je = new JarEntry(basedir.relativize(entry.getKey()).toString());
							jaros.putNextEntry(je);
							je.setLastModifiedTime(DEFAULT_FILE_TIME);
							entry.getValue().writeTo(jaros);
							jaros.closeEntry();
						}
					}
					jarbytes = baos.toByteArray();
				}

				SakerFile outfile = new ByteArraySakerFile("output.jar", jarbytes);
				taskcontext.getTaskUtilities()
						.resolveDirectoryAtPathCreate(SakerPathFiles.requireBuildDirectory(taskcontext),
								SakerPath.valueOf("test.jar.create"))
						.add(outfile);
				outfile.synchronize();
				taskcontext.getTaskUtilities().reportOutputFileDependency(null, outfile);
				return outfile.getSakerPath();
			}
		};
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}

	@Override
	public int hashCode() {
		return getClass().getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return ObjectUtils.isSameClass(this, obj);
	}
}
