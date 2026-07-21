// 读取必填环境变量；缺少时在服务启动阶段立刻失败，而不是等第一次协同请求才暴露错误。
const required = (name) => {
  // process.env 中所有值都是字符串，未设置时为 undefined。
  const value = process.env[name]
  // 空字符串也不能作为密码、服务令牌或数据库配置使用。
  if (!value) throw new Error(`${name} is required`)
  // 返回原始字符串；密钥绝不打印到日志或响应中。
  return value
}

/**
 * 将运行环境变量整理成协同 BFF 所需的配置对象。
 *
 * @returns {{ port: number, host: string, allowedOrigins: Set<string>, ticketSecret: string, serviceToken: string, internalBaseUrl: string, database: object }}
 */
export function loadConfig() {
  // 此密钥用于验证短期协同票据，必须与 Spring Boot 签发票据时使用的密钥一致。
  const ticketSecret = required('SKYLINK_COLLABORATION_TICKET_SECRET')
  // 此令牌仅用于 BFF -> Spring 的内部二次授权接口，区别于终端用户的 JWT。
  const serviceToken = required('SKYLINK_COLLABORATION_SERVICE_TOKEN')
  // 过短的共享密钥容易被暴力猜测，因此启动前就强制最小长度。
  if (ticketSecret.length < 32 || serviceToken.length < 32) {
    throw new Error('collaboration secrets must contain at least 32 characters')
  }

  return {
    // 对外监听端口；本地默认 8180。
    port: Number(process.env.COLLABORATION_PORT || 8180),
    // 默认只监听本机，部署时可显式配置反向代理需要的地址。
    host: process.env.COLLABORATION_HOST || '127.0.0.1',
    // 浏览器 Origin 白名单，逗号分隔并去除每项两端空格。
    allowedOrigins: new Set(required('COLLABORATION_ALLOWED_ORIGINS').split(',').map((item) => item.trim())),
    // 仅在内存中交给 verifyTicket 使用，不应发送给浏览器。
    ticketSecret,
    // 仅放在 BFF 发往内部授权接口的请求头中，不应发送给浏览器。
    serviceToken,
    // BFF 访问 Spring Boot 的内网地址；生产环境通常不是浏览器可见的公网地址。
    internalBaseUrl: process.env.SKYLINK_INTERNAL_BASE_URL || 'http://localhost:8080',
    database: {
      // 以下数据库信息只供 mysql2 连接池使用。
      host: process.env.DB_HOST || 'localhost',
      port: Number(process.env.DB_PORT || 3306),
      database: required('DB_NAME'),
      user: required('DB_USERNAME'),
      password: required('DB_PASSWORD'),
      // 协同服务自身并发量受限，连接池保持较小以避免抢占主后端连接。
      connectionLimit: 5,
    },
  }
}
