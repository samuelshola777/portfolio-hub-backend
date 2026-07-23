# Portfolio Hub Backend

The matching responsive frontend uses the existing profile APIs and adds the
`BACKGROUND_THUMBNAIL` upload purpose. Copy `.env.example` into your local
environment and replace SMTP, Cloudinary and deployment secrets before use.

## Administration pagination and verification delivery

- Administration collection endpoints use `page` (1-based) and `size` (maximum 50) and return `items`, `currentPage`, `pageSize`, `totalItems`, `totalPages`, `hasNext`, and `hasPrevious`.
- `/api/v1/admin/private/users/{userId}` returns the account, public portfolio link, every portfolio field (including `null` values), and section counts. Each large section has its own paginated endpoint and is loaded separately by the frontend.
- Verification emails are queued with an `AFTER_COMMIT` transaction event. A failed registration cannot send an email.
- Verification resends have a persistent two-minute cooldown and return HTTP 429 while cooling down.
- The application still uses `spring.jpa.hibernate.ddl-auto=update`. `RoleConstraintUpdater` idempotently repairs an old PostgreSQL `users_role_check` so `PROFESSIONAL` and `BUSINESS_OWNER` registrations work without Flyway.
- Uploaded files now record the authenticated owner when an access token is supplied. Older file rows remain valid but may have no owner because that information did not previously exist.

The API supports separate professional and business-owner accounts. Existing `USER` rows remain readable as
professional accounts, while new registrations use `PROFESSIONAL` or `BUSINESS_OWNER`. A business owner may create
multiple independent businesses, each with a permanent `/business/{slug}` URL, guided website content, catalog,
orders, enquiries, appearance settings and ownership-scoped APIs.

Business endpoints live below `/api/v1/businesses`. Dynamic business collections and the new professional page
endpoints use one-based `page` parameters, a maximum `size` of 100, and return `items`, `currentPage`, `pageSize`,
`totalItems`, `totalPages`, `hasNext` and `hasPrevious`.

Database tables are managed with `spring.jpa.hibernate.ddl-auto=update`; Flyway is not used. Copy the safe values in
`.env.example` or `env.properties` into Render environment variables and replace every placeholder before deployment.

Independent Spring Boot API for Portfolio Hub. This project contains no Next.js source and can be maintained in its own Git repository and deployed to Render with PostgreSQL.

## Technology

- Java 21
- Spring Boot 4.1
- Spring Security and JWT
- Spring Data JPA
- PostgreSQL
- Cloudinary
- Brevo-compatible SMTP
- OpenPDF portfolio export and ZXing QR codes
- GitHub public-repository integration
- Swagger/OpenAPI

Hibernate manages the schema with:

```yaml
spring.jpa.hibernate.ddl-auto: update
```

Flyway is not used.

## Included product features

- Unique email addresses, unique usernames, JWT access/refresh tokens, email verification, password reset and authenticator-app 2FA
- Public recruiter portfolios with optional experience, education, certifications and achievements
- Categorized skills with proficiency levels and optional technology icons
- Project case studies with challenge, process, results, technology stack, links, thumbnails and media galleries
- Profile image, short introduction video, CV, social links, personal website and GitHub repositories
- Portfolio views, project clicks, CV downloads, source/location summaries, PDF exports and QR sharing
- Recruiter enquiries and a private owner inbox
- Super-admin user controls, user growth, active/published totals, storage, enquiries and activity audit data

## Local development

1. Create an empty PostgreSQL database named `portfolio_hub`.
2. Copy the values from `.env.example` into your IDE environment or terminal.
3. Start the API:

```bash
./mvnw spring-boot:run
```

The API runs at `http://localhost:9999`.

- Health: `http://localhost:9999/api/v1/system/public/health`
- Swagger: `http://localhost:9999/swagger-ui.html`
- OpenAPI JSON: `http://localhost:9999/api-docs`

## Authenticated upload flow

Upload files to:

```text
POST /api/v1/utilities/private/file/upload
```

Multipart fields:

- `file`: the uploaded file
- `category`: `IMAGE`, `VIDEO`, `DOCUMENT`, `AUDIO`, or `OTHER`
- `usageType`: `PROFILE_IMAGE`, `PROFILE_VIDEO`, `CV`, `WORK_THUMBNAIL`, `WORK_GALLERY_IMAGE`, `WORK_GALLERY_VIDEO`, `WORK_DOCUMENT`, `BILLING_TRANSFER_PROOF`, or `GENERAL`

The request must include a valid access token and the account email must be verified. The API validates file type and size, uploads to Cloudinary, saves an ownership-scoped `ManagedFile` record and returns `data.fileUrl`.

## Render deployment

1. Push this backend folder to its own GitHub repository.
2. In Render, create a Blueprint from that repository using `render.yaml`.
3. Supply the requested JWT, frontend, administrator, SMTP and Cloudinary values.
4. Deploy the frontend separately to Vercel.
5. Set `FRONTEND_URL` and `ALLOWED_ORIGINS` to the final Vercel URL.

Render injects the supported PostgreSQL `connectionString`, `user` and `password` properties from the database declared in `render.yaml`. The API converts Render's connection string into a JDBC URL at startup. JPA then creates or updates the tables automatically.

### Super administrator bootstrap

The first application start creates the super administrator and a normal portfolio for that account when these values are set:

```env
SUPER_ADMIN_EMAIL=samuelshola14@gmail.com
SUPER_ADMIN_USERNAME=samuel-shola-admin
SUPER_ADMIN_PASSWORD=your-private-bootstrap-password
```

`SUPER_ADMIN_PASSWORD` is deliberately not stored in this repository. Add your chosen bootstrap value as a Render secret, deploy once, then rotate the account password from the Security page. If the email already exists, the seeder does nothing.

`GITHUB_TOKEN` is optional but recommended because it gives the public repository integration a higher GitHub API rate limit.

### Brevo SMTP variables

Set these Render environment variables so verification, password-reset and announcement emails are delivered:

```env
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
MAIL_USERNAME=your-brevo-smtp-login
MAIL_PASSWORD=your-brevo-smtp-key
MAIL_FROM=your-verified-brevo-sender-address
```

`MAIL_HOST` now defaults to Brevo's SMTP relay, preventing application startup from failing when only that variable is omitted. Username, password and sender address must still be configured with valid Brevo values for delivery.

## Main API groups

- `/api/v1/auth/public/**` — registration, login, verification and password recovery
- `/api/v1/auth/private/**` — current user, password change and 2FA
- `/api/v1/portfolios/public/**` — published portfolios
- `/api/v1/portfolios/public/{username}/qr` — portfolio QR image
- `/api/v1/portfolios/public/{username}/export` — downloadable portfolio PDF
- `/api/v1/portfolios/private/**` — portfolio management
- `/api/v1/works/private/**` — work management
- `/api/v1/profile-content/private/**` — experience, education, skills and social links
- `/api/v1/analytics/**` — recruiter activity capture and owner analytics
- `/api/v1/enquiries/**` — recruiter contact and private inbox
- `/api/v1/github/public/**` — public GitHub repositories
- `/api/v1/admin/private/**` — super-administrator management
- `/api/v1/admin/private/portfolio-setup/**` — secure account creation and deterministic Excel portfolio import
- `/api/v1/setup-requests/public` — public assisted-setup requests using email and an active WhatsApp number
- `/api/v1/setup-requests/admin/private/**` — super-administrator assisted-setup queue
- `/api/v1/utilities/private/file/upload` — authenticated Cloudinary upload
- `/api/v1/subscriptions/public/plans` — public portfolio and business pricing plans
- `/api/v1/subscriptions/private/**` — the current user's workspace plans, entitlements and usage
- `/api/v1/admin/subscriptions/**` — super-administrator plan and workspace subscription controls
- `/api/v1/billing/private/**` — checkout, verification, transfer proof and payment history
- `/api/v1/billing/public/paystack/webhook` — signed Paystack payment notifications
- `/api/v1/admin/billing/**` — super-administrator payment review

## Workspace subscriptions

Subscriptions belong to individual `PORTFOLIO` or `BUSINESS` workspaces, so one account may own several independently billed workspaces. Startup creates permanent `FREE_PORTFOLIO` and `FREE_BUSINESS` plans and safely assigns them to existing workspaces. The first free workspace of each type is allowed; additional workspaces require a paid plan.

Plan entitlements control pages, sections, products, music tracks, team members, storage, video backgrounds, custom domains, animations, branding, cart orders and WhatsApp orders. Use `-1` for an unlimited numeric entitlement. Paid workspace creation initially uses `PENDING_PAYMENT` until payment is confirmed.

## Paystack and bank-transfer billing

Users can pay for or renew a paid portfolio or business plan from the Billing screen. A successful Paystack verification activates the selected plan immediately. A bank transfer remains pending until a Super Admin checks the uploaded receipt and approves it. Rejected transfers never change the subscription.

Configure these values in the deployment environment:

```env
PAYSTACK_SECRET_KEY=sk_live_or_test_key
PAYSTACK_PORTFOLIO_CALLBACK_URL=https://your-portfolio-app.example/billing/callback
PAYSTACK_BUSINESS_CALLBACK_URL=https://your-business-app.example/billing/callback
BILLING_BANK_NAME=Your bank
BILLING_BANK_ACCOUNT_NAME=Your account name
BILLING_BANK_ACCOUNT_NUMBER=0123456789
BILLING_BANK_INSTRUCTIONS=Use the displayed payment reference as the narration.
```

Set the Paystack webhook URL to `https://your-api.example/api/v1/billing/public/paystack/webhook`. The API verifies Paystack's signature, amount and currency before activating access. Repeated callbacks and webhook deliveries are locked and handled idempotently.

Each confirmed payment adds one month to the same active plan. A plan change begins a new one-month period immediately. Users may schedule cancellation for period end or resume before the expiry date. The hourly lifecycle task expires unpaid checkout references and ends subscriptions whose paid period has elapsed; no workspace content is deleted.

Business setup progress is stored on the business record as `BASICS`, `BRAND`, `CONTACT`, `CONTENT`, `CATALOG`, `PREVIEW` or `COMPLETE`. Each saved action advances progress without moving it backwards, allowing an owner to sign in on another device and resume from the next stage.

The startup seed includes permanent Free Business plus monthly NGN Starter (₦10,000), Growth (₦20,000), Professional (₦30,000), Scale (₦40,000) and Premium (₦50,000) plans. Plan creation is idempotent and includes future email-template and monthly-email allowances.

## Assisted portfolio setup without AI

The frontend provides a structured `.xlsx` workbook with Account, Profile, Skills, Background, Projects and Social Links sheets. The backend reads the fixed columns with Apache POI, validates the workbook, returns a preview and imports only after the administrator confirms a mode:

- `FILL_EMPTY` preserves existing values and skips matching portfolio entries.
- `MERGE` updates matching entries and keeps information not present in the workbook.
- `REPLACE_SECTIONS` replaces imported skills, background, projects and social links.

New admin-created accounts receive separate password-setup and email-verification links. Passwords are never shown to administrators.
