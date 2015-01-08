# Cask Common: Security

**Introduction**

Security framework that provides authentication and authorization.

## Authorization

The Authorization module contains an authorization system that can be used to control access to protected resources.

### Definitions

* Object: A resource that may be access-controlled (e.g. file, database table)
* Subject: A user or group that acts on an object (e.g. you)
* Permission: A type of action that a subject can do on an object (e.g. write)
* ACL entry: Consists of an object type and unique identifier, subject type and unique identifier, and a permission

### Usage

To use authorization in your project, you must first run an `AuthorizationService`. This service
provides ways to create, destroy, and find ACL entries. It can be run in in-memory mode
for development and testing, or distributed mode for running in a cluster with ZooKeeper.

Once the `AuthorizationService` is running, you can use an `AuthorizationClient` to verify
that a certain subject has certain permissions for an object, set ACL entries, and list ACL entries.

Alternatively, you can use an `AuthorizationProxyFactory` to acquire proxied objects that authorize
on calls to methods that are annotated with `@RequiresPermissions`.

### Usage (Server)

#### In-Memory Mode

In in-memory mode, the `AuthorizationService` manages the ACL entries in-memory.
It requires no external dependencies, but the ACL entries are not persisted.
Furthermore, the `AuthorizationClient` must be used in the same Java program,
since the `DiscoveryServiceClient` stores its state in-memory.

To run the `AuthorizationService` in in-memory mode, use

```
Injector injector = Guice.createInjector(
  new DiscoveryRuntimeModule().getInMemoryModules(),
  new AuthorizationRuntimeModule().getInMemoryModules()
);

AuthorizationService service = injector.getInstance(AuthorizationService.class);
service.startAndWait();

ListenableFuture<Service.State> completionFuture = Services.getCompletionFuture(service);
completionFuture.get();
```

#### Distributed Cluster Mode

In distributed mode, the `AuthorizationService` manages the ACL entries in user-defined storage.
This way, ACL entries may be persisted if the user desires. `AuthorizationService` requires
ZooKeeper to be running, so that the `AuthorizationService` can make itself
discoverable to the `AuthorizationClient` via ZooKeeper.

To run the `AuthorizationService` in a distributed cluster, you will have to define your own `ACLStore` implementation
if you wish to persist the ACL store:

```
class MyACLStore implements ACLStore {
  ...
}

Module myACLStoreModule = new AbstractModule() {
  @Override
  protected void configure() {
    bind(ACLStore.class).to(MyACLStore.class);
  }
};
```

If ACL store persistence is not necessary, you can use the in-memory `AuthorizationClient` module:

```
Module myACLStoreModule = AuthorizationClientRuntimeModule.getInMemoryModules();
```

Then, initialize your `AuthorizationService` using the distributed discovery module and your `myACLStoreModule`:

```
Injector injector = Guice.createInjector(
  new DiscoveryRuntimeModule().getDistributedModules(),
  myACLStoreModule
);

AuthorizationService service = injector.getInstance(AuthorizationService.class);
service.startAndWait();

ListenableFuture<Service.State> completionFuture = Services.getCompletionFuture(service);
completionFuture.get();
```

### Usage (Client)

Once you have an `AuthorizationService` running, you can use the `AuthorizationClient` to
guard access to protected resources.

#### Obtaining an AuthorizationClient (with Guice)

If you have a `DiscoveryService` running, then you can use the `DiscoveryRuntimeModule`
to instantiate an `AuthorizationClient` which will discover where the `DiscoveryService` is
running using a `DiscoveryServiceClient`.

If `DiscoveryService` is running in the same process, you can use the in-memory modules:

```
Injector injector = Guice.createInjector(
  new DiscoveryRuntimeModule().getInMemoryModules()
);

AuthorizationClient client = injector.getInstance(AuthorizationClient.class);
```

If `DiscoveryService` is running in the same cluster, you can use the distributed modules:

```
Injector injector = Guice.createInjector(
  new DiscoveryRuntimeModule().getDistributedModules()
);

AuthorizationClient client = injector.getInstance(AuthorizationClient.class);
```

#### Setting and Getting Access Control List (ACL) entries

You can set and get ACL entries using the `AuthorizationClient`:

```
ObjectId secretFile = new ObjectId("FILE", "/tmp/sdf");
SubjectId currentUser = SubjectId.ofUser("Bob");

// Grant "Bob" WRITE access to the "/tmp/sdf" file
client.setACL(secretFile, currentUser, "WRITE");

// Get all ACL entries whose subject is "Bob" and object is the "/tmp/sdf" file
client.getACLs(secretFile, currentUser);
```

#### Controlling Access

You can protect your resources by using the `AuthorizationClient`:

```
AuthorizationClient client;
ObjectId secretFile = new ObjectId("FILE", "/tmp/sdf");
SubjectId currentUser = SubjectId.ofUser("Bob");

// Verify that "Bob" has WRITE access to the "/tmp/sdf" file - throws UnauthorizedException if not authorized
client.verifyAuthorized(secretFile, currentUser, Arrays.asList("WRITE"));
```

Alternatively, you can use `@RequiresPermissions` to guard access to method calls:

```
class SecretFile {
  @RequiresPermissions({ "WRITE" })
  public void write(String content) {
    ...
  }
}

class MyAuthorizationContext implements AuthorizationContext {
  public static SubjectId CURRENT_USER = SubjectId.ANON_USER;
  public static List<SubjectId> CURRENT_USER_GROUPS = Collections.EMPTY_LIST;

  @Override
  public SubjectId getCurrentUser() {
    return CURRENT_USER;
  }

  @Override
  public List<SubjectId> getCurrentUsersGroups() {
    return CURRENT_USER_GROUPS;
  }
}

Injector injector = Guice.createInjector(
  new AbstractModule() {
    @Override
    protected void configure() {
      bind(AuthorizationContext.class).to(MyAuthorizationContext.class);
    }
  },
  new DiscoveryRuntimeModule().getInMemoryModules(),
  new CGLibAuthorizationProxyRuntimeModule()
);

AuthorizationProxyFactory proxyFactory = injector.getInstance(AuthorizationProxyFactory.class);

SecretFile secretFile = new SecretFile();
SecretFile protectedSecretFile = proxyFactory.getProxy(secretFile, new ObjectId("FILE", "/tmp/sdf"));

MyAuthorizationContext.CURRENT_USER = SubjectId.ofUser("Bob");
// Will throw UnauthorizedException due to missing permissions
protectedSecretFile.write("lkj");
```
