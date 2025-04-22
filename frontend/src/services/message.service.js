import axios from 'axios';
import authHeader from './auth-header';
import { API_URL } from '@/config/api.config';

// Messages API endpoint
const MESSAGES_API = `${API_URL}/api/messages`;

class MessageService {
  /**
   * 获取当前用户的所有会话
   */
  getConversations(page = 0, size = 20) {
    console.log('Fetching conversations with URL:', `${MESSAGES_API}/conversations`);
    return axios.get(`${MESSAGES_API}/conversations?page=${page}&size=${size}`, { headers: authHeader() });
  }

  /**
   * 获取特定会话的详情
   */
  getConversationDetails(partnerId) {
    console.log('Fetching conversation details with URL:', `${MESSAGES_API}/conversations/${partnerId}`);
    return axios.get(`${MESSAGES_API}/conversations/${partnerId}`, { headers: authHeader() });
  }

  /**
   * 获取与特定用户的消息历史
   */
  getMessageHistory(partnerId, page = 0, size = 50) {
    console.log('Fetching message history with URL:', `${MESSAGES_API}/history/${partnerId}`);
    return axios.get(`${MESSAGES_API}/history/${partnerId}?page=${page}&size=${size}`, { headers: authHeader() });
  }

  /**
   * 发送私信
   */
  sendMessage(receiverId, content) {
    console.log('Sending message with URL:', `${MESSAGES_API}`, 'Data:', { receiverId, content });
    
    return axios({
      method: 'post',
      url: `${MESSAGES_API}`,
      data: { receiverId, content },
      headers: authHeader()
    }).catch(error => {
      console.error('Error sending message:', error.response || error);
      throw error;
    });
  }

  /**
   * 标记与特定用户的消息为已读
   */
  markAsRead(partnerId) {
    console.log('Marking messages as read with URL:', `${MESSAGES_API}/read/${partnerId}`);
    return axios.put(`${MESSAGES_API}/read/${partnerId}`, {}, { headers: authHeader() });
  }

  /**
   * 获取未读消息数
   */
  getUnreadCount() {
    return axios.get(`${MESSAGES_API}/unread-count`, { headers: authHeader() });
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
    
    const sseUrl = `${MESSAGES_API}/sse-connect?token=${encodeURIComponent(token)}`;
    console.log('Creating SSE connection to:', sseUrl);
    
    try {
      const eventSource = new EventSource(sseUrl);
      
      // Add event listeners for debugging
      eventSource.onopen = () => console.log('SSE connection opened successfully');
      eventSource.onerror = (error) => console.error('SSE connection error:', error);
      
      return eventSource;
    } catch (error) {
      console.error('Failed to create SSE connection:', error);
      // Return a mock EventSource that won't cause errors
      return {
        addEventListener: () => {},
        close: () => {},
        onerror: () => {}
      };
    }
  }
}

export default new MessageService(); 
