# ActaCofrade Frontend

SPA en Angular 19 (standalone components, lazy loading en todas las rutas) que consume la API REST del backend. En producción se construye con `ng build --configuration production` y se sirve desde una imagen `nginx:alpine` que además hace de reverse proxy hacia el backend. La descripción general del proyecto y la arquitectura completa están en el [README raíz](../README.md) y en [DEPLOY.md](../DEPLOY.md).

## Índice

- [Stack y estructura](#stack-y-estructura)
- [Desarrollo local](#desarrollo-local)
- [Build de producción](#build-de-producción)
- [Tests](#tests)
- [Estilos y arquitectura SCSS](#estilos-y-arquitectura-scss)

---

## Stack y estructura

| Capa | Tecnología |
|---|---|
| Framework | Angular 19 (standalone, sin `NgModules`) |
| Lenguaje | TypeScript 5 |
| Estilos | SCSS con arquitectura ITCSS |
| Iconos | Lucide Angular |
| HTTP | `HttpClient` con interceptor JWT |
| Tests | Karma + Jasmine, cobertura con Istanbul |

```
src/
├── app/
│   ├── guards/          authGuard, roleGuard
│   ├── interceptors/    authInterceptor (inyecta el JWT)
│   ├── layout/          shell de la aplicación (sidebar, header)
│   ├── models/          tipos compartidos
│   ├── pages/           páginas (lazy-loaded)
│   ├── services/        clientes HTTP de la API
│   └── shared/          componentes reutilizables
├── styles/              ITCSS (00-settings → 06-utilities)
├── testing/             utilidades de tests
└── index.html           meta, OG, Twitter Card y carga de fuentes
public/                  favicon, logos, robots.txt, sitemap.xml
```

## Desarrollo local

```bash
npm ci
npm start          # ng serve, escucha en http://localhost:4200
```

El backend en local se puede atacar de dos formas:

- **Con todo el stack en Docker** (recomendado): `docker compose up -d --build` desde la raíz. La SPA queda en `http://localhost/` y nginx hace el proxy de `/api`. `ng serve` no es necesario.
- **Solo el frontend en `ng serve`**: el proxy de Angular reescribe `/api` hacia `http://localhost:8080` mediante `proxy.conf.json`. Hay que tener el backend levantado por separado.

## Build de producción

```bash
npx ng build --configuration production
```

Genera los estáticos en `dist/frontend/browser/`. La imagen Docker (`Dockerfile`) hace este mismo build y lo copia a `/usr/share/nginx/html` dentro del contenedor.

## Tests

```bash
npm test                       # ejecuta Karma una vez en modo headless
npm test -- --watch=false      # idéntico, pensado para CI
```

El informe de cobertura se genera en `coverage/frontend/`. Los umbrales mínimos están configurados en `karma.conf.js` (≥ 85% en statements, branches, functions y lines).

## Estilos y arquitectura SCSS

La hoja de estilos sigue ITCSS:

| Capa | Para qué |
|---|---|
| `00-settings/` | Variables SCSS y custom properties (paleta, tipografía, espacios, breakpoints). |
| `01-tools/` | Mixins (`from-mobile`, `from-tablet`, `from-desktop`, `from-wide`, `visually-hidden`). |
| `02-generic/` | Reset y normalizaciones. |
| `03-elements/` | Estilos base de elementos HTML (`body`, `a`, `button`, `input`, `:focus-visible`). |
| `04-layout/` | Estructura global de la página. |
| `05-components/` | Componentes reutilizables (botones, badges, tablas, modales, sidebar, datepicker...). Cada uno en su propio fichero. |
| `06-utilities/` | Helpers atómicos. |

Nomenclatura BEM en clases (`block__element--modifier`), media queries siempre a través de los mixins y nada de valores mágicos sueltos.

