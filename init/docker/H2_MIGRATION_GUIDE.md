# H2 Database Migration to Docker

This guide explains how to migrate your existing H2 database files to the new Docker environment.

## 1. Extract your Database File

Extract the `db.mv.db` file from your backup archive (`c:\desenv\tedros\backup\.tedrosData.zip`).

## 2. Start the H2 Container

Run this command from `c:\desenv\tedros\docker` to spin up the H2 container. This ensures the Docker volume `h2_data` is initialized.

```bash
docker compose up -d h2db
```

## 3. Copy the File into the Volume

Copy your extracted `.mv.db` file to the running container using Docker's `cp` command:

```bash
docker cp "c:\path\to\extracted\db.mv.db" tedros-h2db:/opt/h2-data/db.mv.db
```

*(Replace `"c:\path\to\extracted\db.mv.db"` with the actual location where you extracted it).*

## 4. Fix Permissions

Because Docker `cp` might assign default root ownership to the file, and H2 needs write access to the database, apply the following command to correct ownership to `h2:h2` (or simply make it writable by running users):

```bash
docker exec -u root -it tedros-h2db chown -R 1000:1000 /opt/h2-data
```

*(Note: H2 official images usually run under UID/GID 1000).*

## 5. Restart the Database Container

Apply the file ownership changes and load your database.

```bash
docker compose restart h2db
```

Your database is now available securely at `jdbc:h2:tcp://h2db:1521/db` for the TomEE cluster.
