// =============================================================================
// EduVerse Academy — Azure Service Bus Module
// Deploys a Service Bus namespace with topics and subscriptions for
// event-driven communication between microservices
// =============================================================================

@description('Name of the Service Bus namespace')
param namespaceName string

@description('Location for resources')
param location string = resourceGroup().location

@description('SKU for Service Bus')
@allowed(['Basic', 'Standard', 'Premium'])
param skuName string = 'Standard'

@description('Tags for the resource')
param tags object = {}

// ── Service Bus Namespace ──────────────────────────────────────────────────────
resource serviceBusNamespace 'Microsoft.ServiceBus/namespaces@2022-10-01-preview' = {
  name: namespaceName
  location: location
  tags: tags
  sku: {
    name: skuName
    tier: skuName
  }
  properties: {
    minimumTlsVersion: '1.2'
    disableLocalAuth: false
  }
}

// ── Topics ─────────────────────────────────────────────────────────────────────
// Event topics matching the domain events in shared-events library

var topics = [
  'student-enrolled'
  'enrollment-activated'
  'enrollment-completed'
  'payment-completed'
  'payment-failed'
  'assessment-passed'
  'progress-updated'
  'certificate-issued'
]

resource serviceBusTopics 'Microsoft.ServiceBus/namespaces/topics@2022-10-01-preview' = [
  for topic in topics: {
    parent: serviceBusNamespace
    name: topic
    properties: {
      defaultMessageTimeToLive: 'P14D'
      maxSizeInMegabytes: 1024
      enablePartitioning: false
      supportOrdering: true
    }
  }
]

// ── Subscriptions ──────────────────────────────────────────────────────────────
// Each service subscribes to the topics it consumes

// payment-service subscribes to student-enrolled
resource subPaymentStudentEnrolled 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: serviceBusTopics[0] // student-enrolled
  name: 'payment-service'
  properties: {
    deadLetteringOnMessageExpiration: true
    defaultMessageTimeToLive: 'P14D'
    lockDuration: 'PT1M'
    maxDeliveryCount: 10
  }
}

// notification-service subscribes to student-enrolled
resource subNotificationStudentEnrolled 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: serviceBusTopics[0] // student-enrolled
  name: 'notification-service'
  properties: {
    deadLetteringOnMessageExpiration: true
    defaultMessageTimeToLive: 'P14D'
    lockDuration: 'PT1M'
    maxDeliveryCount: 10
  }
}

// progress-service subscribes to enrollment-activated
resource subProgressEnrollmentActivated 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: serviceBusTopics[1] // enrollment-activated
  name: 'progress-service'
  properties: {
    deadLetteringOnMessageExpiration: true
    defaultMessageTimeToLive: 'P14D'
    lockDuration: 'PT1M'
    maxDeliveryCount: 10
  }
}

// certificate-service subscribes to enrollment-completed
resource subCertificateEnrollmentCompleted 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: serviceBusTopics[2] // enrollment-completed
  name: 'certificate-service'
  properties: {
    deadLetteringOnMessageExpiration: true
    defaultMessageTimeToLive: 'P14D'
    lockDuration: 'PT1M'
    maxDeliveryCount: 10
  }
}

// enrollment-service subscribes to payment-completed
resource subEnrollmentPaymentCompleted 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: serviceBusTopics[3] // payment-completed
  name: 'enrollment-service'
  properties: {
    deadLetteringOnMessageExpiration: true
    defaultMessageTimeToLive: 'P14D'
    lockDuration: 'PT1M'
    maxDeliveryCount: 10
  }
}

// notification-service subscribes to payment-completed
resource subNotificationPaymentCompleted 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: serviceBusTopics[3] // payment-completed
  name: 'notification-service'
  properties: {
    deadLetteringOnMessageExpiration: true
    defaultMessageTimeToLive: 'P14D'
    lockDuration: 'PT1M'
    maxDeliveryCount: 10
  }
}

// enrollment-service subscribes to payment-failed
resource subEnrollmentPaymentFailed 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: serviceBusTopics[4] // payment-failed
  name: 'enrollment-service'
  properties: {
    deadLetteringOnMessageExpiration: true
    defaultMessageTimeToLive: 'P14D'
    lockDuration: 'PT1M'
    maxDeliveryCount: 10
  }
}

// progress-service subscribes to assessment-passed
resource subProgressAssessmentPassed 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: serviceBusTopics[5] // assessment-passed
  name: 'progress-service'
  properties: {
    deadLetteringOnMessageExpiration: true
    defaultMessageTimeToLive: 'P14D'
    lockDuration: 'PT1M'
    maxDeliveryCount: 10
  }
}

// enrollment-service subscribes to progress-updated
resource subEnrollmentProgressUpdated 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: serviceBusTopics[6] // progress-updated
  name: 'enrollment-service'
  properties: {
    deadLetteringOnMessageExpiration: true
    defaultMessageTimeToLive: 'P14D'
    lockDuration: 'PT1M'
    maxDeliveryCount: 10
  }
}

// enrollment-service subscribes to certificate-issued
resource subEnrollmentCertificateIssued 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: serviceBusTopics[7] // certificate-issued
  name: 'enrollment-service'
  properties: {
    deadLetteringOnMessageExpiration: true
    defaultMessageTimeToLive: 'P14D'
    lockDuration: 'PT1M'
    maxDeliveryCount: 10
  }
}

// notification-service subscribes to certificate-issued
resource subNotificationCertificateIssued 'Microsoft.ServiceBus/namespaces/topics/subscriptions@2022-10-01-preview' = {
  parent: serviceBusTopics[7] // certificate-issued
  name: 'notification-service'
  properties: {
    deadLetteringOnMessageExpiration: true
    defaultMessageTimeToLive: 'P14D'
    lockDuration: 'PT1M'
    maxDeliveryCount: 10
  }
}

// ── Authorization Rules ────────────────────────────────────────────────────────
resource sendListenRule 'Microsoft.ServiceBus/namespaces/AuthorizationRules@2022-10-01-preview' = {
  parent: serviceBusNamespace
  name: 'microservices-shared-access'
  properties: {
    rights: [
      'Send'
      'Listen'
    ]
  }
}

@description('The connection string for Service Bus')
@secure()
output connectionString string = listKeys(sendListenRule.id, sendListenRule.apiVersion).primaryConnectionString

@description('The namespace name')
output namespaceName string = serviceBusNamespace.name

@description('The resource ID')
output id string = serviceBusNamespace.id
