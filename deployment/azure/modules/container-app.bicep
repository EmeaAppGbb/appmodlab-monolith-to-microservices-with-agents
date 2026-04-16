// =============================================================================
// EduVerse Academy — Azure Container App Module
// Deploys a single Container App within a managed environment
// =============================================================================

@description('Name of the container app')
param appName string

@description('Location for resources')
param location string = resourceGroup().location

@description('Container Apps Environment ID')
param environmentId string

@description('Container image to deploy')
param containerImage string

@description('Container port')
param containerPort int

@description('Azure Container Registry login server')
param acrLoginServer string

@description('User-assigned managed identity ID for ACR pull')
param identityId string

@description('Environment variables for the container')
param envVars array = []

@description('CPU cores allocated to the container (e.g., 0.5)')
param cpu string = '0.5'

@description('Memory allocated to the container (e.g., 1Gi)')
param memory string = '1Gi'

@description('Minimum number of replicas')
param minReplicas int = 1

@description('Maximum number of replicas')
param maxReplicas int = 3

@description('Enable external ingress')
param externalIngress bool = false

@description('Tags for the resource')
param tags object = {}

@description('Secrets to inject into the container app (array of {name, value})')
param secrets array = []

resource containerApp 'Microsoft.App/containerApps@2023-11-02-preview' = {
  name: appName
  location: location
  tags: tags
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${identityId}': {}
    }
  }
  properties: {
    managedEnvironmentId: environmentId
    configuration: {
      activeRevisionsMode: 'Single'
      secrets: secrets
      ingress: {
        external: externalIngress
        targetPort: containerPort
        transport: 'auto'
        allowInsecure: false
        traffic: [
          {
            weight: 100
            latestRevision: true
          }
        ]
      }
      registries: [
        {
          server: acrLoginServer
          identity: identityId
        }
      ]
    }
    template: {
      containers: [
        {
          name: appName
          image: containerImage
          resources: {
            cpu: json(cpu)
            memory: memory
          }
          env: envVars
          probes: [
            {
              type: 'Liveness'
              httpGet: {
                path: '/actuator/health'
                port: containerPort
                scheme: 'HTTP'
              }
              initialDelaySeconds: 30
              periodSeconds: 15
              failureThreshold: 3
            }
            {
              type: 'Readiness'
              httpGet: {
                path: '/actuator/health'
                port: containerPort
                scheme: 'HTTP'
              }
              initialDelaySeconds: 15
              periodSeconds: 10
              failureThreshold: 3
            }
            {
              type: 'Startup'
              httpGet: {
                path: '/actuator/health'
                port: containerPort
                scheme: 'HTTP'
              }
              initialDelaySeconds: 10
              periodSeconds: 10
              failureThreshold: 30
            }
          ]
        }
      ]
      scale: {
        minReplicas: minReplicas
        maxReplicas: maxReplicas
        rules: [
          {
            name: 'http-scaling'
            http: {
              metadata: {
                concurrentRequests: '50'
              }
            }
          }
        ]
      }
    }
  }
}

@description('The FQDN of the container app')
output fqdn string = containerApp.properties.configuration.ingress.fqdn

@description('The resource ID of the container app')
output id string = containerApp.id

@description('The name of the container app')
output name string = containerApp.name
