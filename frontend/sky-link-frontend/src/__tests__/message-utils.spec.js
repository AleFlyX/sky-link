import { describe, expect, it, vi } from 'vitest'

import {
  buildRouteSession,
  buildSessionParams,
  canRecallMessage,
  getConversationKeyFromMessage,
  getSessionIdFromParams,
  normalizeMessage,
  normalizeSession,
  upsertMessage,
  upsertSession,
} from '../utils/message'

describe('message utils', () => {
  it('normalizes message payloads from backend fields', () => {
    const message = normalizeMessage({
      messageId: 18,
      senderId: 1001,
      senderName: '陈雨桐',
      messageType: 'text',
      content: '你好',
      sendTime: '2026-07-15T10:30:00',
    })

    expect(message).toMatchObject({
      id: 18,
      senderId: 1001,
      senderName: '陈雨桐',
      content: '你好',
      messageType: 'text',
      sentAt: '10:30',
    })
  })

  it('normalizes sessions and infers the session id', () => {
    const session = normalizeSession({
      sessionType: 'single',
      targetId: 1002,
      targetName: '李明浩',
      lastMessage: {
        messageId: 9,
        senderId: 1002,
        content: '收到',
        sendTime: '2026-07-15T09:45:00',
      },
      lastTime: '2026-07-15T09:45:00',
    })

    expect(session).toMatchObject({
      id: 'single-1002',
      targetName: '李明浩',
      lastMessage: '收到',
      lastTimeLabel: '09:45',
    })
  })

  it('builds chat parameters from session ids and route queries', () => {
    expect(buildSessionParams('single-1002', { size: 20 })).toEqual({
      receiverId: 1002,
      size: 20,
    })
    expect(getSessionIdFromParams({ groupId: 701 })).toBe('group-701')

    expect(
      buildRouteSession({
        type: 'group',
        id: '701',
        name: 'Day 3 联调小组',
      }),
    ).toMatchObject({
      id: 'group-701',
      targetName: 'Day 3 联调小组',
      sessionType: 'group',
    })
  })

  it('computes the conversation key relative to the current user', () => {
    expect(
      getConversationKeyFromMessage(
        {
          senderId: 1001,
          receiverId: 1002,
        },
        1001,
      ),
    ).toBe('single-1002')

    expect(
      getConversationKeyFromMessage(
        {
          senderId: 1003,
          groupId: 701,
        },
        1001,
      ),
    ).toBe('group-701')
  })

  it('deduplicates realtime session and message updates', () => {
    const nextSessions = upsertSession(
      [
        {
          id: 'single-1002',
          sessionType: 'single',
          targetId: 1002,
          targetName: '李明浩',
          lastTime: '2026-07-15T09:00:00',
        },
      ],
      {
        sessionType: 'single',
        targetId: 1002,
        targetName: '李明浩',
        lastMessage: { content: '最新消息', sendTime: '2026-07-15T10:00:00' },
        lastTime: '2026-07-15T10:00:00',
      },
    )

    const nextMessages = upsertMessage(
      [{ id: 1, senderId: 1001, content: '旧消息', sendTime: '2026-07-15T09:00:00' }],
      {
        id: 1,
        senderId: 1001,
        content: '旧消息（已更新）',
        sendTime: '2026-07-15T09:00:00',
      },
    )

    expect(nextSessions).toHaveLength(1)
    expect(nextSessions[0].lastMessage).toBe('最新消息')
    expect(nextMessages).toHaveLength(1)
    expect(nextMessages[0].content).toBe('旧消息（已更新）')
  })

  it('honors the two-minute recall window', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-15T10:02:00'))

    expect(
      canRecallMessage(
        {
          senderId: 1001,
          recalled: false,
          sendTime: '2026-07-15T10:01:10',
        },
        1001,
      ),
    ).toBe(true)

    expect(
      canRecallMessage(
        {
          senderId: 1001,
          recalled: false,
          sendTime: '2026-07-15T09:58:30',
        },
        1001,
      ),
    ).toBe(false)

    vi.useRealTimers()
  })
})
