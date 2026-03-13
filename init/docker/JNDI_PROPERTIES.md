# JNDI Configuration for JavaFX Client

Now that the system is properly load balanced and using SSL termination via Nginx, you must update the `jndi.properties` in your JavaFX client so it communicates via HTTPS to the cluster.

Find your properties, which previously looked like this:

```properties
java.naming.provider.url = http://72.61.54.227:8081/tomee/ejb
```

## Updated Properties

Replace or add the following properties:

```properties
java.naming.factory.initial = org.apache.openejb.client.RemoteInitialContextFactory
java.naming.provider.url = https://tedros.io/tomee/ejb
```

### Notes on SSL

Because we are migrating traffic to HTTPS:

- Ensure that the JavaFX client trusts the SSL certificate from Let's Encrypt (which is trusted by default in Java 17+).
- The EJB communication will now transparently be wrapped over HTTP2/SSL thanks to Nginx, hitting `tomee_cluster` in the background.
