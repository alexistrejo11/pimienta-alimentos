#!/usr/bin/env bash
set -euo pipefail
BUCKET="${AWS_S3_BUCKET_NAME:-pimienta-employees}"
awslocal s3 mb "s3://${BUCKET}" || true
awslocal s3api put-bucket-cors --bucket "${BUCKET}" --cors-configuration '{
  "CORSRules": [{
    "AllowedOrigins": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST", "HEAD", "DELETE"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag"]
  }]
}' || true
