# Azure Deployment Plan — PS04



## 1. Overview



This project implements a support-ticket automation REST API using Java 17 and Spring Boot. It classifies incoming tickets, suggests priority and an assigned team, generates a summary and draft response, and requires human review before approval.



## 2. Proposed Architecture



- **Azure App Service:** Hosts the Spring Boot application as a Linux custom container.

- **Azure Container Registry (ACR):** Stores the versioned Docker image.

- **GitHub Actions:** Builds and tests the application, then deploys the image through a manually triggered workflow.

- **Microsoft Entra ID / GitHub OIDC:** Authenticates the deployment workflow without storing an Azure client secret.

- **Application logs:** App Service logs are used for troubleshooting and deployment verification.



## 3. Deployment Workflow



1. Push reviewed source code to the `master` branch.

2. GitHub Actions runs the automated test suite.

3. Manually trigger the Azure deployment workflow when the build is green.

4. The workflow authenticates to Azure using OpenID Connect.

5. Build the Docker image and push it to ACR.

6. Configure App Service to use the new image and restart the application.

7. Verify that the health endpoint or `GET /api/tickets` responds successfully.



## 4. Required Configuration



Configure these GitHub repository settings without committing credentials:



**Repository secrets**

- `AZURE_CLIENT_ID`

- `AZURE_TENANT_ID`

- `AZURE_SUBSCRIPTION_ID`



**Repository variables**



Set the names expected by `.github/workflows/deploy-azure.yml` for the Azure resource group, container registry, and App Service. Check the workflow file for their exact names.



Configure a federated identity credential in Microsoft Entra ID for the GitHub repository and deployment workflow. Grant the identity only the Azure permissions needed to publish images and update the target App Service.



## 5. Persistence and Limitations



The application currently uses file-based H2 storage. A container's writable filesystem is not a suitable durable database location for production deployments. Replacing or recreating a container can risk losing ticket data unless persistent storage is configured.



For a production deployment, use Azure Database for PostgreSQL or another supported managed database, configure credentials through App Service settings, and verify database connectivity before deployment.



## 6. Security and Operational Checks



- Keep secrets and credentials out of Git.

- Use GitHub OIDC instead of a long-lived Azure client secret.

- Restrict deployment permissions and protect the deployment branch.

- Review application and platform logs after deployment.

- Verify ticket creation, duplicate-ID rejection, invalid-input handling, and human approval.

- Do not enable real email sending without an explicit requirement and appropriate authorization.



## 7. Deployment Status



The Docker image and GitHub Actions workflow are available. The Azure workflow is manually triggered. A live Azure deployment is optional for this assignment and should only be marked complete after it has actually been deployed and verified.


