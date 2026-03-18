# GCS Signed URLs Implementation

## Overview

This feature adds **V4-signed URL generation** for Google Cloud Storage files, so that users who receive GCS filenames also get a time-limited link to view the file directly (e.g. in-browser PDF viewing). The bucket remains private — no public ACLs are needed.

## What Changed

### New File

- `src/main/java/com/megacorp/humanresources/model/FileItem.java` — a record with `name` and `signedUrl` fields, returned to the front-end so users get both the filename and a clickable link.

### Modified Files

- **`FileStorageService.java`** — two new interface methods:
  - `generateSignedUrl(objectName, duration, timeUnit)` — generates a signed URL for a single object.
  - `listFilesWithSignedUrls(prefix, duration, timeUnit)` — lists files matching a prefix and returns each with a signed URL.

- **`FileStorageServiceImpl.java`** — implements the two new methods using `Storage.signUrl()` with V4 signatures (`SignUrlOption.withV4Signature()`). Includes a `getSigningCredentials()` helper that:
  - Uses ADC directly when it already has a private key (service account JSON key, Compute metadata).
  - Falls back to **IAM-based impersonation** (`ImpersonatedCredentials`) when running with user credentials (e.g. `gcloud auth application-default login`), signing via the `signBlob` API server-side.
  - The `listFilesWithSignedUrls` method is also exposed as a `@Tool` for AI agent use.

- **`FileStorageController.java`** — two new REST endpoints:
  - `GET /signed-url?fileName=...&durationMinutes=15` — returns a signed URL for a single file.
  - `GET /list-files-signed?prefix=...&durationMinutes=15` — returns a JSON array of `{ "name": "...", "signedUrl": "..." }` objects.
  - Both default to 15 minutes but accept an optional `durationMinutes` parameter.

- **`application.properties`** — new property:
  ```properties
  google.cloud.storage.signing.service-account=${SIGNING_SERVICE_ACCOUNT:}
  ```

- **`src/test/http/file-storage.http`** — added test requests for the new endpoints.

### No Dependency Changes

`google-cloud-storage` 2.55.0 was already in `pom.xml` and includes the `signUrl` capability.

## To Make It Work (from scratch, no service account)

The `SIGNING_SERVICE_ACCOUNT` environment variable expects a **GCP service account email** in the format:

```
<service-account-name>@<project-id>.iam.gserviceaccount.com
```

You can find existing service accounts in the [Google Cloud Console](https://console.cloud.google.com) under **IAM & Admin → Service Accounts**, or via the CLI:

```bash
gcloud iam service-accounts list
```

To check your current project ID and authenticated account:

```bash
gcloud config get-value project
gcloud config get-value account
```

If you don't already have a suitable service account, follow all the steps below.

### 1. Create a service account for URL signing

```bash
gcloud iam service-accounts create gcs-signer \
  --display-name="GCS URL Signer"
```

This creates `gcs-signer@<your-project-id>.iam.gserviceaccount.com`.

### 2. Grant your user the Service Account Token Creator role

Your Google user account must be allowed to impersonate the service account (find your email with `gcloud config get-value account`):

```bash
gcloud iam service-accounts add-iam-policy-binding \
  gcs-signer@<your-project-id>.iam.gserviceaccount.com \
  --member="user:<your-google-email>" \
  --role="roles/iam.serviceAccountTokenCreator"
```

### 3. Grant the service account Storage Object Viewer on the bucket

The service account whose identity is used for signing must be able to read the objects. Replace `<your-bucket>` with your actual bucket name (the value of `STORAGE_BUCKET_NAME`):

```bash
gcloud storage buckets add-iam-policy-binding gs://<your-bucket> \
  --member="serviceAccount:gcs-signer@<your-project-id>.iam.gserviceaccount.com" \
  --role="roles/storage.objectViewer"
```

### 4. Ensure the bucket uses Uniform Bucket-Level Access

This prevents conflicting permissions from legacy ACLs and keeps everything manageable via IAM. Verify in the Google Cloud Console under **Bucket details → Permissions → Access control**. It should say "Uniform". If it says "Fine-grained", switch it (note: this is a one-way change).

### 5. Set the signing service account environment variable

```bash
export SIGNING_SERVICE_ACCOUNT=gcs-signer@<your-project-id>.iam.gserviceaccount.com
```

This is only required when running locally with user credentials. In production (GCE/Cloud Run/GKE), the compute credentials already implement `ServiceAccountSigner` and impersonation is skipped automatically.

**Note:** If using `start-run.sh` or `start-debug.sh`, add this export to those scripts instead of running it manually.

### 6. Authenticate with Application Default Credentials (ADC) locally

```bash
gcloud auth application-default login
```

This is what the app uses to call the `signBlob` API on behalf of the service account.

### Summary

| # | Action | One-time? |
|---|--------|-----------|
| 1 | Create the service account | Yes |
| 2 | Grant yourself Token Creator role | Yes |
| 3 | Grant the SA `storage.objectViewer` on the bucket | Yes |
| 4 | Verify Uniform Bucket-Level Access | Yes |
| 5 | Set `SIGNING_SERVICE_ACCOUNT` env var | Each session (or add to script/profile) |
| 6 | Run `gcloud auth application-default login` | Occasionally (token refresh) |

Steps 1–4 are one-time GCP setup. Steps 5–6 are what you repeat for local dev sessions.
