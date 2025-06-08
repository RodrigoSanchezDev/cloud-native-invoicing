#!/bin/bash
set -euo pipefail

# Validate env vars
if [ -z "${S3_BUCKET:-}" ]; then
  echo "Error: S3_BUCKET not set"
  exit 1
fi
if [ -z "${AWS_REGION:-}" ]; then
  echo "Error: AWS_REGION not set"
  exit 1
fi
# Create prefix folder in S3
aws s3api put-object \
  --bucket "$S3_BUCKET" \
  --key "carpeta/" \
  --region "$AWS_REGION"

echo "Carpeta creada en el bucket S3: $S3_BUCKET/carpeta/"
