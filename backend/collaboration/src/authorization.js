/**
 * 向 Spring Boot 再次确认“当前用户现在是否仍能协作这篇文档”。
 *
 * WebSocket 建立后用户的部门、角色、文档权限可能被管理员修改，所以不能只依赖最初票据。
 *
 * @param {{ internalBaseUrl: string, serviceToken: string }} config BFF 运行配置。
 * @param {{ userId: number, documentId: number }} context 已验签的连接上下文。
 * @returns {Promise<{ allowed: boolean, permission?: 'read'|'edit'|'manage' }>} 后端实际授权结果。
 */
export async function reauthorize(config, context) {
  // 请求只在 BFF 与 Spring Boot 内部网络之间发送，不把 serviceToken 暴露给浏览器。
  const response = await fetch(`${config.internalBaseUrl}/internal/collaboration/authorize`, {
    // 授权检查是带参数的内部动作，因此使用 POST 且 JSON 传递用户/文档 ID。
    method: 'POST',
    headers: {
      // Spring 据此把请求体解析为 JSON。
      'content-type': 'application/json',
      // Spring 以服务令牌验证“调用者确实是协同 BFF”，不是任意外部客户端。
      'X-SkyLink-Service-Token': config.serviceToken,
    },
    // ID 取自先前已验签的 context，而不是从 WebSocket 客户端再次接收。
    body: JSON.stringify({ userId: context.userId, documentId: context.documentId }),
    // 最多等待 5 秒；授权服务卡住时不能无限占用 WebSocket 定时器。
    signal: AbortSignal.timeout(5000),
  })

  // 任何非 2xx 都不当作“允许”；调用方会按短暂不可用策略处理该异常。
  if (!response.ok) {
    throw new Error(`authorization service returned ${response.status}`)
  }

  // Spring 的接口使用统一 { code, message, data } 外层，这里只取真正的授权 data。
  const payload = await response.json()
  return payload?.data ?? payload
}
