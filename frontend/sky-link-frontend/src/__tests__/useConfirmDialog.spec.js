import { nextTick } from 'vue'
import { afterEach, describe, expect, it } from 'vitest'

import { confirmDialog } from '../composables/useConfirmDialog'

afterEach(() => {
  document.body.innerHTML = ''
})

function getButtons() {
  return Array.from(document.querySelectorAll('button'))
}

describe('confirmDialog', () => {
  it('resolves after confirming and removes the dialog', async () => {
    const promise = confirmDialog('确定要删除这条数据吗？', '删除数据', {
      confirmText: '删除',
      confirmVariant: 'danger',
    })

    await nextTick()

    expect(document.body.textContent).toContain('删除数据')

    const confirmButton = getButtons().find((button) => button.textContent?.includes('删除'))
    expect(confirmButton).toBeTruthy()
    confirmButton.click()

    await expect(promise).resolves.toBe('confirm')
    expect(document.body.textContent).not.toContain('删除数据')
  })

  it('rejects after canceling and removes the dialog', async () => {
    const promise = confirmDialog('确认要继续吗？', '继续操作')

    await nextTick()

    expect(document.body.textContent).toContain('继续操作')

    const cancelButton = getButtons().find((button) => button.textContent?.includes('取消'))
    expect(cancelButton).toBeTruthy()
    cancelButton.click()

    await expect(promise).rejects.toBe('cancel')
    expect(document.body.textContent).not.toContain('继续操作')
  })
})
