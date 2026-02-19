#!/bin/bash

# Start MCP Human Resources Server in RUN mode
# This script exports all required environment variables from README.md and starts the server

echo "Setting up environment variables..."

# Brave Search API Key
export BRAVE_API_KEY=${BRAVE_API_KEY:-""}

# Google Cloud Storage
export GEMINI_PROJECT_ID=${GEMINI_PROJECT_ID:-""}
export STORAGE_BUCKET_NAME=${STORAGE_BUCKET_NAME:-""}

# Gemini API Key for Nano Banana image generation
export GEMINI_API_KEY=${GEMINI_API_KEY:-""}

# LLM API Keys
export GROQ_API_KEY=${GROQ_API_KEY:-""}
export OPENROUTER_API_KEY=${OPENROUTER_API_KEY:-""}

# Gmail Configuration
export GMAIL_EMAIL_ADDRESS=${GMAIL_EMAIL_ADDRESS:-""}
export GMAIL_EMAIL_APP_PASSWORD=${GMAIL_EMAIL_APP_PASSWORD:-""}

echo "Environment variables set."
echo ""
echo "Starting MCP Human Resources Server in RUN mode..."
echo ""

# Run the Maven Spring Boot application
./mvnw spring-boot:run
