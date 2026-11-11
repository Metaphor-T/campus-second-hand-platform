// 浏览器验收使用显式配置的测试管理员，不读取 SQL 或本机凭据文件。
function getAdminCredentials(env = process.env) {
  const account = env.CAMPUS_TEST_ADMIN_ACCOUNT?.trim()
  const password = env.CAMPUS_TEST_ADMIN_PASSWORD
  if (!account || !password?.trim()) {
    throw new Error('浏览器验收需要设置 CAMPUS_TEST_ADMIN_ACCOUNT 和 CAMPUS_TEST_ADMIN_PASSWORD，请使用独立开发测试库的管理员账号。')
  }
  return { account, password }
}
module.exports = { getAdminCredentials }
