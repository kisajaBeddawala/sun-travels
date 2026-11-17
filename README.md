# Sun Travels

Sun Travels manages hotel contracts and quotes room stays against contracted rates and allocations. A contract manager can add a hotel and save an agreement with several room types. A call center agent can search with a check-in date, number of nights, and a separate adult count for each room.

## Run locally on Windows

Install Java 17 or later, Node.js 22.12 or later within the 22.x release line, and Docker with Compose. Maven Wrapper is in `backend`.

1. From the project folder, start PostgreSQL and wait for the database to be healthy.

   ```powershell
   docker compose up -d --wait db
   ```

2. In the same terminal, start the backend. Set `DEMO_ENABLED` to `true` for a fictional 2027 contract in an empty database.

   ```powershell
   Set-Location backend
   $env:DEMO_ENABLED = 'true'
   .\mvnw.cmd spring-boot:run
   ```

3. In a second terminal opened in the project folder, install and start Angular. The initial install of Java and Angular packages needs internet access.

   ```powershell
   Set-Location frontend
   npm install
   npm start
   ```

4. Open [Sun Travels](http://127.0.0.1:4200/). Select **Search rooms** with the prefilled demo inputs to see **USD 362.25** for Standard and **USD 569.25** for Sea View. The sample contract covers June 10–13, 2027. The user manual walks through adding and opening a contract. [User manual](docs/user-manual.md)

On macOS or Linux, run `./mvnw spring-boot:run` in step 2 and set the demo flag with `export DEMO_ENABLED=true`. If you use an existing PostgreSQL server, create a dedicated database and set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the backend terminal. The Docker defaults target `jdbc:postgresql://localhost:5433/sun_travels` with disposable local credentials. Spring applies the schema on startup. The frontend proxies `/api` to port `8080`. Stop the app with Ctrl+C in each terminal and stop the Docker database with `docker compose stop db`. Its volume retains saved contracts.

## Verify

Run `.\mvnw.cmd test` from `backend` to check price calculation, allocation, per-room occupancy, date boundaries, contract validation, and JSON responses. Those tests do not require PostgreSQL. Run `npm run build` from `frontend` after installing dependencies. For an end-to-end check on a running app, follow the examples in the user manual and the acceptance cases. [Design and acceptance cases](docs/design-and-verification.md)

With the backend running against a test database, run `.\scripts\smoke.ps1` from the project folder to exercise the API and PostgreSQL together. The script creates a new test hotel and agreement each time.

## Project structure

- `backend` contains the Spring Boot REST API, a Spring JDBC repository, and the PostgreSQL schema.
- `frontend` contains the Angular pages for search, contract entry, and contract lookup.
- `compose.yaml` runs a local PostgreSQL 16 instance.
- `docs` contains the user manual, ER diagram, user stories, and acceptance cases.

The first version uses USD as its quote currency. Each result evaluates one room type for the entire group, and each contract must cover every occupied night. The app reads allocation when calculating availability and keeps it unchanged when a quote is requested. PostgreSQL persists contracts across app restarts.

## Publish to GitHub

Create an empty repository in your GitHub account or organization, then run these commands from this project folder after the local checks pass. Replace the example owner with your account or organization. For a repository that already has commits, fetch and inspect its history before integrating this project.

```powershell
git remote add origin https://github.com/YOUR-OWNER/sun-travels.git
git push -u origin main
```

The training discussion also mentions daily GitLab commits. Confirm the evaluator’s submission channel if you are using GitHub for the assignment.
