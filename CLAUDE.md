# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Scope

This repository currently contains multiple Vue 3 frontends under `Web/`. Each app is a separate Vite project with its own dependencies, dev server port, routing tree, and API surface. Do not assume cross-app shared packages beyond the repeated upstream template structure.

## QuestionBank-Admin

### Common Commands

Run all commands from `Web/QuestionBank-Admin`.

```bash
pnpm install
pnpm dev
pnpm build
pnpm build:staging
pnpm preview
pnpm lint
pnpm test
pnpm vitest --run tests/utils/validate.test.ts
pnpm vitest --run tests/components/Notify.test.ts
```

Notes:
- `pnpm dev` starts Vite on port `3335` and proxies `/api` to `http://127.0.0.1:8080`.
- Production build runs `vue-tsc && vite build`; staging build uses `--mode staging`.
- Vitest is configured in `vite.config.ts` with `happy-dom` and `tests/**/*.test.{ts,js}`.
- The Husky pre-commit hook runs `npx vue-tsc` and then `npx lint-staged`, so type errors will block commits even if `pnpm lint` passes.

### High-Level Architecture

This app is a Vue 3 + Vite + TypeScript admin frontend derived from `v3-admin-vite`, but it has been adapted into a question-bank management system for administrators.

- App bootstrap lives in `src/main.ts`: create app, install shared plugins, attach Pinia and Vue Router, then mount after `router.isReady()`.
- Global plugin registration is centralized in `src/plugins/index.ts`, which wires Element Plus icon components, the permission directive, SVG icon support, and VXE Table once at startup.
- Routing is defined in `src/router/index.ts`. Most business pages are currently in `constantRoutes`, organized by domains like system logs, question bank management, and learning statistics.
- Route access control is enforced in `src/router/guard.ts`: token comes from cookies, user info is fetched lazily on first guarded navigation, and allowed routes are injected through the permission store.
- `src/router/config.ts` has `dynamic: true`, so permission flow matters even though most route declarations are static; the guard still loads roles and calls the permission store before allowing navigation.
- State is organized with Pinia in `src/pinia`. `stores/user.ts` owns token, roles, username, logout/reset behavior, and `getInfo()`, which calls the current-user API and supplies fallback roles from router config.
- HTTP access is centralized in `src/http/axios.ts`. All page-level API modules call the exported `request()` helper, which injects `Authorization`, merges defaults, unwraps backend `code`, and handles auth/network failures with global `ElMessage` feedback.
- Backend-facing API wrappers are colocated with features: shared auth/user calls live in `src/common/apis`, while page-specific endpoints live under each page directory such as `src/pages/questionBanks/apis` or `src/pages/activityRecords/apis`.
- Business screens are feature-first under `src/pages`. Each major route usually has its own page component plus a local `apis/` folder, so when changing a feature, start from the page directory rather than searching globally.
- Layout shell code is under `src/layouts`, with multiple layout modes and shared pieces like sidebar, breadcrumb, tags view, and settings drawer.
- Shared reusable code is under `src/common`: assets, composables, utility functions, generic components, and cache helpers used across pages and stores.

### Build and Tooling Details

- Path aliases in `vite.config.ts`: `@` -> `src`, `@@` -> `src/common`.
- Auto-import scans `src/pinia/stores` and `src/common/composables`, so some store/composable symbols may appear without explicit imports.
- SVG icons are generated from `src/common/assets/icons`, with a special preserve-color directory at `src/common/assets/icons/preserve-color`.
- Manual chunk splitting groups Vue core, Element Plus, and VXE Table into separate bundles.
- CI deploy workflow in `.github/workflows/deploy.yml` installs with pnpm, runs `pnpm build`, and deploys `dist` to GitHub Pages.

### Existing Tests

Current test coverage is light and mostly template-level. Existing tests are in `tests/`, including `tests/utils/validate.test.ts`, `tests/components/Notify.test.ts`, and `tests/demo.test.ts`. For feature work, do not assume the current suite protects page-level behavior.

### Upstream Context From README

The app inherits several upstream conventions from `v3-admin-vite`: Vue 3 Composition API with `script setup`, Element Plus UI, Pinia, Vue Router, UnoCSS, SCSS theme variables, multi-layout support, and role-based permission routing. Keep that template heritage in mind before refactoring core layout, router, or store infrastructure.

## QuestionBank-Web

### Common Commands

Run all commands from `Web/QuestionBank-Web`.

```bash
pnpm install
pnpm dev
pnpm build
pnpm build:staging
pnpm preview
pnpm lint
pnpm test
pnpm vitest --run tests/utils/validate.test.ts
pnpm vitest --run tests/components/Notify.test.ts
```

Notes:
- `pnpm dev` starts Vite on port `3334` and proxies `/api` to `http://127.0.0.1:8080`.
- Build, lint, test, and Vitest configuration are effectively the same shape as the admin app.
- This app also keeps the upstream Husky flow, so `vue-tsc` and `lint-staged` are part of commit-time validation.

### High-Level Architecture

This app reuses the same Vue 3/Vite template foundation as the admin app, but the route tree and feature modules are end-user facing around studying, practicing, and managing personal question banks.

- The same bootstrap pattern appears in `src/main.ts`: create app, install shared plugins, then register Pinia and router.
- Routing in `src/router/index.ts` is centered on three areas: auth pages under `/user/*`, learner dashboard pages under `/home` and its child views, and question-bank workflows under `/question-bank/*`.
- `src/pages/home/index.vue` acts as a landing/dashboard page and links users into public banks, learning statistics, and the wrong-question workflow.
- `src/pages/question-bank/index.vue` is the main personal-bank workspace. It loads created and favorited banks, filters them client-side, and routes into create/detail/practice/exam/manage subflows.
- Practice behavior is substantial and stateful in `src/pages/question-bank/views/practice.vue`: it loads questions by bank, tracks answer history and progress, persists activity records through page-local API modules, and saves on navigation or visibility changes.
- Learning analytics live under `src/pages/home/views/statistics.vue`, where ECharts renders trend and progress visualizations based on page-local statistics APIs.
- Shared auth/user flow still comes from Pinia plus cookie-backed router guards. The app keeps the same `src/pinia/stores/user.ts` pattern and `src/common/apis/users/index.ts` current-user request used by the template guard flow.
- API wrappers are split by feature under `src/pages/home/apis` and `src/pages/question-bank/apis`, so feature work usually requires changing both the page view and its colocated API helpers.

### Build and Tooling Details

- Path aliases remain `@` -> `src` and `@@` -> `src/common`.
- Vite auto-import and component auto-registration follow the same upstream pattern as the admin app.
- This app adds `echarts` to the dependency set for the learning-statistics views.
- The structure is close to `QuestionBank-Admin`, so shared fixes can sometimes be ported, but verify ports, route names, and page responsibilities before copying changes.

### Existing Tests

The visible tooling and test layout match the upstream template, which means tests exist but are still sparse relative to the customized learner-facing flows. Treat interactive pages like practice, exam, and statistics as needing targeted verification after changes.

## UserHub-Web

### Common Commands

Run all commands from `Web/UserHub-Web`.

```bash
pnpm install
pnpm dev
pnpm build
pnpm build:staging
pnpm preview
pnpm lint
pnpm test
pnpm vitest --run tests/utils/validate.test.ts
pnpm vitest --run tests/components/Notify.test.ts
```

Notes:
- `pnpm dev` starts Vite on port `3333` and proxies `/api` to `http://127.0.0.1:8080`.
- The file encoding/comments in some files appear messy, but the runtime structure still follows the same Vite + Vue 3 template as the other two apps.
- Login flow includes both captcha and token APIs in `src/pages/login/apis/index.ts`.

### High-Level Architecture

`Web/UserHub-Web` currently looks closest to the upstream admin-template baseline, but with user/account-oriented auth endpoints and a separate runtime port.

- App startup still follows the same pattern as the other apps: `src/main.ts` creates the app, installs shared plugins, then mounts with Pinia and router.
- Routing in `src/router/index.ts` is still largely template-oriented, with dashboard-first navigation and the standard redirect/error/auth setup rather than the question-bank learner flows.
- `src/router/config.ts` still enables dynamic routing through role loading, so the permission-store/guard model remains relevant here too.
- `src/pinia/stores/user.ts` follows the same template contract as the admin app: token and roles in store state, current-user lookup via `src/common/apis/users/index.ts`, logout/reset helpers, and fallback roles from router config.
- Shared request/auth infrastructure is still template-shaped, so if auth breaks here, inspect the same chain as in the other apps: login API -> cookie token helpers -> user store -> route guard -> centralized axios wrapper.
- Because this app is less customized than `QuestionBank-Web`, treat it as a separate app that may still be mid-migration from the upstream template rather than as a polished feature-complete user portal.

### Build and Tooling Details

- Tooling conventions match the other apps: Vite, Vue 3, TypeScript, Element Plus, UnoCSS, Pinia, Vue Router, Vitest, and the same alias layout.
- The port differs from the other two apps, so do not assume one frontend can be started in place of another during local development.
- If you need to compare behavior across apps, `UserHub-Web` is the best place to inspect unmodified template patterns before applying changes to the more customized projects.
