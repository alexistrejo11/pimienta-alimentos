# Auth / client error messages follow-ups

## 2026-09-11 — Spanish presentation messages

- Client-facing `ApiErrorResponse.message` is resolved from `i18n/messages_es.properties` via `ClientErrorMessages` (fixed locale `es`). Domain exception English text remains for logs / fallback only.
- `POST /auth/register` use case returns `RegisterResult(requireAdminActivation)`; Spanish copy is assembled in `AuthController`.
- Bean Validation `fieldErrors[].message` may still be English annotation defaults; summary `message` is Spanish.

No extra follow-ups from this pass.
