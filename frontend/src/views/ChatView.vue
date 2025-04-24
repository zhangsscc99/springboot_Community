<template>
  <div class="chat-page">
    <div class="chat-header">
      <div class="back-button" @click="goBack">
        <i class="fas fa-chevron-left"></i>
      </div>
      <div class="partner-info">
        <div class="partner-name">{{ partnerName }}</div>
        <div class="online-status" v-if="isOnline">在线</div>
      </div>
      <div class="menu-button" @click="showMenu">
        <i class="fas fa-ellipsis-v"></i>
      </div>
    </div>

    <div class="chat-content">
      <div class="chat-body">
        <div class="loading-indicator" v-if="loading">
          <div class="loader"></div>
        </div>

        <div v-else-if="messages.length === 0" class="empty-chat">
          <div class="icon">
            <i class="fas fa-comments"></i>
          </div>
          <div class="text">
            <p>没有消息记录</p>
            <p class="hint">开始发送消息吧</p>
          </div>
        </div>

        <div v-else class="message-list">
          <div v-for="(message, index) in messages" :key="message.id" class="message-wrapper" :data-sender="isOwnMessage(message) ? 'own' : 'other'" :data-message-id="message.id">
            <div v-if="shouldShowDateDivider(message, index)" class="date-divider">
              {{ formatDate(message.createdAt) }}
            </div>
            <div v-if="shouldShowTimeDisplay(message, index)" class="time-display">
              {{ formatMessageTime(message.createdAt) }}
            </div>
            <div class="message" :class="{ 'own': isOwnMessage(message) }">
              <div class="avatar" v-if="!isOwnMessage(message)">
                <UserAvatar 
                  :src="message.senderAvatar || partnerAvatar || 'https://via.placeholder.com/35/CCCCCC/666666/?text=?'" 
                  :username="message.senderUsername || partnerName || '对方'" 
                  :size="35"
                />
              </div>
              <div class="message-content">
                <div class="message-text" v-html="formatMessage(message.content)"></div>
                <div class="message-time">{{ formatTime(message.createdAt || message.timestamp) }}</div>
              </div>
              <div class="avatar" v-if="isOwnMessage(message)">
                <UserAvatar 
                  :src="userAvatar || 'https://via.placeholder.com/35/007AFF/FFFFFF/?text=YOU'" 
                  username="我" 
                  :size="35"
                />
              </div>
            </div>
          </div>
          <div v-if="hasMoreMessages && !loadingMore" class="load-more">
            <button @click="loadMoreMessages">加载更多消息</button>
          </div>
          <div v-if="loadingMore" class="loading-more">
            <div class="small-loader"></div>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input-bar">
      <div class="chat-input-container">
        <input 
          type="text" 
          class="chat-input" 
          v-model="newMessage"
          placeholder="输入消息..." 
          @keyup.enter="sendMessage"
        />
      </div>
      <div class="input-actions">
        <button class="action-button emoji-button" @click="toggleEmojiPicker">
          <i class="fas fa-smile"></i>
        </button>
        <button class="action-button image-button" @click="openImagePicker">
          <i class="fas fa-image"></i>
        </button>
        <button class="send-button" @click="sendMessage" :disabled="!newMessage.trim()">
          <i class="fas fa-paper-plane"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import MessageService from '@/services/message.service';
import { format, isToday, isYesterday, isSameDay } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import UserAvatar from '@/components/UserAvatar.vue';

export default {
  name: 'ChatView',
  components: {
    UserAvatar
  },
  data() {
    return {
      partnerId: null,
      partnerName: '',
      partnerAvatar: '',
      isOnline: false,
      messages: [],
      newMessage: '',
      loading: true,
      loadingMore: false,
      page: 0,
      size: 20,
      hasMoreMessages: false,
      userAvatar: '',
      eventSource: null
    };
  },
  created() {
    // Get partner ID from route params and validate it
    const partnerIdParam = this.$route.params.id;
    console.log('Partner ID from route:', partnerIdParam);
    
    if (!partnerIdParam || partnerIdParam === 'undefined') {
      console.error('Invalid partner ID detected');
      // Handle invalid ID - redirect to messages page
      this.$router.push('/messages');
      return;
    }
    
    this.partnerId = parseInt(partnerIdParam);
    console.log('Parsed partner ID:', this.partnerId);
    
    // Only proceed if we have a valid partner ID
    if (isNaN(this.partnerId) || this.partnerId <= 0) {
      console.error('Partner ID could not be parsed as a valid number');
      this.$router.push('/messages');
      return;
    }
    
    this.loadUserInfo();
    this.fetchConversationDetails();
    this.fetchMessages();
    this.setupSseConnection();
  },
  mounted() {
    // Force scroll to bottom on mount
    this.$nextTick(() => {
      this.scrollToBottom();
    });
    
    // Focus on input field
    const input = document.querySelector('.chat-input');
    if (input) {
      input.focus();
    }
    
    // 检查用户头像
    console.log('[Debug] 界面挂载时的用户头像:', this.userAvatar);
    
    // 如果未设置头像，尝试重新加载用户信息和设置默认头像
    if (!this.userAvatar) {
      console.log('[Debug] 挂载时未找到头像，重新加载用户信息');
      this.loadUserInfo();
      
      // 设置默认头像
      this.userAvatar = 'https://via.placeholder.com/35/007AFF/FFFFFF/?text=YOU';
      console.log('[Debug] 设置默认头像:', this.userAvatar);
    }
    
    // 在组件挂载后检查头像元素是否正确显示
    setTimeout(() => {
      const avatars = document.querySelectorAll('.message .avatar');
      console.log('[Debug] 页面中发现的头像元素数量:', avatars.length);
      
      // 检查 UserAvatar 组件是否正确挂载
      const userAvatars = document.querySelectorAll('.user-avatar');
      console.log('[Debug] 页面中发现的 UserAvatar 组件数量:', userAvatars.length);
      
      // 检查是否有消息应该显示为自己的
      const ownMessages = document.querySelectorAll('.message.own');
      console.log('[Debug] 自己发送的消息数量:', ownMessages.length);
    }, 1000);
  },
  beforeUnmount() {
    this.closeSseConnection();
    // Mark messages as read when leaving
    this.markAsRead();
  },
  methods: {
    loadUserInfo() {
      // 获取用户数据 - 使用正确的键名 'userInfo'
      const userJson = localStorage.getItem('userInfo');
      if (userJson) {
        try {
          const userData = JSON.parse(userJson);
          console.log('[Debug] 用户数据:', userData);
          
          // 直接使用用户数据中的 avatar 字段
          this.userAvatar = userData.avatar || '';
          console.log('[Debug] 设置用户头像:', this.userAvatar);
          
          // 如果用户没有设置头像，UserAvatar 组件会使用默认头像
        } catch (e) {
          console.error('[Debug] 解析用户数据失败:', e);
        }
      } else {
        console.log('[Debug] localStorage 中未找到用户数据，尝试使用 user 键');
        // 尝试使用旧键名 'user' 作为兼容性处理
        const oldUserJson = localStorage.getItem('user');
        if (oldUserJson) {
          try {
            const userData = JSON.parse(oldUserJson);
            this.userAvatar = userData.avatar || '';
            console.log('[Debug] 从旧键获取用户头像:', this.userAvatar);
          } catch (e) {
            console.error('[Debug] 解析旧用户数据失败:', e);
          }
        } else {
          console.log('[Debug] localStorage 中未找到任何用户数据');
        }
      }
    },
    async fetchConversationDetails() {
      try {
        const response = await MessageService.getConversationDetails(this.partnerId);
        if (response.data) {
          this.partnerName = response.data.partnerUsername;
          this.partnerAvatar = response.data.partnerAvatar;
        }
      } catch (error) {
        console.error('Failed to fetch conversation details:', error);
      }
    },
    async fetchMessages() {
      try {
        this.loading = true;
        const response = await MessageService.getMessageHistory(this.partnerId, this.page, this.size);
        
        if (response.data && response.data.content) {
          // Sort messages by date (oldest first)
          const newMessages = response.data.content.sort((a, b) => 
            new Date(a.createdAt) - new Date(b.createdAt)
          );
          
          this.messages = [...this.messages, ...newMessages];
          this.hasMoreMessages = !response.data.last;
          
          // Mark messages as read
          this.markAsRead();
          
          // Scroll to bottom on initial load
          if (this.page === 0) {
            this.$nextTick(() => {
              this.scrollToBottom();
            });
          }
        }
      } catch (error) {
        console.error('Failed to fetch messages:', error);
      } finally {
        this.loading = false;
      }
    },
    async loadMoreMessages() {
      if (this.loadingMore || !this.hasMoreMessages) return;
      
      this.loadingMore = true;
      this.page += 1;
      
      try {
        const response = await MessageService.getMessageHistory(this.partnerId, this.page, this.size);
        
        if (response.data && response.data.content) {
          // Sort messages by date (oldest first)
          const newMessages = response.data.content.sort((a, b) => 
            new Date(a.createdAt) - new Date(b.createdAt)
          );
          
          // Prepend older messages
          this.messages = [...newMessages, ...this.messages];
          this.hasMoreMessages = !response.data.last;
          
          // Maintain scroll position relative to new content
          this.$nextTick(() => {
            if (this.$refs.messagesContainer && newMessages.length > 0) {
              const firstNewMessage = document.querySelector(`[data-id="${newMessages[0].id}"]`);
              if (firstNewMessage) {
                firstNewMessage.scrollIntoView();
              }
            }
          });
        }
      } catch (error) {
        console.error('Failed to load more messages:', error);
      } finally {
        this.loadingMore = false;
      }
    },
    async sendMessage() {
      const content = this.newMessage.trim();
      if (!content) return;
      
      try {
        // 发送前检查头像
        console.log('[Debug] 发送消息前的头像:', this.userAvatar);
        
        // 如果没有设置头像，设置默认头像
        if (!this.userAvatar) {
          console.log('[Debug] 发送消息前未找到头像，设置默认头像');
          this.userAvatar = 'https://via.placeholder.com/35/007AFF/FFFFFF/?text=YOU';
        }
        
        // 创建临时消息，使用与 Profile 相同的头像
        const tempMessage = {
          id: 'temp-' + Date.now(),
          senderId: this.getCurrentUserId(),
          receiverId: this.partnerId,
          content: content,
          createdAt: new Date().toISOString(),
          senderAvatar: this.userAvatar,
          senderUsername: '我'
        };
        
        console.log('[Debug] 创建的临时消息:', tempMessage);
        console.log('[Debug] 临时消息头像:', tempMessage.senderAvatar);
        
        // Add temporary message immediately
        this.messages.push(tempMessage);
        
        // 检查添加后是否正确标记为自己的消息
        this.$nextTick(() => {
          const lastMessage = document.querySelector('.message-wrapper:last-child .message');
          console.log('[Debug] 新消息添加后是否标记为自己的消息:', lastMessage?.classList.contains('own'));
        });
        
        // Clear input right away for better UX
        this.newMessage = '';
        
        // Scroll to the new message
        this.$nextTick(() => {
          this.scrollToBottom();
          // Focus input for continuous typing
          const input = document.querySelector('.chat-input');
          if (input) {
            input.focus();
          }
        });
        
        // Send to server
        console.log('Sending message to API...', {
          receiverId: this.partnerId,
          content: content
        });
        
        const response = await MessageService.sendMessage(this.partnerId, content);
        console.log('Message sent successfully:', response);
        
        // Replace temp message with actual response if needed
        if (response.data) {
          const index = this.messages.findIndex(m => m.id === tempMessage.id);
          if (index !== -1) {
            this.messages.splice(index, 1, response.data);
          }
        }
      } catch (error) {
        console.error('Failed to send message:', error);
        // Keep the message in UI but mark it as failed
        const failedMessage = this.messages.find(m => m.id.toString().includes('temp'));
        if (failedMessage) {
          failedMessage.sendFailed = true;
        }
        // More user-friendly error message
        if (error.response && error.response.status === 404) {
          alert('发送失败: API接口未找到，请检查后端服务');
        } else {
          alert('发送失败: ' + (error.message || '未知错误，请稍后重试'));
        }
      }
    },
    async markAsRead() {
      try {
        await MessageService.markAsRead(this.partnerId);
      } catch (error) {
        console.error('Failed to mark messages as read:', error);
      }
    },
    setupSseConnection() {
      // Only set up SSE if user is logged in
      const userJson = localStorage.getItem('user');
      const token = localStorage.getItem('token');
      
      if (!userJson && !token) return;
      
      try {
        this.eventSource = MessageService.createSseConnection();
        
        // Listen for private message events
        this.eventSource.addEventListener('privateMessage', event => {
          const message = JSON.parse(event.data);
          this.handleNewMessage(message);
        });
        
        // Handle SSE connection errors
        this.eventSource.onerror = error => {
          console.error('SSE connection error:', error);
          this.closeSseConnection();
          // Retry connection after a delay
          setTimeout(() => this.setupSseConnection(), 5000);
        };
      } catch (error) {
        console.error('Failed to establish SSE connection:', error);
      }
    },
    closeSseConnection() {
      if (this.eventSource) {
        this.eventSource.close();
        this.eventSource = null;
      }
    },
    handleNewMessage(message) {
      // Check if message belongs to current conversation
      const isRelevant = 
        (message.senderId === this.partnerId && message.receiverId === this.getCurrentUserId()) ||
        (message.senderId === this.getCurrentUserId() && message.receiverId === this.partnerId);
      
      if (isRelevant) {
        // Add message to list
        this.messages.push(message);
        // Mark as read
        this.markAsRead();
        // Scroll to bottom
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      }
    },
    getCurrentUserId() {
      // Try to get user data from localStorage
      const userJson = localStorage.getItem('user');
      const userInfoJson = localStorage.getItem('userInfo');
      const token = localStorage.getItem('token');
      
      console.log('[Debug] Looking for user ID in localStorage');
      
      // 首先尝试从 userInfo 中获取
      if (userInfoJson) {
        try {
          const userData = JSON.parse(userInfoJson);
          console.log('[Debug] userInfo 数据:', userData);
          
          // 检查是否有ID
          if (userData.id) {
            console.log('[Debug] 从 userInfo 找到用户 ID:', userData.id);
            return userData.id;
          }
        } catch (e) {
          console.error('[Debug] 解析 userInfo 失败:', e);
        }
      }
      
      // 然后尝试从 user 中获取
      if (userJson) {
        try {
          const userData = JSON.parse(userJson);
          console.log('[Debug] user 数据:', userData);
          
          // Check if id exists directly
          if (userData.id) {
            console.log('[Debug] 从 user 找到用户 ID:', userData.id);
            return userData.id;
          }
          
          // Some implementations might store user ID in userId field
          if (userData.userId) {
            console.log('[Debug] 从 user 找到 userId:', userData.userId);
            return userData.userId;
          }
          
          // Or it might be nested in a user object
          if (userData.user && userData.user.id) {
            console.log('[Debug] 从 user.user 找到用户 ID:', userData.user.id);
            return userData.user.id;
          }
          
          console.log('[Debug] Could not find user ID in userData:', userData);
        } catch (e) {
          console.error('[Debug] Failed to parse user data:', e);
        }
      } else {
        console.log('[Debug] No user data found in localStorage');
      }
      
      // As a fallback, try to find the user ID in the token
      if (token) {
        try {
          // If token is a JWT, try to decode it
          const parts = token.split('.');
          if (parts.length === 3) {
            const payload = JSON.parse(atob(parts[1]));
            console.log('[Debug] Decoded JWT payload:', payload);
            
            // JWT might store user ID in different fields
            if (payload.id) return payload.id;
            if (payload.userId) return payload.userId;
            if (payload.sub) return payload.sub;
          }
        } catch (e) {
          console.error('[Debug] Failed to decode token:', e);
        }
      }
      
      // 如果所有方法都失败，返回一个临时ID（这是为了调试目的）
      const tempId = "temp-user-" + Date.now();
      console.warn('[Debug] 无法确定用户ID，使用临时ID:', tempId);
      return tempId;
    },
    isOwnMessage(message) {
      // 获取当前用户 ID
      const currentUserId = this.getCurrentUserId();
      
      console.log('[Debug] isOwnMessage 检查:', 
        '消息ID:', message?.id, 
        '发送者ID:', message?.senderId, 
        '当前用户ID:', currentUserId,
        '是否是自己的消息:', message?.senderId === currentUserId
      );
      
      // 如果消息或发送者 ID 不存在，返回 false
      if (!message || !message.senderId) {
        return false;
      }
      
      // 特殊处理临时消息（刚发送的消息）
      if (message.id && message.id.toString().startsWith('temp-')) {
        console.log('[Debug] 检测到临时消息，设置为自己的消息');
        return true;
      }
      
      // 比较消息发送者 ID 与当前用户 ID
      return String(message.senderId) === String(currentUserId);
    },
    scrollToBottom() {
      const chatBody = document.querySelector('.chat-body');
      if (chatBody) {
        this.$nextTick(() => {
          chatBody.scrollTop = chatBody.scrollHeight;
        });
      }
    },
    shouldShowDateDivider(message, index) {
      if (index === 0) return true;
      
      const prevMessage = this.messages[index - 1];
      return !isSameDay(new Date(message.createdAt), new Date(prevMessage.createdAt));
    },
    shouldShowTimeDisplay(message, index) {
      if (index === 0) return true;
      
      const prevMessage = this.messages[index - 1];
      const currentTime = new Date(message.createdAt);
      const prevTime = new Date(prevMessage.createdAt);
      
      // Show time if more than 5 minutes have passed since the last message
      return (currentTime - prevTime) > 5 * 60 * 1000;
    },
    formatDate(dateString) {
      const date = new Date(dateString);
      
      if (isToday(date)) {
        return '今天';
      } else if (isYesterday(date)) {
        return '昨天';
      } else {
        return format(date, 'yyyy年MM月dd日', { locale: zhCN });
      }
    },
    formatMessageTime(dateString) {
      const date = new Date(dateString);
      const now = new Date();
      
      // Format for timestamps - format like "下午3:44"
      const hour = date.getHours();
      const amPm = hour < 12 ? '上午' : '下午';
      const hour12 = hour <= 12 ? hour : hour - 12;
      const hourFormatted = hour12 === 0 ? 12 : hour12; // Handle midnight (0 hours)
      const minuteFormatted = date.getMinutes().toString().padStart(2, '0');
      
      return `${amPm}${hourFormatted}:${minuteFormatted}`;
    },
    formatMessage(content) {
      // 处理消息内容，例如将链接转换为可点击的 HTML
      if (!content) return '';
      
      // 简单的链接检测和转换
      const urlRegex = /(https?:\/\/[^\s]+)/g;
      return content.replace(urlRegex, url => `<a href="${url}" target="_blank">${url}</a>`);
    },
    formatTime(timestamp) {
      if (!timestamp) return '';
      
      const date = new Date(timestamp);
      const now = new Date();
      
      // 如果是当天的消息，只显示时间
      if (date.toDateString() === now.toDateString()) {
        const hours = date.getHours().toString().padStart(2, '0');
        const minutes = date.getMinutes().toString().padStart(2, '0');
        return `${hours}:${minutes}`;
      }
      
      // 否则显示日期和时间
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      return `${month}-${day} ${hours}:${minutes}`;
    },
    goBack() {
      this.$router.push('/messages');
    },
    showMenu() {
      // TODO: Show options menu (delete conversation, block user, etc.)
      alert('功能开发中');
    },
    toggleEmojiPicker() {
      // TODO: Implement emoji picker
      alert('表情选择器开发中');
    },
    openImagePicker() {
      // TODO: Implement image upload
      alert('图片上传功能开发中');
    },
    toggleVoiceInput() {
      alert('语音输入功能开发中');
    },
    retryMessage(message) {
      // Remove the failed flag
      message.sendFailed = false;
      
      // Try to send again
      this.sendMessage(message.content);
      
      // Remove the original failed message
      const index = this.messages.findIndex(m => m.id === message.id);
      if (index !== -1) {
        this.messages.splice(index, 1);
      }
    }
  }
};
</script>

<style scoped>
@import '@/assets/css/main.css';

.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background-color: var(--background-color);
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background-color: var(--secondary-color);
  border-bottom: 1px solid var(--border-color);
  height: 60px;
  flex-shrink: 0;
}

.chat-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px 16px;
  -webkit-overflow-scrolling: touch;
  scroll-behavior: smooth;
}

.chat-input-bar {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background-color: var(--secondary-color);
  border-top: 1px solid var(--border-color);
  height: 70px;
  flex-shrink: 0;
}

.back-button, .menu-button {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--primary-color);
}

.back-button i {
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-middle), var(--primary-gradient-end));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.partner-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.partner-name {
  font-size: 17px;
  font-weight: 500;
  color: var(--text-color);
}

.online-status {
  font-size: 12px;
  color: var(--primary-color);
}

.message-list {
  display: flex;
  flex-direction: column;
  padding-bottom: 10px;
}

.message-wrapper {
  margin-bottom: 15px;
  position: relative;
  width: 100%;
  display: flex;
  flex-direction: column;
}

.message-wrapper[data-sender="own"] .message {
  margin-left: auto;
  margin-right: 0;
}

/* Add clearfix to ensure message container wraps around floated content */
.message::after {
  content: "";
  display: table;
  clear: both;
}

/* Override any styles that might be causing positioning issues */
.message-sent {
  /* Placeholder - keep this class for backwards compatibility */
}

.message-received {
  /* Placeholder - keep this class for backwards compatibility */
}

/* Remove redundant styling */
.message-sent .message-content {
  /* Already handled in the main message-content style */
}

/* Remove redundant styling */
.message-bubble {
  /* Styles are covered by message-content */
}

.message-bubble.own-bubble {
  /* No longer needed with our new styling */
}

.date-divider {
  text-align: center;
  margin: 10px 0;
  color: var(--light-text-color);
  font-size: 12px;
  background: var(--border-color);
  padding: 4px 10px;
  border-radius: 10px;
  display: inline-block;
  margin-left: auto;
  margin-right: auto;
}

.time-display {
  text-align: center;
  margin: 10px 0;
  color: var(--light-text-color);
  font-size: 13px;
}

.message {
  display: flex;
  align-items: flex-start;
  max-width: 85%;
  clear: both;
  position: relative;
  margin-bottom: 20px;
  padding-bottom: 10px;
}

.message-content {
  order: 1;
  padding: 10px 15px;
  border-radius: 16px;
  border-top-right-radius: 4px;
  word-break: break-word;
  position: relative;
  max-width: calc(100% - 55px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  margin-right: 10px;
  background-color: #007AFF;
  color: white;
}

.message .avatar {
  order: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 0 0 8px;
  flex-shrink: 0;
}

/* 移除不再需要的样式 */
.avatar img {
  /* 这些样式现在由 UserAvatar 组件提供 */
}

/* Reset all message-specific styles to ensure proper display */
.message-content {
  padding: 10px 15px;
  border-radius: 16px;
  word-break: break-word;
  position: relative;
  max-width: 100%;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message-status {
  font-size: 12px;
  color: var(--light-text-color);
  margin-top: 5px;
  text-align: right;
}

/* Fix indicator position */
.message-indicator {
  display: none; /* 隐藏所有消息指示器 */
}

.message-received .message-content {
  background-color: #E5E5EA; /* Light grey for received messages */
  color: #333;
  border-top-left-radius: 4px;
  margin-left: 5px;
  text-align: left;
}

.retry-button {
  background: none;
  border: none;
  color: var(--error-color);
  font-size: 12px;
  padding: 2px 8px;
  margin-left: 5px;
  cursor: pointer;
  text-decoration: underline;
}

.message-sender {
  font-size: 12px;
  color: var(--light-text-color);
  margin-bottom: 5px;
}

.message-time {
  font-size: 11px;
  color: var(--light-text-color);
  margin-top: 5px;
  align-self: center;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 0 20px;
  text-align: center;
  color: #8e8e8e;
}

.empty-chat .icon {
  font-size: 60px;
  margin-bottom: 20px;
  color: #d0d0d0;
}

.empty-chat .text {
  font-size: 16px;
  max-width: 300px;
  line-height: 1.4;
}

.chat-input-container {
  display: flex;
  align-items: center;
  background-color: #fff;
  border-radius: 18px;
  padding: 5px 10px;
  flex: 1;
  margin: 0 8px 0 0;
  box-shadow: 0 1px 1px rgba(0, 0, 0, 0.05);
}

.chat-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 8px;
  max-height: 100px;
  overflow-y: auto;
  font-size: 16px;
  background-color: transparent;
  resize: none;
}

.input-actions {
  display: flex;
  align-items: center;
}

.action-button {
  background: none;
  border: none;
  color: #7d7d7d;
  font-size: 20px;
  padding: 5px 8px;
  cursor: pointer;
  margin-left: 0;
}

.emoji-button, .image-button {
  margin-right: 0px;
}

.send-button {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-middle), var(--primary-gradient-end));
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.send-button:hover {
  background-image: linear-gradient(to right, var(--hover-color), var(--primary-gradient-middle), var(--hover-color));
  cursor: pointer;
}

.send-button:disabled {
  background-color: #c9c9c9;
}

.loading-indicator {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.loader {
  border: 4px solid transparent;
  border-radius: 50%;
  border-top: 4px solid var(--primary-gradient-end);
  width: 30px;
  height: 30px;
  animation: spin 1s linear infinite;
}

.load-more {
  text-align: center;
  margin: 16px 0;
}

.load-more button {
  padding: 6px 14px;
  background-color: rgba(0, 0, 0, 0.1);
  color: #666;
  border: none;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
}

.loading-more {
  text-align: center;
  margin: 16px 0;
}

.small-loader {
  border: 2px solid transparent;
  border-radius: 50%;
  border-top: 2px solid;
  border-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-middle), var(--primary-gradient-end));
  border-image-slice: 1;
  width: 20px;
  height: 20px;
  animation: spin 1.5s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.input-button {
  width: 30px;
  height: 30px;
  border: none;
  background-color: transparent;
  margin: 0 3px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.input-button i {
  font-size: 20px;
  color: var(--primary-color);
}

.message-input {
  flex-grow: 1;
  border: none;
  outline: none;
  resize: none;
  height: 36px;
  background: #F5F5F5;
  border-radius: 18px;
  padding: 8px 12px;
  font-size: 14px;
  margin: 0 8px;
  overflow-y: auto;
  max-height: 100px;
}

.message-input::placeholder {
  color: #999;
  font-size: 14px;
}

/* Apply global font and line-height */
body {
  font-family: 'Helvetica Neue', 'Arial', sans-serif;
  line-height: 1.6;
}

/* Ensure responsive design */
@media (max-width: 576px) {
  .chat-header {
    padding: 6px 12px;
  }
  .chat-input-bar {
    padding: 8px 12px;
  }
  .message {
    max-width: 90%;
  }
  .chat-content {
    padding: 8px 10px;
  }
}

.menu-button i {
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-middle), var(--primary-gradient-end));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.message-content.failed {
  background-color: #ffeeee;
  border: 1px dashed #ff6b6b;
}

.message-status.failed {
  color: var(--error-color);
}

/* Fix status position */
.message-status {
  font-size: 12px;
  color: var(--light-text-color);
  margin-top: 5px;
  text-align: right;
}

/* 调整自己发送的消息样式 */
.message.own {
  flex-direction: row !important;
  justify-content: flex-end;
  margin-left: auto !important;
  margin-right: 5px !important;
}

/* 调整对方发送的消息样式 */
.message:not(.own) {
  flex-direction: row !important;
  justify-content: flex-start;
  margin-left: 5px !important;
  margin-right: auto !important;
}

.message:not(.own) .message-content {
  order: 2;
  background-color: #E5E5EA; /* 灰色背景 */
  color: #333;
  border-top-left-radius: 4px;
  border-top-right-radius: 16px;
  margin-left: 10px;
  margin-right: 0;
}

.message.own .message-content {
  order: 1;
  background-color: #007AFF;
  color: white;
  border-top-right-radius: 4px;
  border-top-left-radius: 16px;
  margin-right: 10px;
  margin-left: 0;
}

.message:not(.own) .avatar {
  order: 1;
  margin: 0 0 0 0;
}

.message.own .avatar {
  order: 2;
  margin: 0 0 0 8px;
}

.message .avatar {
  min-width: 35px;
  min-height: 35px;
  width: 35px;
  height: 35px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0;
  flex-shrink: 0;
  display: flex !important;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  border: 1px solid #e0e0e0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  z-index: 5;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.message-content {
  padding: 10px 15px;
  border-radius: 16px;
  word-break: break-word;
  position: relative;
  max-width: calc(100% - 55px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

/* 修复消息指示器位置 */
.message.own .message-indicator {
  right: 0px;
  left: auto;
}

.message:not(.own) .message-indicator {
  left: 0px;
  right: auto;
}

/* 增加消息文本区域的样式 */
.message-text {
  width: 100%;
  word-wrap: break-word;
  min-width: 60px;
}
</style> 