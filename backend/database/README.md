# Database Setup and Flyway Migrations

This backend uses Flyway for production database schema migrations. Flyway reads migration files from `src/main/resources/db/migration` and records applied versions in `flyway_schema_history`.

Do not commit real data, database dumps, credentials, password hashes, email addresses, API keys, or other secrets.

## Workflow A: Teammate With a New Empty Local Database

1. Run `database/create-database.sql` in MySQL to create the empty `syde_meal_planner` database.
2. Configure `DB_USERNAME` and `DB_PASSWORD` in your local environment or IDE run configuration.
3. Keep `FLYWAY_BASELINE_ON_MIGRATE=false`. This is the committed default.
4. Start the backend.
5. Flyway executes `V1__create_initial_schema.sql` and creates all six application tables plus `flyway_schema_history`.

Teammates must not enable baseline for an empty database. Baseline is only for adopting an existing populated database whose schema already matches V1.

## Workflow B: Original Developer With an Existing Populated Database

1. Back up the existing local database before enabling Flyway.
2. Confirm the existing schema matches `V1__create_initial_schema.sql`.
3. Temporarily set `FLYWAY_BASELINE_ON_MIGRATE=true`.
4. Start the backend exactly once.
5. Confirm `flyway_schema_history` was created and version `1` was baselined.
6. Stop the backend.
7. Remove `FLYWAY_BASELINE_ON_MIGRATE` or change it back to `false`.
8. Start normally.

Baseline does not delete or rebuild existing tables. Baseline does not execute V1 against the populated database. It only tells Flyway that the existing schema should be treated as version 1.

## Future Migrations

Future schema changes must use new versioned files such as `V2__...sql`, `V3__...sql`, and later. Never edit a migration that has already been applied to a shared environment.
