import axios from 'axios';
import authHeader from './auth-header';
import { API_URL } from '@/config/api.config';

class ConversationService {
  /**
   * 获取所有会话列表
   * @returns {Promise} 包含会话列表的Promise
   */
  getConversations() {
    return axios.get(`${API_URL}/conversations`, { headers: authHeader() });
  }

  /**
   * 获取与特定用户的会话详情
   * @param {number} partnerId - 对话用户ID 
   * @returns {Promise} 包含会话详情的Promise
   */
  getConversationDetails(partnerId) {
    return axios.get(`${API_URL}/conversations/${partnerId}`, { headers: authHeader() });
  }

  /**
   * 获取会话的消息历史
   * @param {number} partnerId - 对话用户ID
   * @param {number} page - 页码，从0开始
   * @param {number} size - 每页消息数量
   * @returns {Promise} 包含消息列表的Promise
   */
  getMessages(partnerId, page = 0, size = 20) {
    return axios.get(`${API_URL}/conversations/${partnerId}/messages`, {
      headers: authHeader(),
      params: { page, size }
    });
  }

  /**
   * 发送消息
   * @param {number} receiverId - 接收者ID
   * @param {string} content - 消息内容
   * @returns {Promise} 包含发送结果的Promise
   */
  sendMessage(receiverId, content) {
    return axios.post(`${API_URL}/messages`, {
      receiverId,
      content
    }, { headers: authHeader() });
  }

  /**
   * 将与特定用户的对话标记为已读
   * @param {number} partnerId - 对话用户ID
   * @returns {Promise} 包含操作结果的Promise
   */
  markAsRead(partnerId) {
    return axios.put(`${API_URL}/conversations/${partnerId}/read`, {}, { headers: authHeader() });
  }

  /**
   * 获取未读消息总数
   * @returns {Promise} 包含未读消息数的Promise
   */
  getUnreadCount() {
    return axios.get(`${API_URL}/conversations/unread-count`, { headers: authHeader() });
  }

  /**
   * 创建 SSE 连接以接收实时消息
   * @returns {EventSource} SSE 连接
   */
  createSseConnection() {
    const token = localStorage.getItem('token');
    const eventSource = new EventSource(`${API_URL}/sse/connect?token=${token}`);
    return eventSource;
  }

  /**
   * 删除会话
   * @param {number} partnerId - 对话用户ID
   * @returns {Promise} 包含操作结果的Promise
   */
  deleteConversation(partnerId) {
    return axios.delete(`${API_URL}/conversations/${partnerId}`, { headers: authHeader() });
  }

  /**
   * 关闭 SSE 连接
   * @param {EventSource} eventSource - 要关闭的 SSE 连接
   */
  closeSseConnection(eventSource) {
    if (eventSource) {
      eventSource.close();
    }
  }
}

export default new ConversationService(); 