# java-binrepo-proxy
Policy enforcing binary repository (i.e. Maven) (reverse) proxy

The proxy is to be placed between the developer (or build server) and the binary repository (i.e. the Central or an internal repository) and will enforce policies before serving the requested artifacts.

Artifacts are only to be retunred after its (OpenPGP) digital signature has been verified.

The purpose is to solve the lack of proper signature verification support in Maven for both project dependencies and plugins.

## Usage

Start the proxy
```
jbinrepoproxy-standalone trust/keysmap.properties 8888 repo.maven.apache.org 80
```

Configure your ~/.m2/settings.xml to use the proxy instead of Central
```
<settings...
  <profiles>
    <repositories>
      <repository>
        <id>central</id>
        <url>http://localhost:8888/maven2</url>
      </repository>
    </repositories>
    <pluginRepositories>
      <pluginRepository>
        <id>central</id>
        <url>http://localhost:8888/maven2</url>
      </pluginRepository>
    </pluginRepositories>
  </profiles>
</settings>
```

Trusted keys (or digests) for different artifacts are configured in the keys mapping file. See trust/keysmap.properties.

Tip: To test building of your project with an empty repository in order to see that it fetches and verifies the artifacts an empty folder can be specified as your local Maven repo:
```
mvn -Dmaven.repo.local=$HOME/.my/other/repository clean install
```

## Known Limitations

- No HTTPS/TLS support yet
