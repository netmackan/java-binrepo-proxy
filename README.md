# java-binrepo-proxy
Policy enforcing binary repository (i.e. Maven) (reverse) proxy

The proxy is to be placed between the developer (or build server) and the binary repository (i.e. the Central or an internal repository) and will enforce policies before serving the requested artifacts.

Artifacts (including Maven plugins) are only to be returned after its (OpenPGP) digital signature has been verified.

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

Tip: To test building of your project with an empty repository in order to see that it fetches and verifies the artifacts an empty folder can be specified as your local Maven repo:
```
mvn -Dmaven.repo.local=$HOME/.my/other/repository clean install
```

## Trust Configuration
Trusted keys (or digests) for different artifacts are configured in the keys mapping file. See trust/keysmap.properties.

The idea being that the public keys are obtained from each project of interest and the keys mapping file is updated with entries mapping from a key fingerprint to the path it is trusted for (i.e. an Apache Commons Lang release key could be allowed to verify artifacts with a URI starting with "/maven2/org/apache/commons/commons-lang/".

To simplify the mapping as projects can have many different signing keys it is also possible to specify that all the public keys in a KEYS file should be trusted for a given path. This is useful for all the Apache projects as those publishes a KEYS file for each of their projects.

Further as some projects even on the Central are missing signatures it is possible to specify a complete URI of an Artifact and the SHA-256 sum of it. In that case the digest would be verified and the artifact accepted even though there is no signature. This exception is required as some old dependencies of Maven (!) are missing signatures.

### Example:
```
# Trust Maven keys for Maven and the Apache parent POMs
TRUSTFILE./maven2/org/apache/maven/=apache-maven-KEYS.asc
TRUSTFILE./maven2/org/apache/apache/=apache-maven-KEYS.asc

# maven-toolchain was signed by a different key, manually specify that one
FINGERPRINT.2E795B761F5B3B39CA46E48C66F196CA727DE1C5=/maven2/org/apache/maven/maven-toolchain/1.0/

# Old Maven dependencies without signatures, hardcode the digests
TRUSTEDDIGEST./maven2/org/apache/maven/maven-parent/5/maven-parent-5.pom=5d7c2a229173155823c45380332f221bf0d27e52c9db76e9217940306765bd50

# Trust Commons Logging keys for Log4j
TRUSTFILE./maven2/log4j/log4j/=apache-logging-KEYS.asc
```

## Known Limitations

- No HTTPS/TLS support yet
- Format of the keysmap will likely change
- No automagic reload of the keysmap. It is read on startup, any changes in it and you will have to restart the proxy
- No option yet to configure the location of the key cache
- No option yet to enable/disable fetching keys from a key server
- No option yet to choose key server(s)
- Logging needs improvement
- Information on why the artifact was refused not visible on the client side (in the future: the status string should say "Invalid signature for artifact URI", "No signature for artifact [URI] with digest 0x[DIGEST]" etc

## Credits
- Some code is from the [Verify PGP signatures plugin](https://github.com/s4u/pgpverify-maven-plugin)
