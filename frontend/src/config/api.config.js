/**
 * API 配置文件
 * 包含 API 相关的所有配置信息
 */

// API基础URL配置
const apiConfig = {
  // 本地开发环境配置
  BASE_URL: 'http://localhost:8083',
  // 生产环境可以使用：
  // BASE_URL: 'http://47.243.102.28:8083',
};

// 导出完整的API URL
export const API_URL = apiConfig.BASE_URL;

export default apiConfig; 
