# Deploy to Azure App Service

The manual GitHub Actions workflow builds the existing Dockerfile, pushes the image to an existing Azure Container Registry (ACR), then configures and restarts an existing Linux App Service. It uses GitHub Actions OIDC through `azure/login`; no Azure client secret is stored in GitHub.

## GitHub configuration

Create a GitHub environment named `azure-production` in **Settings > Environments**. Add these repository or environment **secrets**:

| Secret | Value |
| --- | --- |
| `AZURE_CLIENT_ID` | Application (client) ID of the Microsoft Entra application/service principal used for deployment |
| `AZURE_TENANT_ID` | Directory (tenant) ID for that application |
| `AZURE_SUBSCRIPTION_ID` | Subscription ID containing the ACR and App Service |

Add these **variables** (environment variables are appropriate when scoped to `azure-production`):

| Variable | Value |
| --- | --- |
| `AZURE_RESOURCE_GROUP` | Existing resource group containing the App Service |
| `AZURE_WEBAPP_NAME` | Existing Linux App Service name |
| `AZURE_ACR_NAME` | Existing ACR resource name, without `.azurecr.io` |
| `AZURE_ACR_LOGIN_SERVER` | That registry's login server, normally `<registry-name>.azurecr.io` |

The registry must already exist in the subscription and be reachable by the App Service. The workflow does not create Azure resources.

## Configure the Azure federated credential

In Microsoft Entra ID, create an app registration for GitHub deployments, then add a **Federated credential** with these values:

- Federated credential scenario: **GitHub Actions deploying Azure resources**
- Organization: the GitHub account or organization that owns this repository
- Repository: this repository's exact name
- Entity type: **Environment**
- Environment: `azure-production`
- Issuer: `https://token.actions.githubusercontent.com`
- Audience: `api://AzureADTokenExchange`
- Subject: `repo:<GITHUB_OWNER>/<GITHUB_REPOSITORY>:environment:azure-production`

Replace the owner and repository placeholders with the exact, case-sensitive GitHub values. The subject must match the workflow's `environment: azure-production` setting. No client secret is needed. Create or identify the corresponding service principal and grant it `AcrPush` on the existing ACR and `Website Contributor` on the existing App Service (or its resource group). Use the narrowest scopes supported by your deployment policy.

## Configure App Service image pulls

The workflow authenticates to ACR with the federated deployment identity to push each commit-tagged image. The App Service needs its own pull access:

1. Enable a system-assigned managed identity on the existing App Service.
2. Grant that identity the `AcrPull` role on the existing ACR.
3. Enable managed-identity pulls for the App Service container:

```sh
az webapp config set \
  --name "$AZURE_WEBAPP_NAME" \
  --resource-group "$AZURE_RESOURCE_GROUP" \
  --generic-configurations '{"acrUseManagedIdentityCreds": true}'
```

Ensure the App Service is a Linux custom-container app and its container port is set to `8080` (for example, App Service app setting `WEBSITES_PORT=8080`). The Dockerfile uses Java 17 and exposes port 8080.

## Run the deployment

Open **Actions > Deploy to Azure App Service > Run workflow** and select the branch to build. The workflow uses Maven to package the application, builds the root Dockerfile, pushes `support-ticket-automation:<commit-sha>` to the configured ACR, and updates the configured App Service to that image.

The application stores H2 data under `/app/data` inside the container. That container filesystem is not durable across replacements. For persistent data, configure suitable durable storage or move to a managed database before relying on the deployment for retained tickets or multiple instances.
