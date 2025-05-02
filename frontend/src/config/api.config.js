/**
 * API 配置文件
 * 包含 API 相关的所有配置信息
 */

// API基础URL配置
const apiConfig = {
  // 更新为与curl测试相同的域名
  BASE_URL: 'http://localhost:8083',
  // 或者使用相对路径，让浏览器自动使用当前域名
  // BASE_URL: '',
};

// 导出完整的API URL
export const API_URL = apiConfig.BASE_URL;

export default apiConfig; 
