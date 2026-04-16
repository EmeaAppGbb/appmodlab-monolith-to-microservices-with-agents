// =============================================================================
// EduVerse Academy — Main Azure Deployment Template
// Orchestrates the full microservices infrastructure on Azure
//
// Usage:
//   az deployment group create \
//     --resource-group rg-eduverse \
//     --template-file deployment/azure/main.bicep \
//     --parameters environmentName=dev dbAdminLogin=eduadmin dbAdminPassword=<secret>
// =============================================================================

targetScope = 'resourceGroup'

// ── Parameters ─────────────────────────────────────────────────────────────────

@description('Environment name (dev, staging, prod)')
@allowed(['dev', 'staging', 'prod'])
param environmentName string = 'dev'

@description('Location for all resources')
param location string = resourceGroup().location

@description('PostgreSQL administrator login')
@secure()
param dbAdminLogin string

@description('PostgreSQL administrator password')
@secure()
param dbAdminPassword string

@description('Container image tag')
param imageTag string = 'latest'

// ── Variables ──────────────────────────────────────────────────────────────────

var prefix = 'eduverse-${environmentName}'
var tags = {
  project: 'eduverse-academy'
  environment: environmentName
  managedBy: 'bicep'
}

var services = [
  { name: 'notification',    port: 8084, dbName: 'notifications' }
  { name: 'course-catalog',  port: 8085, dbName: 'coursecatalog' }
  { name: 'payment',         port: 8086, dbName: 'payments' }
  { name: 'video',           port: 8087, dbName: 'videos' }
  { name: 'certificate',     port: 8088, dbName: 'certificates' }
  { name: 'assessment',      port: 8089, dbName: 'assessments' }
  { name: 'progress',        port: 8091, dbName: 'progress' }
  { name: 'enrollment',      port: 8092, dbName: 'enrollments' }
]

// ── Azure Container Registry ───────────────────────────────────────────────────

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: replace('${prefix}acr', '-', '')
  location: location
  tags: tags
  sku: {
    name: 'Standard'
  }
  properties: {
    adminUserEnabled: false
  }
}

// ── User-Assigned Managed Identity (for ACR pull) ──────────────────────────────

resource acrPullIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: '${prefix}-acr-pull-identity'
  location: location
  tags: tags
}

// Assign AcrPull role to the managed identity
resource acrPullRoleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(acr.id, acrPullIdentity.id, 'AcrPull')
  scope: acr
  properties: {
    principalId: acrPullIdentity.properties.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '7f951dda-4ed3-4680-a7ca-43fe172d538d') // AcrPull
  }
}

// ── Log Analytics Workspace ────────────────────────────────────────────────────

resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: '${prefix}-logs'
  location: location
  tags: tags
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
  }
}

// ── Container Apps Environment ─────────────────────────────────────────────────

resource containerAppsEnv 'Microsoft.App/managedEnvironments@2023-11-02-preview' = {
  name: '${prefix}-env'
  location: location
  tags: tags
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalytics.properties.customerId
        sharedKey: logAnalytics.listKeys().primarySharedKey
      }
    }
    zoneRedundant: environmentName == 'prod'
  }
}

// ── Service Bus ────────────────────────────────────────────────────────────────

module serviceBus 'modules/service-bus.bicep' = {
  name: 'serviceBus'
  params: {
    namespaceName: '${prefix}-servicebus'
    location: location
    skuName: environmentName == 'prod' ? 'Premium' : 'Standard'
    tags: tags
  }
}

// ── PostgreSQL Server (shared for non-prod, dedicated per service in prod) ─────

module database 'modules/sql-database.bicep' = {
  name: 'database'
  params: {
    serverName: '${prefix}-pgserver'
    location: location
    adminLogin: dbAdminLogin
    adminPassword: dbAdminPassword
    postgresVersion: '16'
    tier: environmentName == 'prod' ? 'GeneralPurpose' : 'Burstable'
    skuName: environmentName == 'prod' ? 'Standard_D2ds_v4' : 'Standard_B1ms'
    storageSizeGB: environmentName == 'prod' ? 64 : 32
    databaseNames: [for svc in services: svc.dbName]
    tags: tags
  }
}

// ── API Gateway Container App ──────────────────────────────────────────────────

module apiGateway 'modules/container-app.bicep' = {
  name: 'api-gateway'
  params: {
    appName: '${prefix}-api-gateway'
    location: location
    environmentId: containerAppsEnv.id
    containerImage: '${acr.properties.loginServer}/eduverse/api-gateway:${imageTag}'
    containerPort: 8090
    acrLoginServer: acr.properties.loginServer
    identityId: acrPullIdentity.id
    externalIngress: true
    cpu: environmentName == 'prod' ? '1.0' : '0.5'
    memory: environmentName == 'prod' ? '2Gi' : '1Gi'
    minReplicas: environmentName == 'prod' ? 2 : 1
    maxReplicas: environmentName == 'prod' ? 10 : 3
    tags: tags
    envVars: [
      { name: 'COURSE_CATALOG_SERVICE_URL', value: 'https://${prefix}-course-catalog.internal.${containerAppsEnv.properties.defaultDomain}' }
      { name: 'ENROLLMENT_SERVICE_URL',     value: 'https://${prefix}-enrollment.internal.${containerAppsEnv.properties.defaultDomain}' }
      { name: 'ASSESSMENT_SERVICE_URL',     value: 'https://${prefix}-assessment.internal.${containerAppsEnv.properties.defaultDomain}' }
      { name: 'VIDEO_SERVICE_URL',          value: 'https://${prefix}-video.internal.${containerAppsEnv.properties.defaultDomain}' }
      { name: 'CERTIFICATE_SERVICE_URL',    value: 'https://${prefix}-certificate.internal.${containerAppsEnv.properties.defaultDomain}' }
      { name: 'PAYMENT_SERVICE_URL',        value: 'https://${prefix}-payment.internal.${containerAppsEnv.properties.defaultDomain}' }
      { name: 'NOTIFICATION_SERVICE_URL',   value: 'https://${prefix}-notification.internal.${containerAppsEnv.properties.defaultDomain}' }
      { name: 'PROGRESS_SERVICE_URL',       value: 'https://${prefix}-progress.internal.${containerAppsEnv.properties.defaultDomain}' }
      { name: 'MONOLITH_URL',              value: 'http://localhost:8080' } // Disabled in Azure — no monolith deployed
    ]
  }
  dependsOn: [acrPullRoleAssignment]
}

// ── Microservice Container Apps ────────────────────────────────────────────────

module microservices 'modules/container-app.bicep' = [
  for svc in services: {
    name: svc.name
    params: {
      appName: '${prefix}-${svc.name}'
      location: location
      environmentId: containerAppsEnv.id
      containerImage: '${acr.properties.loginServer}/eduverse/${svc.name}-service:${imageTag}'
      containerPort: svc.port
      acrLoginServer: acr.properties.loginServer
      identityId: acrPullIdentity.id
      externalIngress: false
      cpu: environmentName == 'prod' ? '0.5' : '0.25'
      memory: environmentName == 'prod' ? '1Gi' : '0.5Gi'
      minReplicas: environmentName == 'prod' ? 2 : 1
      maxReplicas: environmentName == 'prod' ? 5 : 3
      tags: tags
      secrets: [
        { name: 'db-password',                  value: dbAdminPassword }
        { name: 'servicebus-connection-string',  value: serviceBus.outputs.connectionString }
      ]
      envVars: [
        { name: 'SPRING_PROFILES_ACTIVE', value: 'azure' }
        { name: 'SERVER_PORT',            value: '${svc.port}' }
        { name: 'DB_URL',                 value: 'jdbc:postgresql://${database.outputs.fqdn}:5432/${svc.dbName}?sslmode=require' }
        { name: 'DB_USERNAME',            value: dbAdminLogin }
        { name: 'DB_PASSWORD',            secretRef: 'db-password' }
        { name: 'SERVICEBUS_CONNECTION_STRING', secretRef: 'servicebus-connection-string' }
      ]
    }
    dependsOn: [acrPullRoleAssignment, database, serviceBus]
  }
]

// ── API Management (optional, for prod-grade API governance) ───────────────────

resource apim 'Microsoft.ApiManagement/service@2023-05-01-preview' = if (environmentName == 'prod') {
  name: '${prefix}-apim'
  location: location
  tags: tags
  sku: {
    name: 'Consumption'
    capacity: 0
  }
  properties: {
    publisherEmail: 'platform@eduverse.academy'
    publisherName: 'EduVerse Academy'
  }
}

// ── Outputs ────────────────────────────────────────────────────────────────────

@description('Container Registry login server')
output acrLoginServer string = acr.properties.loginServer

@description('API Gateway FQDN')
output apiGatewayFqdn string = apiGateway.outputs.fqdn

@description('Container Apps Environment default domain')
output environmentDomain string = containerAppsEnv.properties.defaultDomain

@description('PostgreSQL server FQDN')
output databaseFqdn string = database.outputs.fqdn

@description('Service Bus namespace name')
output serviceBusNamespace string = serviceBus.outputs.namespaceName

@description('API Management gateway URL (prod only)')
output apimGatewayUrl string = environmentName == 'prod' ? apim.properties.gatewayUrl : 'N/A - APIM not deployed'
