# saker.java.testing

![Build status](https://img.shields.io/azure-devops/build/sakerbuild/5e05673c-f79b-4b0e-9866-bafebbde4851/16/master)

The project implements incremental Java testing support for the [saker.build system](https://saker.build). The implementation dynamically instruments the tested classes to record the dependencies. It can track the accessed classes and files on a test-case basis.

See the [documentation](https://saker.build/saker.java.testing/doc/) for more information.

## Build instructions

The project uses the [saker.build system](https://saker.build) for building. It requires both JDK8 and JDK9 to be installed. Use the following command to build the project:

```
java -jar path/to/saker.build.jar -bd build -EUsaker.java.jre.install.locations=path/to/jdk8;path/to/jdk9 compile saker.build
```

## License

The source code for the project is licensed under *GNU General Public License v3.0 only*.

Short identifier: [`GPL-3.0-only`](https://spdx.org/licenses/GPL-3.0-only.html).

Official releases of the project (and parts of it) may be licensed under different terms. See the particular releases for more information.
