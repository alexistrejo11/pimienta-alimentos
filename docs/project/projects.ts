import type { ProjectModel } from "./projects.model";

const repositoryLink = "https://github.com/alexis-trejo-11/pimienta-alimentos";
const recentCommits = [
  {
    hash: "2bde20a",
    message:
      "feat(global): publish APK and integrate it into the web environment",
    date: new Date("2026-09-10"),
  },
  {
    hash: "f16643c",
    message:
      "feat(pos): add HQ ACL, web central, stock projection, and Android B7",
    date: new Date("2026-09-09"),
  },
  {
    hash: "4922a2d",
    message: "feat(pos): implement workers and add the sandbox feature",
    date: new Date("2026-09-09"),
  },
  {
    hash: "02626da",
    message: "feat(backend): implement the POS integration",
    date: new Date("2026-09-08"),
  },
  {
    hash: "b27454f",
    message: "chore(pos): implement manager panel",
    date: new Date("2026-09-07"),
  },
];

export const PROJECTS: ProjectModel[] = [
  {
    slug: "pimienta-alimentos",
    name: "Pimienta Alimentos",
    type: "multiplatform",
    description:
      "Monorepo for Pimienta Alimentos operations software: a public and staff web application, a central operations API, and an Android point of sale for school cafeterias.",
    status: "production",
    docs: {
      product: true,
      services: true,
      overview: {
        techStack: [
          { icon: "language", label: "Angular" },
          { icon: "database", label: "Spring Boot" },
          { icon: "phone_android", label: "Android" },
          { icon: "storage", label: "PostgreSQL" },
          { icon: "deployed_code", label: "Docker" },
        ],
        highlightedFeatures: [
          {
            icon: "business_center",
            label: "Company Operations",
            description:
              "One suite covers staff, HR, CRM, payroll, contracts, inventory, and operational work.",
          },
          {
            icon: "point_of_sale",
            label: "Cafeteria POS",
            description:
              "A local-first Android POS supports school-cafeteria sales and later synchronization.",
          },
          {
            icon: "sync",
            label: "Connected Platforms",
            description:
              "Web, API, and POS exchange operational data through explicit authenticated flows.",
          },
        ],
      },
    },
    metadata: {
      repositoryLink,
      deploymentLink: "https://pimienta-alimentos.com",
      license: "Apache-2.0",
      version: "2.1.0",
      recentCommits,
      metrics: [
        { label: "Platforms", value: 3 },
        { label: "Backend Java files", value: 1015 },
        { label: "Web TypeScript files", value: 133 },
      ],
    },
    services: [
      {
        slug: "pimienta-web",
        name: "Pimienta Alimentos Web",
        type: "frontend",
        description:
          "Angular web application for Pimienta Alimentos public information and the authenticated workspace for company operations and POS administration.",
        status: "production",
        docs: {
          product: true,
          architecture: true,
          features: true,
          infrastructure: true,
          overview: {
            techStack: [
              { icon: "language", label: "Angular" },
              { icon: "dns", label: "Node.js" },
              { icon: "style", label: "Tailwind CSS" },
              { icon: "api", label: "REST" },
              { icon: "deployed_code", label: "Docker" },
            ],
            highlightedFeatures: [
              {
                icon: "public",
                label: "Public Presence",
                description:
                  "The site presents company information, quality commitments, and legal notices.",
              },
              {
                icon: "dashboard",
                label: "Staff Workspace",
                description:
                  "Authenticated staff manage operational areas from a role-aware workspace.",
              },
              {
                icon: "point_of_sale",
                label: "POS Central",
                description:
                  "Administrators manage cafeterias, devices, catalog data, incidents, and reports.",
              },
            ],
            highlightedCommand: {
              title: "Run the web app locally",
              command: "cd web && npm start",
            },
          },
        },
        metadata: {
          repositoryLink,
          deploymentLink: "https://pimienta-alimentos.com",
          license: "Apache-2.0",
          version: "2.1.0",
          recentCommits,
          metrics: [
            { label: "TypeScript files", value: 133 },
            { label: "Page TypeScript files", value: 65 },
            { label: "Production routes", value: 6 },
          ],
        },
      },
      {
        slug: "pimienta-backend",
        name: "Pimienta Alimentos API",
        type: "backend",
        description:
          "Spring Boot REST API for Pimienta Alimentos operations, including identity, HR, CRM, inventory, payroll, files, notifications, and point-of-sale synchronization.",
        status: "production",
        docs: {
          product: true,
          architecture: true,
          features: true,
          infrastructure: true,
          apiDocs: true,
          apiExplorer: true,
          overview: {
            techStack: [
              { icon: "code", label: "Java" },
              { icon: "database", label: "Spring Boot" },
              { icon: "storage", label: "PostgreSQL" },
              { icon: "memory", label: "Redis" },
              { icon: "cloud", label: "Amazon S3" },
              { icon: "deployed_code", label: "Docker" },
            ],
            highlightedFeatures: [
              {
                icon: "admin_panel_settings",
                label: "Role Scoped Access",
                description:
                  "JWT authentication and headquarters access rules protect staff and POS administration.",
              },
              {
                icon: "inventory_2",
                label: "Operational Domains",
                description:
                  "Dedicated modules support the company workforce, commercial, and inventory workflows.",
              },
              {
                icon: "sync",
                label: "POS Synchronization",
                description:
                  "Device enrollment, bootstrap, changes, and idempotent event ingestion connect cafeterias.",
              },
            ],
            highlightedCommand: {
              title: "Read the live OpenAPI document",
              command: "curl https://api.pimienta-alimentos.com/v3/api-docs",
            },
          },
        },
        metadata: {
          repositoryLink,
          deploymentLink: "https://api.pimienta-alimentos.com",
          license: "Apache-2.0",
          version: "2.1.0",
          recentCommits,
          metrics: [
            { label: "Java source files", value: 1015 },
            { label: "Mapped HTTP operations", value: 200 },
            { label: "Integration test classes", value: 22 },
          ],
        },
      },
      {
        slug: "pimienta-pos",
        name: "Pimienta POS",
        type: "mobile",
        description:
          "Native Android point of sale for Pimienta Alimentos school cafeterias, designed for local sales operation and synchronization with the central API.",
        status: "development",
        docs: {
          product: true,
          architecture: true,
          features: true,
          infrastructure: true,
          overview: {
            techStack: [
              { icon: "phone_android", label: "Android" },
              { icon: "code", label: "Kotlin" },
              { icon: "palette", label: "Jetpack Compose" },
              { icon: "storage", label: "Room" },
              { icon: "sync", label: "WorkManager" },
              { icon: "api", label: "Retrofit" },
            ],
            highlightedFeatures: [
              {
                icon: "offline_bolt",
                label: "Local First Sales",
                description:
                  "Sales are recorded in a device database before the central API is contacted.",
              },
              {
                icon: "manage_accounts",
                label: "Device Operations",
                description:
                  "Enrollment, operator access, cash opening, and manager controls are built into the app.",
              },
              {
                icon: "sync",
                label: "Resilient Sync",
                description:
                  "A constrained background worker uploads POS events and recovers from stale cursors.",
              },
            ],
            highlightedCommand: {
              title: "Build a debug APK",
              command: "cd mobile/pos && ./gradlew :app:assembleDebug",
            },
          },
        },
        metadata: {
          repositoryLink,
          license: "Apache-2.0",
          version: "2.1.0",
          recentCommits,
          metrics: [
            { label: "Kotlin source files", value: 34 },
            { label: "Gradle application modules", value: 1 },
            { label: "Main POS screen files", value: 4 },
          ],
        },
      },
    ],
  },
];
