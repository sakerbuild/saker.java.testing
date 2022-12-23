package saker.java.testing.impl.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import saker.build.exception.MissingConfigurationException;
import saker.build.file.content.FileAttributesContentDescriptor;
import saker.build.file.provider.FileEntry;
import saker.build.file.provider.LocalFileProvider;
import saker.build.launching.Main;
import saker.build.meta.PropertyNames;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.runtime.execution.SakerLog;
import saker.build.runtime.repository.RepositoryEnvironment;
import saker.build.thirdparty.saker.util.DateUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.StringUtils;
import saker.build.util.cache.CacheKey;
import saker.build.util.config.ReferencePolicy;
import saker.java.testing.impl.test.launching.RemoteJavaRMIProcess;
import saker.java.testing.impl.test.launching.TestInvokerDaemon;
import saker.nest.bundle.BundleIdentifier;
import saker.nest.bundle.JarNestRepositoryBundle;
import saker.nest.bundle.NestBundleClassLoader;
import saker.nest.bundle.NestRepositoryBundle;
import saker.nest.exc.BundleLoadingFailedException;
import saker.nest.utils.NestUtils;
import testing.saker.java.testing.TestFlag;

public class RemoteJavaTesterCacheKey implements CacheKey<RemoteTester, RemoteJavaRMIProcess> {
	private static final Class<?> TEST_INVOKER_DAEMON_MAIN_CLASS = TestInvokerDaemon.class;
	private static final String TEST_INVOKER_DAEMON_MAIN_CLASS_NAME = TEST_INVOKER_DAEMON_MAIN_CLASS.getName();
	private static final String TEST_INVOKER_DAEMON_ENCLOSING_BUNDLE_IDENTIFIER_STRING = NestUtils
			.getClassBundleIdentifier(TEST_INVOKER_DAEMON_MAIN_CLASS).toString();

	public static final boolean COLLECT_RMI_STATS = saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_015
			&& (System.getProperty(PropertyNames.PROPERTY_COLLECT_RMI_STATISTICS) != null || TestFlag.ENABLED);

	private transient final SakerEnvironment environment;

	private final Path javaExe;
	private final Path workingDirectory;
	private final int identifier;
	private final Map<String, String> executionUserParameters;
	private final List<String> processJVMArguments;
	private final int javaMajor;

	public RemoteJavaTesterCacheKey(SakerEnvironment environment, Path javaExe, Path workingdirectory, int identifier,
			List<String> processjvmarguments, int javaMajor) {
		this.environment = environment;
		this.javaExe = javaExe;
		this.workingDirectory = workingdirectory;
		this.identifier = identifier;
		this.processJVMArguments = processjvmarguments;
		this.javaMajor = javaMajor;

		NestBundleClassLoader cl = (NestBundleClassLoader) IncrementalTestingHandler.class.getClassLoader();
		Map<String, String> userparams = cl.getBundleStorageConfiguration().getBundleLookup()
				.getLocalConfigurationUserParameters(null);
		this.executionUserParameters = userparams;
	}

	@Override
	public void close(RemoteTester data, RemoteJavaRMIProcess resource) throws Exception {
		resource.close();
	}

	@Override
	public RemoteJavaRMIProcess allocate() throws Exception {
		Path testingAgentJar = getAgentJarPath(environment, javaMajor);
		Path bootstrapAgentJar = getBootstrapAgentJarPath(environment, javaMajor);
		String agentpath = getShortPathedAgentPath(testingAgentJar);
		String bootstrapagentpath = getShortPathedAgentPath(bootstrapAgentJar);

		ClassLoader classloader = IncrementalTestingHandler.class.getClassLoader();
		NestBundleClassLoader nestbundlecl = (NestBundleClassLoader) classloader;

		RepositoryEnvironment repoenv = nestbundlecl.getRepository().getRepositoryEnvironment();
		List<String> commands = new ArrayList<>();
		commands.add(javaExe.toString());
		if (!ObjectUtils.isNullOrEmpty(processJVMArguments)) {
			for (Iterator<String> it = processJVMArguments.iterator(); it.hasNext();) {
				String arg = it.next();
				if (javaMajor < 9) {
					if (arg.startsWith("--illegal-access=")) {
						continue;
					}
					if (arg.startsWith("-Xlog:")) {
						//only supported since Java 9
						continue;
					}
					if ("--add-reads".equals(arg) || "--add-exports".equals(arg) || "--add-opens".equals(arg)) {
						//these arguments are not available on java 8 and below
						if (!it.hasNext()) {
							//missing argument? just ignore and break the loop as there are no more arguments
							break;
						}
						//skip its argument as well.
						it.next();
						continue;
					}
				}
				if (javaMajor < 11) {
					if (arg.equals("--enable-preview")) {
						continue;
					}
				}
				commands.add(arg);
			}
		}

		Path sakerjarpath = environment.getEnvironmentJarPath();
		if (sakerjarpath == null) {
			throw new MissingConfigurationException("saker.build JAR path is not available.");
		}
		commands.add("-javaagent:" + agentpath + "=" + bootstrapagentpath);
		if (COLLECT_RMI_STATS) {
			commands.add("-D" + PropertyNames.PROPERTY_COLLECT_RMI_STATISTICS + "=true");
		}
		ObjectUtils.addAll(commands,
				"-D" + PropertyNames.PROPERTY_SAKER_REFERENCE_POLICY + "="
						+ ReferencePolicy.ReferencePolicyCreator.WEAK,

				"-XX:MaxHeapFreeRatio=40", "-XX:MinHeapFreeRatio=15", "-XX:-OmitStackTraceInFastThrow",

				"-cp", sakerjarpath.toAbsolutePath().normalize().toString(), Main.class.getName(),

				"action", "-storage-dir", repoenv.getEnvironmentStorageDirectory().toString(), "-direct-repo",
				repoenv.getRepositoryClassPathLoadDirectory().toString(), "main");

		for (Entry<String, String> entry : executionUserParameters.entrySet()) {
			commands.add("-U" + entry.getKey().replace("=", "\\=") + "=" + entry.getValue());
		}
		commands.add("-class");
		commands.add(TEST_INVOKER_DAEMON_MAIN_CLASS_NAME);
		commands.add("-bundle");
		commands.add(TEST_INVOKER_DAEMON_ENCLOSING_BUNDLE_IDENTIFIER_STRING);

		LocalFileProvider.getInstance().createDirectories(workingDirectory);
		return new RemoteJavaRMIProcess(commands, workingDirectory.toString(), classloader,
				TestInvokerSupport.getTestInvokerRMITransferProperties(), environment.getEnvironmentThreadGroup());
	}

	private static Path getAgentJarPath(SakerEnvironment env, int javamajor) throws IOException {
		try {
			NestRepositoryBundle agentbundle = IncrementalTestingHandler
					.getBundleForJavaMajor(BundleIdentifier.valueOf("saker.java.testing-agent"), javamajor);
			if (agentbundle instanceof JarNestRepositoryBundle) {
				return ((JarNestRepositoryBundle) agentbundle).getJarPath();
			}
			throw new IOException("Unrecognized agent JAR type: " + ObjectUtils.classNameOf(agentbundle));
		} catch (BundleLoadingFailedException e) {
			throw new IOException("Agent Jar not found.", e);
		}
	}

	private static Path getBootstrapAgentJarPath(SakerEnvironment env, int javamajor) throws IOException {
		try {
			NestRepositoryBundle agentbundle = IncrementalTestingHandler
					.getBundleForJavaMajor(BundleIdentifier.valueOf("saker.java.testing-bootstrapagent"), javamajor);
			if (agentbundle instanceof JarNestRepositoryBundle) {
				return ((JarNestRepositoryBundle) agentbundle).getJarPath();
			}
			throw new IOException("Unrecognized bootstrap agent JAR type: " + ObjectUtils.classNameOf(agentbundle));
		} catch (BundleLoadingFailedException e) {
			throw new IOException("Agent Bootstrap Jar not found.", e);
		}
	}

	private static String getShortPathedAgentPath(Path path) throws NoSuchAlgorithmException, IOException {
		//if the jar paths are too long on windows (exceeds MAX_PATH), then the process will not find them.
		//    MAX_PATH is defined as 260, but have some threshold in order to function properly because
		//    the documentation says some functions have less than 260 maximum character count. E.g.
		//        When using an API to create a directory, the specified path cannot be so long that you 
		//        cannot append an 8.3 file name (that is, the directory name cannot exceed MAX_PATH minus 12).

		String pathstr = path.toString();
		if (pathstr.length() <= 240) {
			return pathstr;
		}
		String tempdir = System.getProperty("java.io.tmpdir");
		if (tempdir == null) {
			//no temp directory, warn the user, and return the long path.
			//any failure will be deferred
			SakerLog.warning()
					.println("Temp directory not available. Failed to extract JAR with long path: " + pathstr);
			return pathstr;
		}
		String pathmd5 = StringUtils.toHexString(
				MessageDigest.getInstance("MD5").digest(path.getParent().toString().getBytes(StandardCharsets.UTF_8)));
		Path parentdir = Paths.get(tempdir, "saker", "shorten", pathmd5);
		Path npath = parentdir.resolve(path.getFileName());
		LocalFileProvider localfiles = LocalFileProvider.getInstance();
		FileEntry currentattrs;
		try {
			currentattrs = localfiles.getFileAttributes(npath);
		} catch (IOException e) {
			currentattrs = null;
		}
		if (currentattrs == null
				|| FileAttributesContentDescriptor.isChanged(currentattrs, localfiles.getFileAttributes(path))) {
			try {
				localfiles.createDirectories(parentdir);
				Files.copy(path, npath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
			} catch (IOException e) {
				SakerLog.warning().println("Failed to extract JAR with long path to temp directory: " + pathstr + " -> "
						+ tempdir + " (" + e + ")");
				return pathstr;
			}
		}
		return npath.toString();
	}

	@Override
	public RemoteTester generate(RemoteJavaRMIProcess resource) throws Exception {
		return new RemoteTester(resource);
	}

	@Override
	public boolean validate(RemoteTester data, RemoteJavaRMIProcess resource) {
		boolean valid = resource.isValid();
		if (!valid) {
			resource.close();
		}
		return valid;
	}

	@Override
	public long getExpiry() {
		if (TestFlag.ENABLED) {
			//if testing, expire ASAP to free resources
			return 5 * DateUtils.MS_PER_SECOND;
		}
		return 5 * DateUtils.MS_PER_MINUTE;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((executionUserParameters == null) ? 0 : executionUserParameters.hashCode());
		result = prime * result + identifier;
		result = prime * result + ((javaExe == null) ? 0 : javaExe.hashCode());
		result = prime * result + javaMajor;
		result = prime * result + ((processJVMArguments == null) ? 0 : processJVMArguments.hashCode());
		result = prime * result + ((workingDirectory == null) ? 0 : workingDirectory.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RemoteJavaTesterCacheKey other = (RemoteJavaTesterCacheKey) obj;
		if (executionUserParameters == null) {
			if (other.executionUserParameters != null)
				return false;
		} else if (!executionUserParameters.equals(other.executionUserParameters))
			return false;
		if (identifier != other.identifier)
			return false;
		if (javaExe == null) {
			if (other.javaExe != null)
				return false;
		} else if (!javaExe.equals(other.javaExe))
			return false;
		if (javaMajor != other.javaMajor)
			return false;
		if (processJVMArguments == null) {
			if (other.processJVMArguments != null)
				return false;
		} else if (!processJVMArguments.equals(other.processJVMArguments))
			return false;
		if (workingDirectory == null) {
			if (other.workingDirectory != null)
				return false;
		} else if (!workingDirectory.equals(other.workingDirectory))
			return false;
		return true;
	}

}