const MESSAGE_RECALL_WINDOW_MS = 2 * 60 * 1000 // 消息撤回时间窗口，单位毫秒

/**
 * 将任意值转换为 Date 对象，如果无法转换则返回 null
 * @param {*} value
 * @returns {Date | null}
 */
export function toDate(value) {
  if (value instanceof Date) {
    return value
  }
  if (value === null || value === undefined || value === '') {
    return null
  }

  const raw = String(value)
  const directDate = new Date(raw)
  if (!Number.isNaN(directDate.getTime())) {
    return directDate
  }

  const timeOnlyMatch = raw.match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?$/)
  if (!timeOnlyMatch) {
    return null
  }

  const [, hourText, minuteText, secondText = '0'] = timeOnlyMatch
  const date = new Date()
  date.setHours(Number(hourText), Number(minuteText), Number(secondText), 0)
  return date
}

/**
 *  格式化消息时间为 "HH:mm" 格式，如果无法解析则返回原始值的字符串形式
 * @param {*} value
 * @returns
 */
export function formatMessageTime(value) {
  const date = toDate(value)
  if (!date) {
    return value ? String(value) : ''
  }

  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

/**
 *
 * @param {*} message
 * @returns
 */
export function normalizeMessage(message = {}) {
  const sendTime = message.sendTime ?? message.sentAt ?? message.sentAtLabel ?? null
  const recalled = Boolean(message.recalled ?? message.isRecalled)
  const resolvedSendTime = sendTime === '刚刚' ? new Date().toISOString() : sendTime

  return {
    id: message.messageId ?? message.id,
    senderId: message.senderId,
    senderName: message.senderName,
    receiverId: message.receiverId,
    groupId: message.groupId,
    messageType: message.messageType ?? 'text',
    content: recalled ? '消息已撤回' : message.content,
    recalled,
    sendTime: resolvedSendTime,
    sentAt: formatMessageTime(resolvedSendTime),
  }
}

export function normalizeMessageList(data) {
  const list = Array.isArray(data) ? data : (data?.records ?? [])
  return list.map(normalizeMessage)
}

export function normalizeSession(session = {}) {
  const lastMessageObject =
    session.lastMessage && typeof session.lastMessage === 'object'
      ? normalizeMessage(session.lastMessage)
      : null
  const rawLastMessage = typeof session.lastMessage === 'string' ? session.lastMessage : ''
  const rawLastTime = session.lastTime ?? lastMessageObject?.sendTime ?? ''

  return {
    id: session.id ?? getSessionId(session.sessionType, session.targetId),
    sessionType: session.sessionType,
    targetId: session.targetId,
    targetName: session.targetName,
    lastMessage: lastMessageObject?.content || rawLastMessage,
    lastTime: rawLastTime,
    lastTimeLabel: formatMessageTime(rawLastTime),
  }
}

export function normalizeSessionList(data) {
  const list = Array.isArray(data) ? data : (data?.records ?? [])
  return list.map(normalizeSession)
}

export function getSessionId(sessionType, targetId) {
  if (!sessionType || !targetId) {
    return ''
  }
  return `${sessionType}-${targetId}`
}

export function getSessionIdFromParams(params = {}) {
  if (params.groupId) {
    return getSessionId('group', params.groupId)
  }
  if (params.receiverId) {
    return getSessionId('single', params.receiverId)
  }
  return ''
}

export function buildRouteSession(query = {}) {
  const sessionType = String(query.type || '').trim()
  const targetId = Number(query.id)
  if (!['single', 'group'].includes(sessionType) || !Number.isFinite(targetId) || targetId <= 0) {
    return null
  }

  const targetName =
    typeof query.name === 'string' && query.name.trim()
      ? query.name.trim()
      : sessionType === 'group'
        ? `群聊#${targetId}`
        : `用户#${targetId}`

  return normalizeSession({
    id: getSessionId(sessionType, targetId),
    sessionType,
    targetId,
    targetName,
    lastMessage: '',
    lastTime: '',
  })
}

export function buildSessionParams(sessionOrParams, extraParams = {}) {
  if (typeof sessionOrParams === 'string') {
    const [sessionType, targetIdText] = sessionOrParams.split('-')
    const targetId = Number(targetIdText)
    if (sessionType === 'group' && Number.isFinite(targetId) && targetId > 0) {
      return { groupId: targetId, ...extraParams }
    }
    if (sessionType === 'single' && Number.isFinite(targetId) && targetId > 0) {
      return { receiverId: targetId, ...extraParams }
    }
    return { ...extraParams }
  }

  return {
    ...sessionOrParams,
    ...extraParams,
  }
}

export function getConversationKeyFromMessage(message, currentUserId) {
  if (!message) {
    return ''
  }
  if (message.groupId) {
    return getSessionId('group', message.groupId)
  }

  const otherUserId = message.senderId === currentUserId ? message.receiverId : message.senderId
  return otherUserId ? getSessionId('single', otherUserId) : ''
}

export function canRecallMessage(message, currentUserId) {
  if (!message || message.senderId !== currentUserId || message.recalled) {
    return false
  }

  const sendTime = toDate(message.sendTime ?? message.sentAt ?? message.sentAtLabel)
  if (!sendTime) {
    return true
  }

  return Date.now() - sendTime.getTime() <= MESSAGE_RECALL_WINDOW_MS
}

function compareByTimeDesc(left, right) {
  const leftTime = toDate(left.lastTime)?.getTime() ?? 0
  const rightTime = toDate(right.lastTime)?.getTime() ?? 0
  return rightTime - leftTime
}

function compareByTimeAsc(left, right) {
  const leftTime = toDate(left.sendTime)?.getTime() ?? 0
  const rightTime = toDate(right.sendTime)?.getTime() ?? 0
  return leftTime - rightTime
}

export function upsertSession(list = [], session) {
  const normalized = normalizeSession(session)
  if (!normalized.id) {
    return [...list]
  }

  const next = [...list]
  const index = next.findIndex((item) => item.id === normalized.id)
  if (index >= 0) {
    next[index] = {
      ...next[index],
      ...normalized,
    }
  } else {
    next.unshift(normalized)
  }

  return next.sort(compareByTimeDesc)
}

export function upsertMessage(list = [], message) {
  const normalized = normalizeMessage(message)
  if (!normalized.id) {
    return [...list]
  }

  const next = [...list]
  const index = next.findIndex((item) => item.id === normalized.id)
  if (index >= 0) {
    next[index] = {
      ...next[index],
      ...normalized,
    }
  } else {
    next.push(normalized)
  }

  return next.sort(compareByTimeAsc)
}
