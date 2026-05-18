# How to contribute to ActaCofrade

Thanks for stopping by. ActaCofrade started as a Final Degree Project, but it is designed so that any brotherhood or confraternity can use it and so that anyone who wants to can help improve it. This guide explains how we work and what you need to do so that a contribution comes in cleanly.

## Before you start

- Read the [README](README.md) and the [deployment guide](DEPLOY.md) first. That gives you a general overview of what the project does and how to run it locally.
- Contributions are governed by the [Code of Conduct](CODE_OF_CONDUCT.md). By opening an issue or a pull request, you accept those terms.
- If you are not sure whether a change fits the project, open an issue first describing the idea. It is better to discuss the approach before spending hours on code that may need to be discarded.

## Types of contributions that are welcome

- **Bugs.** Minimal reproduction steps, browser or system version, backend logs if relevant.
- **Accessibility improvements.** The project follows WCAG AA and keyboard navigation; any gap you find is welcome.
- **Documentation.** Small corrections or full chapter rewrites if you find something outdated.
- **New functionality.** Talk it over in an issue first to agree on scope.
- **Translations.** The interface is in Spanish, but the API and messages could be internationalised.

## Development setup

```bash
git clone https://github.com/Aranaaa00/actacofrade.git
cd actacofrade
cp .env.example .env       # fill in the variables marked as required
docker compose up -d --build
```

If you prefer to run the backend and frontend separately outside of Docker:

```bash
# Backend (requires Java 21 and a running Postgres 15)
cd backend
./mvnw spring-boot:run

# Frontend (requires Node 22)
cd frontend
npm ci
npm start
```

The backend listens on `8080`, the frontend on `4200` with `proxy.conf.json` pointing to the local backend.

## Key rules when touching code

### Backend (Spring Boot 4 / Java 21)

- **Controllers** validate input (`@Valid`), call a service, and return a `ResponseEntity` with a DTO. They do not touch repositories or hold business logic.
- **Contextual authorisation logic** lives in `AuthorizationHelper`. Do not duplicate it in concrete services: add a new method there if needed.
- Every **authenticated request** is scoped to the user's brotherhood. Any service that returns a collection must filter by `hermandadId`.
- **Schema changes** must always be a new Flyway migration with a number higher than the last one (`Vxx__description.sql`). Do not edit existing migrations or fill in V5 (it is reserved).
- Tests are run with `./mvnw verify`. JaCoCo requires **≥ 80% line coverage** on `service`, `controller`, `security` and `util`. If you go below that, the build fails.

### Frontend (Angular 20)

- **Standalone** components, no `NgModules`.
- Templates using the new control flow (`@if`, `@for` with `track`, `@switch`). Avoid `*ngIf` / `*ngFor` in new code.
- **Signals** whenever the data is reactive. For subscriptions, use `takeUntilDestroyed()`.
- Styles following the **ITCSS** methodology (`00-settings` → `06-utilities`) described in [`docs/04-guia-estilos.md`](docs/04-guia-estilos.md) and BEM in components.
- Karma requires **≥ 85%** on statements, branches, functions and lines. Do not lower the threshold; add tests instead.

### Commit style

We use [Conventional Commits](https://www.conventionalcommits.org/) in Spanish:

```
type(optional scope): short description in imperative form

Optional body with more context, decisions made and why.
```

Common types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `ci`, `style`, `perf`, `build`.

A real example from the repo history:

```
fix(frontend): corregir visibilidad de la barra de busqueda y alinear el asignado en el detalle del acto
```

DCO commit signing is not required. Atomic commits are appreciated: one commit, one intention.

## Pull requests

1. Fork the repository or create a branch directly if you have permissions: `feat/short-name`, `fix/issue-123`, `docs/contributing`.
2. Before opening the PR, verify locally:
   - `cd backend && ./mvnw verify` passes.
   - `cd frontend && npm test -- --watch=false` passes.
   - `cd frontend && npm run build` does not introduce new warnings.
   - `docker compose up -d --build` brings all three services to healthy.
3. In the PR description, explain:
   - What changes and why.
   - How you tested it.
   - If it affects the database, environment variables, or the public API.
4. CI workflows run automatically. Trivy is informational; tests are not.
5. The integration branch is `main`. PRs are squash-merged unless the commit history adds real value.

## Reporting a vulnerability

If you find a security issue, **do not open a public issue**. Write directly to the project author (GitHub profile in the repository) with the details. I will acknowledge receipt as soon as I can and we will work on a coordinated fix before making the advisory public.

## Something not covered here

The style guides (visual and code), the full architecture, the testing strategy, and the user manual are all in the [`docs/`](docs/) folder. If you still have questions after reading them, open an issue with the `pregunta` label and we will sort it out.

Thanks again for taking the time to work on the project.
