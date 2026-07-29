# Fixing Flyway "Migration checksum mismatch"

When you see:
```
Migration checksum mismatch for migration version 11
-> Applied to database : 1854878766
-> Resolved locally    : -1981549453
```
it means the migration file **V11** was changed (or renamed and its content changed) **after** it was already applied. Flyway does not allow changing applied migrations.

## Option 1: Run Flyway repair (recommended)

Repair updates the stored checksum so it matches your **current** migration file. After that, do **not** edit V11 again; use a new version (e.g. V13) for any further changes.

### Using Flyway CLI

If you have [Flyway CLI](https://flywaydb.org/documentation/usage/commandline/) installed:

```bash
cd muxon-core/libs/core-persistence
flyway repair \
  -url="jdbc:postgresql://<host>:<port>/<database>" \
  -user="<user>" \
  -password="<password>" \
  -locations="filesystem:src/main/resources/db/migration"
```

Use the same URL/user/password as your application.

### Using Gradle (with Flyway plugin)

If you add the [Flyway Gradle plugin](https://flywaydb.org/documentation/usage/gradle/) to the project, you can run:

```bash
./gradlew flywayRepair
```

### Using SQL (if you cannot run repair)

As a last resort you can align the checksum in the database with the value Flyway reports as "Resolved locally". **Only do this if you cannot run `flyway repair`.**

```sql
UPDATE flyway_schema_history
SET checksum = -1981549453
WHERE version = '11';
```

Then run your application (or `flyway migrate`) so any pending migrations (e.g. V12) are applied.

## After repair

- **Validate** will pass.
- **Migrate** (or app startup) will run any migrations that were not yet applied (e.g. V12).
- Do **not** edit V11 again; for schema changes add a new migration (e.g. V13).
