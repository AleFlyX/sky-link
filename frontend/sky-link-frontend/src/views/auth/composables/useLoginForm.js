import { reactive, ref } from 'vue'

// oxlint-disable-next-line no-control-regex -- authentication input must strip C0/C1 controls
const CONTROL_CHARACTERS = /[\u0000-\u001f\u007f-\u009f]/g // 控制字符范围所有 ASCII 0–31、127，以及 Latin-1 128–159 的字符
const ZERO_WIDTH_CHARACTERS = /[\u200b-\u200f\u202a-\u202e\u2060-\u206f\ufeff]/g // 零宽字符范围
const ACCOUNT_PATTERN = /^[A-Za-z0-9@._+-]+$/ // 账号允许的字符范围

/**
 * 消毒账号输入，去除控制字符、零宽字符，并将全角字符转换为半角字符
 * 例如：'  Ａ
 * @param {String} value
 * @returns
 */
export function sanitizeAuthAccount(value) {
  return String(value ?? '')
    .normalize('NFKC') // 将全角字符转换为半角字符
    .replace(CONTROL_CHARACTERS, '') // 移除控制字符
    .replace(ZERO_WIDTH_CHARACTERS, '') // 移除零宽字符
    .trim() // 移除首尾空格
}

/**
 * 消毒密码输入，去除控制字符和零宽字符，但保留首尾空格
 * 例如：'  pass\u200bword  ' => '  password  '
 * @param {String} value
 * @returns
 */
export function sanitizeLoginPassword(value) {
  return String(value ?? '')
    .replace(CONTROL_CHARACTERS, '')
    .replace(ZERO_WIDTH_CHARACTERS, '')
}

export function useLoginForm() {
  const formRef = ref()
  const form = reactive({
    account: '',
    password: '',
    remember: true,
  })

  const rules = {
    account: [
      { required: true, message: '请输入账号', trigger: 'blur' },
      { min: 3, max: 64, message: '账号长度应为 3-64 个字符', trigger: 'blur' },
      {
        pattern: ACCOUNT_PATTERN,
        message: '账号仅支持字母、数字、邮箱符号和 . _ + -',
        trigger: 'blur',
      },
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 8, max: 72, message: '密码长度应为 8-72 个字符', trigger: 'blur' },
    ],
  }

  function sanitizeForm() {
    form.account = sanitizeAuthAccount(form.account)
    form.password = sanitizeLoginPassword(form.password)
  }

  function getCredentials() {
    sanitizeForm()
    return {
      account: form.account,
      password: form.password,
    }
  }

  return {
    formRef,
    form,
    rules,
    sanitizeForm,
    getCredentials,
  }
}
