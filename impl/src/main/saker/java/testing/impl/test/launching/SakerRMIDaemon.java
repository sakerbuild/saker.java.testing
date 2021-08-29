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
package saker.java.testing.impl.test.launching;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

import saker.build.thirdparty.saker.rmi.connection.RMIConnection;
import saker.build.thirdparty.saker.rmi.connection.RMIOptions;
import saker.build.thirdparty.saker.rmi.connection.RMIServer;
import saker.build.thirdparty.saker.rmi.connection.RMITransferProperties;
import saker.build.thirdparty.saker.util.io.ByteSource;
import saker.build.thirdparty.saker.util.io.ReadWriteBufferOutputStream;
import saker.build.thirdparty.saker.util.io.StreamUtils;
import saker.build.thirdparty.saker.util.thread.ThreadUtils;
import saker.java.testing.impl.test.TestInvokerSupport;

public class SakerRMIDaemon {
	//TODO duplicated with saker.java.compiler
	public static final String CONTEXT_VARIABLE_BASE_CLASSLOADER = "daemon.base.classloader";

	private int port = 0;
	private ClassLoader baseClassLoader;
	private RMITransferProperties transferProperties;

	public SakerRMIDaemon() {
	}

	public void run() throws IOException {
		//we need to change the streams else the writing and reading might block forever 
		//    if the output is not read on the other side
		PrintStream prevout = System.out;
		PrintStream preverr = System.err;

		try (ReadWriteBufferOutputStream stdout = new ReadWriteBufferOutputStream();
				ReadWriteBufferOutputStream stderr = new ReadWriteBufferOutputStream()) {
			try (PrintStream stdoutps = new PrintStream(stdout);
					PrintStream stderrps = new PrintStream(stderr)) {
				System.setOut(stdoutps);
				System.setErr(stderrps);
				System.setIn(StreamUtils.nullInputStream());

				try {
					runServer(prevout, prevout, preverr, stdout, stderr);
				} catch (Throwable e) {
					try {
						System.setOut(prevout);
						System.setErr(preverr);
					} catch (Throwable e2) {
						try {
							e.addSuppressed(e2);
						} catch (Throwable e3) {
							// failed to suppress, don't try, ignore. some serious error has happened
						}
					}
					throw e;
				}
			}
		}
	}

	private void runServer(PrintStream portprintout, PrintStream prevout, PrintStream preverr,
			ReadWriteBufferOutputStream stdoutin, ReadWriteBufferOutputStream stderrin) throws IOException {
		try (RMIServer server = new RMIServer(null, port, null) {
			@Override
			protected RMIOptions getRMIOptionsForAcceptedConnection(Socket acceptedsocket, int protocolversion) {
				return new RMIOptions().transferProperties(transferProperties).classLoader(baseClassLoader);
			}

			@Override
			protected void setupConnection(Socket acceptedsocket, RMIConnection connection)
					throws IOException, RuntimeException {
				super.setupConnection(acceptedsocket, connection);
				connection.putContextVariable(CONTEXT_VARIABLE_BASE_CLASSLOADER, baseClassLoader);
			}
		}) {
			TestInvokerSupport.setRMIServerConnectionTimeoutToPropertyOrDefault(server);
			
			portprintout.println(server.getPort());
			portprintout.flush();
			portprintout = null;

			ThreadUtils.startDaemonThread("Stdout printer", () -> {
				try {
					StreamUtils.copyStream(ByteSource.toInputStream(stdoutin), prevout);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			ThreadUtils.startDaemonThread("Stderr printer", () -> {
				try {
					StreamUtils.copyStream(ByteSource.toInputStream(stderrin), preverr);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			server.acceptConnections();
		}
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setBaseClassLoader(ClassLoader baseClassLoader) {
		this.baseClassLoader = baseClassLoader;
	}

	public void setTransferProperties(RMITransferProperties transferProperties) {
		this.transferProperties = transferProperties;
	}
}
