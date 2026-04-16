// =============================================================================
// EduVerse Academy — Azure SQL Database Module
// Deploys an Azure Database for PostgreSQL Flexible Server per microservice
// (Database-per-Service pattern)
// =============================================================================

@description('Name of the PostgreSQL Flexible Server')
param serverName string

@description('Location for resources')
param location string = resourceGroup().location

@description('Database names to create on this server')
param databaseNames array

@description('Administrator login username')
@secure()
param adminLogin string

@description('Administrator login password')
@secure()
param adminPassword string

@description('PostgreSQL version')
@allowed(['14', '15', '16'])
param postgresVersion string = '16'

@description('Compute tier')
@allowed(['Burstable', 'GeneralPurpose', 'MemoryOptimized'])
param tier string = 'Burstable'

@description('Compute SKU name')
param skuName string = 'Standard_B1ms'

@description('Storage size in GB')
param storageSizeGB int = 32

@description('Tags for the resource')
param tags object = {}

@description('Virtual Network subnet ID for private access (optional)')
param subnetId string = ''

@description('Private DNS Zone ID for PostgreSQL (optional)')
param privateDnsZoneId string = ''

// ── PostgreSQL Flexible Server ─────────────────────────────────────────────────
resource postgresServer 'Microsoft.DBforPostgreSQL/flexibleServers@2023-06-01-preview' = {
  name: serverName
  location: location
  tags: tags
  sku: {
    name: skuName
    tier: tier
  }
  properties: {
    version: postgresVersion
    administratorLogin: adminLogin
    administratorLoginPassword: adminPassword
    storage: {
      storageSizeGB: storageSizeGB
      autoGrow: 'Enabled'
    }
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: {
      mode: 'Disabled'
    }
    network: !empty(subnetId) ? {
      delegatedSubnetResourceId: subnetId
      privateDnsZoneArmResourceId: privateDnsZoneId
    } : {}
    authConfig: {
      activeDirectoryAuth: 'Disabled'
      passwordAuth: 'Enabled'
    }
  }
}

// ── Allow Azure Services Firewall Rule ─────────────────────────────────────────
resource firewallAllowAzure 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2023-06-01-preview' = if (empty(subnetId)) {
  parent: postgresServer
  name: 'AllowAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

// ── Databases ──────────────────────────────────────────────────────────────────
resource databases 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2023-06-01-preview' = [
  for dbName in databaseNames: {
    parent: postgresServer
    name: dbName
    properties: {
      charset: 'UTF8'
      collation: 'en_US.utf8'
    }
  }
]

// ── Server Parameters ──────────────────────────────────────────────────────────
resource logConnections 'Microsoft.DBforPostgreSQL/flexibleServers/configurations@2023-06-01-preview' = {
  parent: postgresServer
  name: 'log_connections'
  properties: {
    value: 'on'
    source: 'user-override'
  }
}

@description('The FQDN of the PostgreSQL server')
output fqdn string = postgresServer.properties.fullyQualifiedDomainName

@description('The server name')
output serverName string = postgresServer.name

@description('The resource ID')
output id string = postgresServer.id

@description('JDBC connection string template')
output jdbcTemplate string = 'jdbc:postgresql://${postgresServer.properties.fullyQualifiedDomainName}:5432/{dbName}?sslmode=require'
