/**
 * 为API请求创建认证头
 * 从localStorage获取JWT令牌并创建Authorization头
 */
export default function authHeader() {
  const user = JSON.parse(localStorage.getItem('user'));
  const token = localStorage.getItem('token');

  // 检查user对象或token是否存在
  if ((user && user.accessToken) || token) {
    // 优先使用user对象中的token（如果存在）
    const accessToken = user?.accessToken || token;
    return { Authorization: 'Bearer ' + accessToken };
  } else {
    return {};
  }
} 