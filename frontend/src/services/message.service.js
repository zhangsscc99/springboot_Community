import axios from 'axios';
import authHeader from './auth-header';
import { API_URL } from '@/config/api.config';

// Messages API endpoint
const MESSAGES_API = `${API_URL}/api/messages`;

// 缓存配置
const CACHE_KEY_CONVERSATIONS = 'cached_conversations';
const CACHE_KEY_HISTORY_PREFIX = 'cached_messages_history_';
const CACHE_EXPIRY_TIME = 5 * 60 * 1000; // 5分钟过期时间，单位毫秒
const HISTORY_CACHE_EXPIRY_TIME = 2 * 60 * 1000; // 2分钟过期时间，单位毫秒

class MessageService {
  /**
   * 获取当前用户的所有会话 - 添加缓存功能
   */
  async getConversations(page = 0, size = 20, useCache = true) {
    // 如果允许使用缓存，先尝试从本地缓存获取
    if (useCache) {
      const cachedData = this.getCachedConversations();
      if (cachedData) {
        console.log('Using cached conversations data');
        return Promise.resolve(cachedData);
      }
    }

    // 缓存不存在或已过期，从服务器获取
    console.log('Fetching conversations from server:', `${MESSAGES_API}/conversations`);
    try {
      const response = await axios.get(`${MESSAGES_API}/conversations?page=${page}&size=${size}`, { headers: authHeader() });
      // 缓存新数据
      this.cacheConversations(response);
      return response;
    } catch (error) {
      console.error('Error fetching conversations:', error);
      throw error;
    }
  }

  /**
   * 缓存会话数据
   */
  cacheConversations(response) {
    if (!response || !response.data) return;
    
    const cacheData = {
      data: response.data,
      timestamp: Date.now()
    };
    
    try {
      localStorage.setItem(CACHE_KEY_CONVERSATIONS, JSON.stringify(cacheData));
      console.log('Conversations data cached successfully');
    } catch (error) {
      console.error('Failed to cache conversations data:', error);
    }
  }

  /**
   * 从缓存获取会话数据，如果缓存存在且未过期
   */
  getCachedConversations() {
    try {
      const cachedItem = localStorage.getItem(CACHE_KEY_CONVERSATIONS);
      if (!cachedItem) return null;
      
      const cache = JSON.parse(cachedItem);
      const now = Date.now();
      
      // 检查缓存是否过期
      if (now - cache.timestamp > CACHE_EXPIRY_TIME) {
        console.log('Conversations cache expired, will fetch from server');
        localStorage.removeItem(CACHE_KEY_CONVERSATIONS);
        return null;
      }
      
      return { data: cache.data, status: 200, cached: true };
    } catch (error) {
      console.error('Error reading conversations cache:', error);
      return null;
    }
  }

  /**
   * 主动清除会话缓存
   */
  clearConversationsCache() {
    try {
      localStorage.removeItem(CACHE_KEY_CONVERSATIONS);
      console.log('Conversations cache cleared');
    } catch (error) {
      console.error('Failed to clear conversations cache:', error);
    }
  }

  /**
   * 更新缓存中的特定会话
   * 当收到新消息或标记已读等操作时使用
   */
  updateConversationInCache(partnerId, updatedData) {
    try {
      const cachedItem = localStorage.getItem(CACHE_KEY_CONVERSATIONS);
      if (!cachedItem) return;
      
      const cache = JSON.parse(cachedItem);
      const now = Date.now();
      
      // 如果缓存已过期，直接删除
      if (now - cache.timestamp > CACHE_EXPIRY_TIME) {
        localStorage.removeItem(CACHE_KEY_CONVERSATIONS);
        return;
      }
      
      // 更新缓存中的会话数据
      if (cache.data && cache.data.content) {
        const conversations = cache.data.content;
        const index = conversations.findIndex(conv => conv.partnerId === partnerId);
        
        if (index !== -1) {
          conversations[index] = { ...conversations[index], ...updatedData };
          
          // 将会话移到顶部
          const updatedConversation = conversations.splice(index, 1)[0];
          conversations.unshift(updatedConversation);
          
          // 更新缓存
          cache.timestamp = now; // 刷新缓存时间戳
          localStorage.setItem(CACHE_KEY_CONVERSATIONS, JSON.stringify(cache));
          console.log('Conversation updated in cache');
        }
      }
    } catch (error) {
      console.error('Failed to update conversation in cache:', error);
    }
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
  async getMessageHistory(partnerId, page = 0, size = 50, useCache = true) {
    // 如果允许使用缓存，先尝试从缓存获取
    if (useCache && partnerId) {
      const cachedData = this.getCachedMessageHistory(partnerId);
      if (cachedData) {
        console.log(`Using cached message history for partner ID: ${partnerId}`);
        return Promise.resolve(cachedData);
      }
    }
    
    console.log('Fetching message history from server:', `${MESSAGES_API}/history/${partnerId}`);
    try {
      const response = await axios.get(
        `${MESSAGES_API}/history/${partnerId}?page=${page}&size=${size}`, 
        { headers: authHeader() }
      );
      
      // 缓存新数据
      this.cacheMessageHistory(partnerId, response);
      return response;
    } catch (error) {
      console.error('Error fetching message history:', error);
      throw error;
    }
  }
  
  /**
   * 缓存消息历史
   */
  cacheMessageHistory(partnerId, response) {
    if (!partnerId || !response || !response.data) return;
    
    const cacheKey = `${CACHE_KEY_HISTORY_PREFIX}${partnerId}`;
    const cacheData = {
      data: response.data,
      timestamp: Date.now()
    };
    
    try {
      localStorage.setItem(cacheKey, JSON.stringify(cacheData));
      console.log(`Message history for partner ID: ${partnerId} cached successfully`);
    } catch (error) {
      console.error('Failed to cache message history:', error);
      // 如果存储失败（可能是存储空间不足），清除所有历史缓存
      this.clearAllMessageHistoryCache();
    }
  }
  
  /**
   * 从缓存获取消息历史
   */
  getCachedMessageHistory(partnerId) {
    if (!partnerId) return null;
    
    try {
      const cacheKey = `${CACHE_KEY_HISTORY_PREFIX}${partnerId}`;
      const cachedItem = localStorage.getItem(cacheKey);
      if (!cachedItem) return null;
      
      const cache = JSON.parse(cachedItem);
      const now = Date.now();
      
      // 检查缓存是否过期
      if (now - cache.timestamp > HISTORY_CACHE_EXPIRY_TIME) {
        console.log(`Message history cache for partner ID: ${partnerId} expired`);
        localStorage.removeItem(cacheKey);
        return null;
      }
      
      return { data: cache.data, status: 200, cached: true };
    } catch (error) {
      console.error('Error reading message history cache:', error);
      return null;
    }
  }
  
  /**
   * 清除特定用户的消息历史缓存
   */
  clearMessageHistoryCache(partnerId) {
    if (!partnerId) return;
    
    try {
      const cacheKey = `${CACHE_KEY_HISTORY_PREFIX}${partnerId}`;
      localStorage.removeItem(cacheKey);
      console.log(`Message history cache for partner ID: ${partnerId} cleared`);
    } catch (error) {
      console.error('Failed to clear message history cache:', error);
    }
  }
  
  /**
   * 清除所有消息历史缓存
   */
  clearAllMessageHistoryCache() {
    try {
      const keysToRemove = [];
      
      // 查找所有消息历史缓存
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key && key.startsWith(CACHE_KEY_HISTORY_PREFIX)) {
          keysToRemove.push(key);
        }
      }
      
      // 删除找到的缓存
      keysToRemove.forEach(key => localStorage.removeItem(key));
      console.log(`Cleared ${keysToRemove.length} message history caches`);
    } catch (error) {
      console.error('Failed to clear all message history caches:', error);
    }
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
    }).then(response => {
      // 发送消息成功后，清除缓存以便获取最新数据
      this.clearConversationsCache();
      this.clearMessageHistoryCache(receiverId);
      return response;
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
    return axios.put(`${MESSAGES_API}/read/${partnerId}`, {}, { headers: authHeader() })
      .then(response => {
        // 更新缓存中的未读计数
        this.updateConversationInCache(partnerId, { unreadCount: 0 });
        return response;
      });
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
