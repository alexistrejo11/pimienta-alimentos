# Pimienta Alimentos Web

Angular 21 application for Pimienta Alimentos. It provides the public company
website, legal pages, authenticated staff workspace, and central POS
administration tools.

## Status

Version `2.1.0`. The production domain is acquired and configured. Production
deployment is performed by GitHub Actions after changes are merged into `main`.

## Features

- Public company and service information
- Legal and quality pages
- Staff authentication and role-aware workspace
- Employee, CRM, task, contract, payroll, and headquarters views
- POS administration, catalog, devices, incidents, and reports
- Public Android POS download through the backend release endpoint
- Server-side rendering and prerendered public routes

## Technology Stack

- Angular 21 and standalone components
- TypeScript and RxJS
- Angular SSR with Express
- Tailwind CSS 4
- Vitest and Angular testing tools
- Docker for production delivery

## Requirements

- Node.js 22 or later
- npm 11 or later
- A running backend API for authenticated features

## Local Development

```bash
npm ci
npm start
```

The development server runs on the default Angular port. Configure the API
base URL through the local environment configuration when required.

## Build and Test

```bash
npm run build
npm test
```

## Production Delivery

The web workflow builds the application, publishes a container image to GHCR,
and deploys the matching image to the production host through Cloudflare
Access. The workflow runs for relevant changes merged into `main`.

## Documentation

Web project navigation and detailed notes are organized under `docs/`.

## License

Apache License 2.0.
