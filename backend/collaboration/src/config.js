const required = (name) => {
  const value = process.env[name]
  if (!value) throw new Error(`${name} is required`)
  return value
}

const requiredAny = (...names) => {
  const value = names.map((name) => process.env[name]).find(Boolean)
  if (!value) throw new Error(`${names.join(' or ')} is required`)
  return value
}

export function loadConfig() {
  const ticketSecret = requiredAny('SKYLINK_COLLABORATION_TICKET_SECRET', 'COLLABORATION_TICKET_SECRET')
  const serviceToken = requiredAny('SKYLINK_COLLABORATION_SERVICE_TOKEN', 'COLLABORATION_SERVICE_TOKEN')
  if (ticketSecret.length < 32 || serviceToken.length < 32) throw new Error('collaboration secrets must contain at least 32 characters')
  return {
    port: Number(process.env.COLLABORATION_PORT || 8180),
    host: process.env.COLLABORATION_HOST || '127.0.0.1',
    allowedOrigins: new Set(required('COLLABORATION_ALLOWED_ORIGINS').split(',').map((it) => it.trim())),
    ticketSecret,
    serviceToken,
    internalBaseUrl: process.env.SKYLINK_INTERNAL_BASE_URL || 'http://localhost:8080',
    database: {
      host: process.env.DB_HOST || 'localhost', port: Number(process.env.DB_PORT || 3306),
      database: process.env.DB_NAME || 'skylink', user: required('DB_USERNAME'), password: required('DB_PASSWORD'),
      connectionLimit: 5,
    },
  }
}
