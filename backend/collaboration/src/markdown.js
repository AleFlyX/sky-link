import { getSchema } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import { marked } from 'marked'
import { prosemirrorJSONToYDoc, yDocToProsemirrorJSON } from 'y-prosemirror'

export const schema = getSchema([StarterKit])

function inline(tokens = [], marks = []) {
  return tokens.flatMap((token) => {
    if (token.type === 'strong') return inline(token.tokens, [...marks, { type: 'bold' }])
    if (token.type === 'em') return inline(token.tokens, [...marks, { type: 'italic' }])
    if (token.type === 'codespan') return [{ type: 'text', text: token.text, marks: [...marks, { type: 'code' }] }]
    if (token.type === 'link') return inline(token.tokens, [...marks, { type: 'link', attrs: { href: token.href, target: null, rel: 'noopener noreferrer nofollow', class: null } }])
    if (token.type === 'br') return [{ type: 'hardBreak' }]
    const text = token.text ?? token.raw ?? ''
    return text ? [{ type: 'text', text, ...(marks.length ? { marks } : {}) }] : []
  })
}

function blocks(tokens = []) {
  return tokens.flatMap((token) => {
    if (token.type === 'space') return []
    if (token.type === 'heading') return [{ type: 'heading', attrs: { textAlign: null, level: token.depth }, content: inline(token.tokens) }]
    if (token.type === 'paragraph') return [{ type: 'paragraph', attrs: { textAlign: null }, content: inline(token.tokens) }]
    if (token.type === 'text') return [{ type: 'paragraph', attrs: { textAlign: null }, content: inline(token.tokens || [token]) }]
    if (token.type === 'code') return [{ type: 'codeBlock', attrs: { language: token.lang || null }, content: token.text ? [{ type: 'text', text: token.text }] : [] }]
    if (token.type === 'hr') return [{ type: 'horizontalRule' }]
    if (token.type === 'blockquote') return [{ type: 'blockquote', content: blocks(token.tokens) }]
    if (token.type === 'list') return [{ type: token.ordered ? 'orderedList' : 'bulletList', attrs: token.ordered ? { start: token.start || 1, type: null } : { type: null },
      content: token.items.map((item) => ({ type: 'listItem', content: blocks(item.tokens) })) }]
    return token.raw ? [{ type: 'paragraph', attrs: { textAlign: null }, content: [{ type: 'text', text: token.raw }] }] : []
  })
}

export function markdownToYDoc(markdown) {
  const json = { type: 'doc', content: blocks(marked.lexer(markdown || '')) }
  return prosemirrorJSONToYDoc(schema, json, 'default')
}

const escapeText = (text) => String(text || '').replace(/([*_`[\]])/g, '\\$1')
function serializeInline(nodes = []) {
  return nodes.map((node) => {
    if (node.type === 'hardBreak') return '  \n'
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

export function yDocToMarkdown(document) { return serializeBlocks(yDocToProsemirrorJSON(document, 'default').content).trim() }
