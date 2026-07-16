import { afterEach, describe, expect, it, vi } from 'vitest'

import { ElMessage } from 'element-plus'

import { createDepartment, getDepartments } from '../api/workspace'
import { useDepartmentManagement } from '../views/departments/composables/useDepartmentManagement'

vi.mock('element-plus', () => ({
  ElMessage: {
    warning: vi.fn(),
    success: vi.fn(),
    error: vi.fn(),
  },
}))

vi.mock('../api/workspace', () => ({
  addDepartmentMembers: vi.fn(),
  createDepartment: vi.fn(() => Promise.resolve({})),
  deleteDepartment: vi.fn(),
  getDepartmentMembers: vi.fn(),
  getDepartments: vi.fn(() => Promise.resolve({ data: { records: [], total: 0, page: 1, size: 500 } })),
  getUsers: vi.fn(),
  removeDepartmentMember: vi.fn(),
  updateDepartment: vi.fn(),
}))

vi.mock('../composables/useConfirmDialog', () => ({
  useConfirmDialog: () => ({
    confirm: vi.fn(),
  }),
}))

afterEach(() => {
  vi.clearAllMocks()
})

describe('useDepartmentManagement', () => {
  it('uses the submitted form data when creating a department', async () => {
    const management = useDepartmentManagement()

    await management.saveDepartment({
      departmentName: '  产品研发部  ',
      leaderId: '',
      description: '  负责产品迭代  ',
    })

    expect(createDepartment).toHaveBeenCalledWith({
      departmentName: '产品研发部',
      leaderId: undefined,
      description: '负责产品迭代',
    })
    expect(ElMessage.warning).not.toHaveBeenCalled()
    expect(getDepartments).toHaveBeenCalled()
  })
})
