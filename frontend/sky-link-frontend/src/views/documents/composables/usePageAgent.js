import { computed, ref } from 'vue'

const PAGE_AGENT_BASE_URL = 'https://page-ag-testing-ohftxirgbn.cn-shanghai.fcapp.run'
const PAGE_AGENT_MODEL = 'qwen3.5-plus'
const PAGE_AGENT_API_KEY = 'NA'

export function usePageAgent() {
  const agent = ref(null)
  const open = ref(false)

  async function createAgent() {
    if (agent.value) return agent.value

    const { PageAgent } = await import('page-agent')
    agent.value = new PageAgent({
      model: PAGE_AGENT_MODEL,
      baseURL: PAGE_AGENT_BASE_URL,
      apiKey: PAGE_AGENT_API_KEY,
      language: 'zh-CN',
    })
    return agent.value
  }

  async function show() {
    const instance = await createAgent()
    instance.panel.show()
    open.value = true
  }

  function close() {
    if (!agent.value) return
    agent.value.panel.dispose()
    agent.value = null
    open.value = false
  }

  return {
    isOpen: computed(() => open.value),
    show,
    close,
  }
}
