import { describe, expect, it } from 'vitest'
import { markdownToYDoc, yDocToMarkdown } from '../src/markdown.js'

describe('Markdown compatibility boundary', () => {
  it('preserves supported document semantics through Y.Doc', () => {
    const markdown = '# Plan\n\n- **Build** editor\n- [Read](https://example.com) docs\n\n```js\nconst ready = true\n```'
    const output = yDocToMarkdown(markdownToYDoc(markdown))
    expect(output).toContain('# Plan')
    expect(output).toContain('**Build**')
    expect(output).toContain('[Read](https://example.com)')
    expect(output).toContain('```js')
  })
})
