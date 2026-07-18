import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

async function resolveDemoResult(promise) {
  await vi.advanceTimersByTimeAsync(120)
  return promise
}

describe('workspace friend session mock flow', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.stubEnv('VITE_DATA_SOURCE', 'mock')
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllEnvs()
  })

  it('removes single chat sessions after deleting the friend', async () => {
    const workspace = await import('../api/workspace')

    const initialSessions = await resolveDemoResult(workspace.getSessions())
    expect(initialSessions.data).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          sessionType: 'single',
          targetId: 1002,
        }),
      ]),
    )

    await resolveDemoResult(workspace.deleteFriend(1002))

    const sessionsAfterDelete = await resolveDemoResult(workspace.getSessions())
    expect(sessionsAfterDelete.data).not.toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          sessionType: 'single',
          targetId: 1002,
        }),
      ]),
    )
  })
})
