# java-binrepo-proxy
Policy enforcing binary repository (i.e. Maven) (reverse) proxy

The proxy is to be placed between the developer (or build server) and the binary repository (i.e. the Central or an internal repository) and will enforce policies before serving the requested artifacts.

Artifacts are only to be retunred after its (OpenPGP) digital signature has been verified.

The purpose is to solve the lack of proper signature verification support in Maven for both project dependencies and plugins.
