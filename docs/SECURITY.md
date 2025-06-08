# Security Configuration Guide

## Overview

This document outlines the security configuration setup for the Certification Report System to prevent secrets from being committed to version control.

## Configuration Strategy

### 1. Environment-Based Configuration

The application uses Spring Boot profiles and environment variables to manage sensitive configuration:

- **Development**: Use `application-secure.properties` with environment variable overrides
- **Production**: Use `application-prod.properties` (created from template) with mandatory environment variables
- **Docker**: Use `.env` file (created from template) for container configuration

### 2. File Structure

```
backend/src/main/resources/
├── application.properties              # Base configuration with environment variable placeholders
├── application-secure.properties       # Secure development configuration
├── application-prod.properties.template # Production configuration template
└── application-prod.properties         # GITIGNORED - Created from template for production

docker/
├── .env.template                       # Environment template
├── .env                               # GITIGNORED - Created from template
└── docker-compose.yml                 # Uses environment variables
```

## Setup Instructions

### Development Environment

1. **Backend Configuration**:
   - The current `application.properties` uses environment variable placeholders
   - Default development values are provided as fallbacks
   - Override with environment variables for custom setups

2. **Docker Configuration**:
   ```bash
   cd docker
   copy .env.template .env
   # Edit .env with your local development credentials
   ```

### Production Environment

1. **Create Production Configuration**:
   ```bash
   cd backend/src/main/resources
   copy application-prod.properties.template application-prod.properties
   # Edit application-prod.properties with production values
   ```

2. **Set Environment Variables** (EXAMPLE VALUES - REPLACE WITH ACTUAL):
   ```bash
   set DB_URL=jdbc:postgresql://prod-server:5432/certreport_prod
   set DB_USERNAME=prod_user
   set DB_PASSWORD=YOUR_SECURE_PRODUCTION_PASSWORD_HERE
   set SSL_ENABLED=true
   set SSL_KEYSTORE_PATH=/path/to/keystore.p12
   set SSL_KEYSTORE_PASSWORD=YOUR_KEYSTORE_PASSWORD_HERE
   ```

3. **Run with Production Profile**:
   ```bash
   java -jar app.jar --spring.profiles.active=prod
   ```

## Security Best Practices

### 1. Never Commit Secrets

The following files are gitignored and should NEVER be committed:
- `application-prod.properties`
- `docker/.env`
- Any files in `secrets/` directory
- SSL certificates and keystores
- Any file containing actual passwords or API keys

### 2. Environment Variables

Use environment variables for all sensitive configuration:
```properties
# Good - uses environment variables with safe defaults
spring.datasource.password=${DB_PASSWORD:safe_dev_default}

# Bad - hardcoded secrets (EXAMPLE ONLY - DO NOT USE)
spring.datasource.password=NEVER_HARDCODE_PASSWORDS
```

### 3. Configuration Templates

Always provide `.template` files showing the required configuration structure without actual secrets.

### 4. Docker Secrets

For Docker production deployments, consider using Docker secrets:
```yaml
services:
  app:
    secrets:
      - db_password
secrets:
  db_password:
    external: true
```

## Current Security Status

✅ **Secured**:
- Database credentials use environment variables
- Production configuration template provided
- Docker configuration uses environment variables
- Comprehensive gitignore patterns

✅ **Gitignored**:
- All environment files (`.env*`)
- All profile-specific properties files
- SSL certificates and keystores
- Generated reports and temporary files

## Verification

To verify no secrets are committed:

1. **Check gitignore coverage**:
   ```bash
   git status --ignored
   ```

2. **Scan for potential secrets**:
   ```bash
   git log --all --full-history -- "*.properties" | grep -i password
   ```

3. **Verify environment variable usage**:
   ```bash
   grep -r "password=" backend/src/main/resources/application.properties
   # Should show: spring.datasource.password=${DB_PASSWORD:certpass}
   ```

## Emergency Response

If secrets were accidentally committed:

1. **Immediate Actions**:
   - Change all exposed credentials immediately
   - Revoke any exposed API keys or certificates
   - Update all systems using the compromised credentials

2. **Git History Cleanup**:
   ```bash
   # Remove sensitive file from all history
   git filter-branch --force --index-filter "git rm --cached --ignore-unmatch path/to/secret/file" --prune-empty --tag-name-filter cat -- --all
   
   # Force push to all remotes
   git push origin --force --all
   git push origin --force --tags
   ```

3. **Prevention**:
   - Review and update gitignore patterns
   - Implement pre-commit hooks to scan for secrets
   - Train team on security best practices

## Contact

For security-related questions or incidents, contact the development team immediately.
