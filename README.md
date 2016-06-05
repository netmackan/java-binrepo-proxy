# java-binrepo-proxy
Policy enforcing binary repository (i.e. Maven) (reverse) proxy

The proxy is to be placed between the developer (or build server) and the binary repository (i.e. the Central or an internal repository) and will enforce policies before serving the requested artifacts.

Artifacts (including Maven plugins) are only to be returned after its (OpenPGP) digital signature has been verified.

The purpose is to solve the lack of proper signature verification support in Maven for both project dependencies and plugins.

## Usage

Configure the proxy in conf/jbinrepoproxy-standalone.properties:
```
standalone.host=127.0.0.1
standalone.port=8888

target.scheme=https
target.host=repo.maven.apache.org
target.port=443

trust.map.file=../trust/keysmap.properties

cache.keys.folder=../cache/keys
cache.keys.server=hkps://hkps.pool.sks-keyservers.net
```


Start the proxy
```
./bin/jbinrepoproxy-standalone.sh conf/jbinrepoproxy-standalone.properties
```

Configure your ~/.m2/settings.xml to use the proxy as mirror of EVERYTHING 
(replace localhost with the server's host name in case the proxy is not running
 on the same host):
```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                    http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <mirrors>
        <mirror>
            <id>verified-central</id>
            <name>Central through JBinRepoProxy</name>
            <mirrorOf>*</mirrorOf>
            <url>http://localhost:8888/maven2</url>
        </mirror>
    </mirrors>

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

- Format of the keysmap will likely change
- No option yet to enable/disable fetching keys from a key server
- Logging needs improvement
- Information on why the artifact was refused not visible on the client side (in the future: the status string should say "Invalid signature for artifact URI", "No signature for artifact [URI] with digest 0x[DIGEST]" etc
- No HTTPS/TLS support for incoming connections yet (you can always put Apache HTTP Server or nginx proxy in front)

## Credits
- Some code is from the [Verify PGP signatures plugin](https://github.com/s4u/pgpverify-maven-plugin)

## Related/other Approaches
- For verification of dependencies only (does not cover plugins): [Verify PGP signatures plugin](https://github.com/s4u/pgpverify-maven-plugin)
- For fixing Maven so it handles this itself: [MNG-5814](https://issues.apache.org/jira/browse/MNG-5814)
- For fixing Maven by extending the POM to include trust information: [MNG-6026](https://issues.apache.org/jira/browse/MNG-6026)
