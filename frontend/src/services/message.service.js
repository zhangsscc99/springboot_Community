import axios from 'axios';
import authHeader from './auth-header';

const API_URL = '/api/messages';

class MessageService {
  /**
   * 获取当前用户的所有会话
   */
  getConversations(page = 0, size = 20) {
    return axios.get(`${API_URL}/conversations?page=${page}&size=${size}`, { headers: authHeader() });
  }

  /**
   * 获取特定会话的详情
   */
  getConversationDetails(partnerId) {
    return axios.get(`${API_URL}/conversations/${partnerId}`, { headers: authHeader() });
  }

  /**
   * 获取与特定用户的消息历史
   */
  getMessageHistory(partnerId, page = 0, size = 50) {
    return axios.get(`${API_URL}/history/${partnerId}?page=${page}&size=${size}`, { headers: authHeader() });
  }

  /**
   * 发送私信
   */
  sendMessage(receiverId, content) {
    return axios.post(API_URL, { receiverId, content }, { headers: authHeader() });
  }

  /**
   * 标记与特定用户的消息为已读
   */
  markAsRead(partnerId) {
    return axios.put(`${API_URL}/read/${partnerId}`, {}, { headers: authHeader() });
  }

  /**
   * 获取未读消息数
   */
  getUnreadCount() {
    return axios.get(`${API_URL}/unread-count`, { headers: authHeader() });
  }

  /**
   * 创建SSE连接，用于接收实时消息
   * @returns {EventSource} 事件源对象
   */
  createSseConnection() {
    const userJson = localStorage.getItem('user');
    let token = '';
    
    if (userJson) {
      try {
        const userData = JSON.parse(userJson);
        token = userData?.accessToken || '';
      } catch (e) {
        console.error('Failed to parse user data from localStorage:', e);
      }
    }
    
    // 如果localStorage中没有token，尝试从token字段获取
    if (!token) {
      token = localStorage.getItem('token') || '';
    }
    
    const eventSource = new EventSource(`${API_URL}/sse-connect?token=${token}`);
    
    return eventSource;
  }
}

export default new MessageService(); 
