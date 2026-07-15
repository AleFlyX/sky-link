import { reactive, ref } from 'vue'
import { sanitizeAuthAccount, sanitizeLoginPassword } from './useLoginForm'

const PHONE_PATTERN = /^1\d{10}$/
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function sanitizeNickname(value) {
  return String(value ?? '').normalize('NFKC').trim()
}

function sanitizePhone(value) {
  return String(value ?? '').replace(/\D/g, '').trim()
}

function sanitizeEmail(value) {
  return String(value ?? '').normalize('NFKC').trim().toLowerCase()
}

export function useRegisterForm() {
  const formRef = ref()
  const form = reactive({
    username: '',
    nickname: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
  })

  const rules = {
    username: [
      { required: true, message: '请输入账号', trigger: 'blur' },
      { min: 3, max: 64, message: '账号长度应为 3-64 个字符', trigger: 'blur' },
    ],
    nickname: [{ min: 2, max: 32, message: '昵称长度应为 2-32 个字符', trigger: 'blur' }],
    email: [
      { required: true, message: '请输入邮箱', trigger: 'blur' },
      { pattern: EMAIL_PATTERN, message: '请输入有效邮箱地址', trigger: 'blur' },
    ],
    phone: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: PHONE_PATTERN, message: '请输入 11 位手机号', trigger: 'blur' },
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 8, max: 72, message: '密码长度应为 8-72 个字符', trigger: 'blur' },
      {
        validator: (_, value, callback) => {
          const hasLetter = /[A-Za-z]/.test(value)
          const hasDigit = /\d/.test(value)
          if (!hasLetter || !hasDigit) {
            callback(new Error('密码需同时包含字母和数字'))
            return
          }
          callback()
        },
        trigger: 'blur',
      },
    ],
    confirmPassword: [
      { required: true, message: '请再次输入密码', trigger: 'blur' },
      {
        validator: (_, value, callback) => {
          if (value !== form.password) {
            callback(new Error('两次输入的密码不一致'))
            return
          }
          callback()
        },
        trigger: 'blur',
      },
    ],
  }

  function sanitizeForm() {
    form.username = sanitizeAuthAccount(form.username)
    form.nickname = sanitizeNickname(form.nickname)
    form.email = sanitizeEmail(form.email)
    form.phone = sanitizePhone(form.phone)
    form.password = sanitizeLoginPassword(form.password)
    form.confirmPassword = sanitizeLoginPassword(form.confirmPassword)
  }

  function getPayload() {
    sanitizeForm()
    return {
      username: form.username,
      nickname: form.nickname || form.username,
      email: form.email,
      phone: form.phone,
      password: form.password,
    }
  }

  return {
    formRef,
    form,
    rules,
    sanitizeForm,
    getPayload,
  }
}
