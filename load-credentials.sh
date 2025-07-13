#!/bin/bash

# Load Environment Variables for Cloud Native Invoicing
# This script reads credentials from ~/.aws/credentials and sets environment variables

echo "🔐 Loading credentials from ~/.aws/credentials..."

# Check if credentials file exists
if [ ! -f ~/.aws/credentials ]; then
    echo "❌ Error: ~/.aws/credentials file not found!"
    echo "Please create it first with your AWS and Azure credentials."
    exit 1
fi

# Function to read value from AWS credentials file
read_credential() {
    local section=$1
    local key=$2
    awk -F'=' -v section="[$section]" -v key="$key" '
        $0 == section { found_section = 1; next }
        /^\[/ && found_section { found_section = 0 }
        found_section && $1 ~ "^"key"$" { 
            gsub(/^[ \t]+|[ \t]+$/, "", $2); 
            print $2 
        }
    ' ~/.aws/credentials
}

# Load AWS credentials
export AWS_ACCESS_KEY_ID=$(read_credential "default" "aws_access_key_id")
export AWS_SECRET_ACCESS_KEY=$(read_credential "default" "aws_secret_access_key")
export AWS_SESSION_TOKEN=$(read_credential "default" "aws_session_token")
export AWS_REGION=$(read_credential "default" "region")

# Load Azure credentials
export AZURE_TENANT_ID=$(read_credential "azure" "tenant_id")
export AZURE_CLIENT_ID=$(read_credential "azure" "client_id")
export AZURE_JWK_SET_URI=$(read_credential "azure" "jwk_set_uri")

# Load JWT Token
export AZURE_JWT_TOKEN=$(read_credential "tokens" "azure_jwt")

# Verify credentials were loaded
if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AZURE_TENANT_ID" ]; then
    echo "❌ Error: Failed to load credentials from ~/.aws/credentials"
    echo "Please check the file format and content."
    exit 1
fi

echo "✅ Credentials loaded successfully!"
echo "   • AWS Access Key: ${AWS_ACCESS_KEY_ID:0:10}..."
echo "   • Azure Tenant ID: $AZURE_TENANT_ID"
echo "   • Azure Client ID: $AZURE_CLIENT_ID"
echo ""
echo "🎯 You can now run:"
echo "   docker-compose up -d"
echo "   ./test_endpoints_updated.sh"
echo ""
