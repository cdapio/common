# Cask Common: Authorization

**Introduction**

The Authorization module contains an authorization system that can be used to control access to protected resources.

## Usage (Server)

### In-Memory Mode

To use authorization in your own project, first start the `AuthorizationService`:

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

### Distributed Cluster Mode

To run the `AuthorizationService` in a distributed cluster, you can do the following:

```
Injector injector = Guice.createInjector(
  new DiscoveryRuntimeModule().getDistributedModules(),
  new AuthorizationRuntimeModule().getDistributedModules()
);

AuthorizationService service = injector.getInstance(AuthorizationService.class);
service.startAndWait();

ListenableFuture<Service.State> completionFuture = Services.getCompletionFuture(service);
completionFuture.get();
```

## Usage (Client)

Once you have an `AuthorizationService` running, you can use the `AuthorizationClient` to
guard access to protected resources.

### Obtaining an AuthorizationClient (with Guice)

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

### Setting and Getting Access Control List (ACL) entries

You can set and get ACL entries using the `ACLClient`:

```
ObjectId secretFile = new ObjectId("FILE", "/tmp/sdf");
SubjectId currentUser = SubjectId.ofUser("bob");

// grant "bob" WRITE access to the "/tmp/sdf" file
client.setACL(secretFile, currentUser, "WRITE");

// get all ACL entries whose subject is "bob" and object is the "/tmp/sdf" file
client.getACLs(secretFile, currentUser);
```

### Controlling Access

You can protect your resources by using the `ACLClient`:

```
AuthorizationClient client;
ObjectId secretFile = new ObjectId("FILE", "/tmp/sdf");
SubjectId currentUser = SubjectId.ofUser("bob");

// verify that "bob" has WRITE access to the "/tmp/sdf" file
client.verifyAuthorized(secretFile, currentUser, "WRITE");
```

Alternatively, you can use `@RequiredPermissions` to guard access to method calls:

```
class SecretFile {
  @RequiresPermissions({ "WRITE" })
  public void write(String content) {
    ...
  }
}

class MyAuthorizationContext implements AuthorizationContext {
  public static SubjectId CURRENT_USER = null;
  public static SubjectId CURRENT_USER_GROUPS = null;

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

// will throw UnauthorizedException due to missing permissions
protectedSecretFile.write("lkj");
```
