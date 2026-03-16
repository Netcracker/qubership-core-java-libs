# This document contains issues and tasks which break backward compatibility.

## Deprecation and of `CassandraSessionBuilder#build(CassandraDBConnection)` method.

Method build(CassandraDBConnection) can't provide necessary information for proper metrics registration.
This information can only be obtained from CassandraDatabase instance so CassandraSessionBuilder#build(CassandraDatabase) method was added.

### What should be done before major release:

Remove CassandraSessionBuilder#build(CassandraDBConnection) method.
Optional: prepareBuilder and prepareConfigLoader methods were extracted to avoid code duplication between two build methods. Move code from them back to the build(CassandraDatabase) method to reduce code complexity.