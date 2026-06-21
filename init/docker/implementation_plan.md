# Goal Description

Migrate the Tedros JavaFX client-server application infrastructure from a standalone Ubuntu server to a containerized high-availability cluster using Docker Compose. The environment includes two TomEE Plume 9.1.3 instances, H2 Database (Server Mode), MongoDB, and Nginx (Load Balancer & SSL Termination).

## Proposed Changes

1. **Docker Compose (`docker-compose.yml`)**
   - **Services:**
     - `tomee1`, `tomee2`: Two instances of the application server built from a custom Dockerfile.
     - `h2db`: Official H2 database image (`oscarfonts/h2:2.2.224` or similar compatible with 1.99, wait, version specified is v1.99. `oscarfonts/h2` has tags for 1.4.199, which is v1.99).
     - `mongodb`: Official Mongo image (`mongo:8.0` since 8.2 doesn't exist yet but `mongo:latest` or `8.0-rc` can be used).
     - `nginx`: Load balancer and reverse proxy with SSL termination.
   - **Volumes:**
     - `h2_data`: Persistent volume for H2 database files.
     - `mongo_data`: Persistent volume for MongoDB.

2. **Dockerfile (`Dockerfile`)**
   - Base image: `eclipse-temurin:17-jre-jammy`.
   - Download and extract Apache TomEE Plume 9.1.3.
   - Copy customized configuration files ([server.xml](file:///c:/desenv/tedros/backup/server.xml), [tomee.xml](file:///c:/desenv/tedros/backup/tomee.xml), [catalina.properties](file:///c:/desenv/tedros/backup/catalina.properties), [system.properties](file:///c:/desenv/tedros/backup/system.properties), `tomcat-users.xml`) to the [conf/](file:///c:/desenv/tedros/backup/nginx.conf) directory.

3. **Nginx Configuration ([nginx.conf](file:///c:/desenv/tedros/backup/nginx.conf))**
   - Define an upstream block `tomee_cluster` with `server tomee1:8081;` and `server tomee2:8081;`.
   - Setup HTTPS server block listening on 443 with SSL certificates.
   - Proxy pass `/tomee/ejb` (and other app endpoints) to `http://tomee_cluster`.

4. **TomEE Configurations Adjustments**
   - In [tomee.xml](file:///c:/desenv/tedros/backup/tomee.xml), adjust the JDBC URL from `jdbc:h2:tcp://localhost/~/.tedrosData/h2/db` to `jdbc:h2:tcp://h2db:1521/tedrosData`.
   - Note: The `<Resource>` element will be configured for connection pooling and proper access to the new Docker service name `h2db`.

5. **H2 Database Migration Guide (`H2_MIGRATION_GUIDE.md`)**
   - Step-by-step instructions to extract `db.mv.db` from `.tedrosData.zip` and place it correctly in the `h2_data` Docker volume.

6. **JavaFX Client Properties (`jndi.properties` snippet)**
   - Updated connection properties to use HTTPS pointing to the Nginx Load Balancer (e.g. `java.naming.provider.url = https://tedros.io/tomee/ejb`).

## Verification Plan

### Automated Tests
- Run `docker compose config` to validate the `docker-compose.yml` syntax.

### Manual Verification
- Start the cluster with `docker compose up -d`.
- Verify containers are running: `docker compose ps`.
- Check Nginx logs to ensure load balancing between `tomee1` and `tomee2`.
- Test access using the JavaFX client via `https://tedros.io/tomee/ejb`. 
- Observe logs for successful connection to DBs: `docker compose logs tomee1`.
