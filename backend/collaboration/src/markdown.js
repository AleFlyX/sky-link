// Tiptap/ProseMirror schema 决定允许哪些富文本节点和标记。
import { getSchema } from '@tiptap/core'
// StarterKit 提供段落、标题、列表、代码块、粗体、斜体等基础节点。
import StarterKit from '@tiptap/starter-kit'
// marked 将 Markdown 拆成语法 token，供下方转换为 ProseMirror JSON。
import { marked } from 'marked'
// y-prosemirror 负责 ProseMirror JSON 与 Y.Doc 之间的双向转换。
import { prosemirrorJSONToYDoc, yDocToProsemirrorJSON } from 'y-prosemirror'

// 整个 BFF 使用同一个 schema；导出它方便测试或其他模块复用相同结构。
export const schema = getSchema([StarterKit])

// 将 Markdown 的行内 token（文字、粗体、链接等）转换成 ProseMirror inline 节点。
function inline(tokens = [], marks = []) {
  // flatMap 允许一个 Markdown token 展开为多个 ProseMirror 节点。
  return tokens.flatMap((token) => {
    // 粗体递归转换子 token，并把 bold 标记叠加到已有标记上。
    if (token.type === 'strong') return inline(token.tokens, [...marks, { type: 'bold' }])
    // 斜体的处理与粗体相同，只是标记类型改为 italic。
    if (token.type === 'em') return inline(token.tokens, [...marks, { type: 'italic' }])
    // 行内代码不再递归解析其内容，直接作为带 code 标记的文本。
    if (token.type === 'codespan') return [{ type: 'text', text: token.text, marks: [...marks, { type: 'code' }] }]
    // 链接保留 href，并限制 rel，避免协同编辑器生成不安全的链接打开方式。
    if (token.type === 'link') return inline(token.tokens, [...marks, { type: 'link', attrs: { href: token.href, target: null, rel: 'noopener noreferrer nofollow', class: null } }])
    // Markdown 换行对应 ProseMirror 的 hardBreak 节点。
    if (token.type === 'br') return [{ type: 'hardBreak' }]
    // 对普通文字和未专门支持的行内 token，优先取 text，缺失时退回 raw。
    const text = token.text ?? token.raw ?? ''
    // 空文本不生成节点；已有 marks 时才写入 marks 字段，保持 JSON 精简。
    return text ? [{ type: 'text', text, ...(marks.length ? { marks } : {}) }] : []
  })
}

// 将 Markdown 的块级 token（标题、段落、列表等）转换为 ProseMirror block 节点。
function blocks(tokens = []) {
  return tokens.flatMap((token) => {
    // marked 生成的纯空白 token 不需要进入编辑器文档。
    if (token.type === 'space') return []
    // heading.depth 是 Markdown 中 # 的层级。
    if (token.type === 'heading') return [{ type: 'heading', attrs: { textAlign: null, level: token.depth }, content: inline(token.tokens) }]
    // 普通段落直接复用行内转换。
    if (token.type === 'paragraph') return [{ type: 'paragraph', attrs: { textAlign: null }, content: inline(token.tokens) }]
    // 顶层纯 text 也包装为段落，满足 ProseMirror 文档结构。
    if (token.type === 'text') return [{ type: 'paragraph', attrs: { textAlign: null }, content: inline(token.tokens || [token]) }]
    // 围栏代码块保留语言和原始文字，不对代码内容再次解释 Markdown。
    if (token.type === 'code') return [{ type: 'codeBlock', attrs: { language: token.lang || null }, content: token.text ? [{ type: 'text', text: token.text }] : [] }]
    // 分割线映射为 ProseMirror horizontalRule。
    if (token.type === 'hr') return [{ type: 'horizontalRule' }]
    // 引用块的子内容继续递归走块级转换。
    if (token.type === 'blockquote') return [{ type: 'blockquote', content: blocks(token.tokens) }]
    // 有序/无序列表分别保留节点类型；每个 Markdown item 都成为 listItem。
    if (token.type === 'list') return [{ type: token.ordered ? 'orderedList' : 'bulletList', attrs: token.ordered ? { start: token.start || 1, type: null } : { type: null }, content: token.items.map((item) => ({ type: 'listItem', content: blocks(item.tokens) })) }]
    // 当前未特殊支持的块级语法不静默丢弃：有 raw 内容就降级为普通段落。
    return token.raw ? [{ type: 'paragraph', attrs: { textAlign: null }, content: [{ type: 'text', text: token.raw }] }] : []
  })
}

// 将普通文档表中的 Markdown 初始化为 Hocuspocus 需要的 Y.Doc。
export function markdownToYDoc(markdown) {
  // marked.lexer 负责词法分析；空值按空文档处理。
  const json = { type: 'doc', content: blocks(marked.lexer(markdown || '')) }
  // 'default' 必须与下方导出时使用的 fragment 名保持一致。
  return prosemirrorJSONToYDoc(schema, json, 'default')
}

// 序列化普通文字时转义 Markdown 特殊字符，防止正文被意外解释为新的格式。
const escapeText = (text) => String(text || '').replace(/([*_`[\]])/g, '\\$1')

// 将 ProseMirror 行内节点恢复为 Markdown 字符串。
function serializeInline(nodes = []) {
  return nodes.map((node) => {
    // hardBreak 使用 Markdown 的两个空格加换行表示。
    if (node.type === 'hardBreak') return '  \n'
    // 先转义纯文字，再按节点 marks 重新套上 Markdown 标记。
    let value = escapeText(node.text)
    for (const mark of node.marks || []) {
      if (mark.type === 'code') value = `\`${node.text}\``
      if (mark.type === 'bold') value = `**${value}**`
      if (mark.type === 'italic') value = `*${value}*`
      if (mark.type === 'link') value = `[${value}](${mark.attrs.href})`
    }
    return value
  }).join('')
}

// 将 ProseMirror 块节点恢复为 Markdown，indent 用于列表项内部的嵌套文本。
function serializeBlocks(nodes = [], indent = '') {
  return nodes.map((node) => {
    if (node.type === 'heading') return `${'#'.repeat(node.attrs.level)} ${serializeInline(node.content)}`
    if (node.type === 'paragraph') return `${indent}${serializeInline(node.content)}`
    if (node.type === 'codeBlock') return `\`\`\`${node.attrs?.language || ''}\n${node.content?.[0]?.text || ''}\n\`\`\``
    if (node.type === 'horizontalRule') return '---'
    if (node.type === 'blockquote') return serializeBlocks(node.content).split('\n').map((line) => `> ${line}`).join('\n')
    if (node.type === 'bulletList' || node.type === 'orderedList') return (node.content || []).map((item, index) => {
      const prefix = node.type === 'orderedList' ? `${(node.attrs?.start || 1) + index}. ` : '- '
      const body = serializeBlocks(item.content || [], indent).replace(/\n\n/g, '\n')
      return `${prefix}${body}`
    }).join('\n')
    return ''
  }).filter(Boolean).join('\n\n')
}

// 将协同服务内存中的 Y.Doc 导出为普通文档表可保存的干净 Markdown。
export function yDocToMarkdown(document) {
  return serializeBlocks(yDocToProsemirrorJSON(document, 'default').content).trim()
}
