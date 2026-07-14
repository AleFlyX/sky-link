export async function reauthorize(config, context) {
  const response = await fetch(`${config.internalBaseUrl}/internal/collaboration/authorize`, {
    method: 'POST', headers: { 'content-type': 'application/json', 'X-SkyLink-Service-Token': config.serviceToken },
    body: JSON.stringify({ userId: context.userId, documentId: context.documentId }), signal: AbortSignal.timeout(5000),
  })
  if (!response.ok) throw new Error(`authorization service returned ${response.status}`)
  const payload = await response.json()
  return payload?.data ?? payload
}
