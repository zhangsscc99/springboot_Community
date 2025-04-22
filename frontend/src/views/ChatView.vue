<template>
  <div class="chat-container">
    <!-- Header -->
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

    <!-- Messages Area -->
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
        <div v-for="(message, index) in messages" :key="message.id" class="message-wrapper">
          <!-- Date divider -->
          <div v-if="shouldShowDateDivider(message, index)" class="date-divider">
            {{ formatDate(message.createdAt) }}
          </div>
          
          <!-- Time display -->
          <div v-if="shouldShowTimeDisplay(message, index)" class="time-display">
            {{ formatMessageTime(message.createdAt) }}
          </div>

          <!-- Message bubble -->
          <div class="message" :class="{ 
            'message-sent': isOwnMessage(message),
            'message-received': !isOwnMessage(message)
          }">
            <div class="avatar" v-if="!isOwnMessage(message)">
              <img :src="message.senderAvatar || '/default-avatar.png'" :alt="message.senderUsername">
            </div>
            <div class="message-content" :class="{ 
              'own-bubble': isOwnMessage(message),
              'failed': message.sendFailed
            }">
              {{ message.content }}
              <div v-if="isOwnMessage(message)" class="message-status" :class="{ 'failed': message.sendFailed }">
                <span v-if="message.sendFailed">
                  发送失败 <button class="retry-button" @click="retryMessage(message)">重试</button>
                </span>
                <span v-else-if="message.id.toString().includes('temp')">发送中...</span>
                <span v-else>已发送</span>
              </div>
            </div>
            <div class="avatar" v-if="isOwnMessage(message)">
              <img :src="userAvatar || '/default-avatar.png'" alt="You">
            </div>
          </div>
        </div>
        
        <!-- Load more messages button -->
        <div v-if="hasMoreMessages && !loadingMore" class="load-more">
          <button @click="loadMoreMessages">加载更多消息</button>
        </div>
        <div v-if="loadingMore" class="loading-more">
          <div class="small-loader"></div>
        </div>
      </div>
    </div>

    <!-- Updated chat input styles for desktop and mobile -->
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

export default {
  name: 'ChatView',
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
  },
  beforeUnmount() {
    this.closeSseConnection();
    // Mark messages as read when leaving
    this.markAsRead();
  },
  methods: {
    loadUserInfo() {
      const userJson = localStorage.getItem('user');
      if (userJson) {
        try {
          const userData = JSON.parse(userJson);
          this.userAvatar = userData.avatar;
        } catch (e) {
          console.error('Failed to parse user data:', e);
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
        // Show optimistic update
        const tempMessage = {
          id: 'temp-' + Date.now(),
          senderId: this.getCurrentUserId(),
          receiverId: this.partnerId,
          content: content,
          createdAt: new Date().toISOString(),
          senderAvatar: this.userAvatar
        };
        
        // Add temporary message immediately
        this.messages.push(tempMessage);
        
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
      const userJson = localStorage.getItem('user');
      if (userJson) {
        try {
          const userData = JSON.parse(userJson);
          return userData.id;
        } catch (e) {
          console.error('Failed to parse user data:', e);
        }
      }
      return null;
    },
    isOwnMessage(message) {
      return message.senderId === this.getCurrentUserId();
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
.main-content {
  flex: 1;
  position: relative;
  height: 100%;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-height: 100vh;
  background-color: #f6f6f6;
  position: relative;
  margin: 0 auto;
  max-width: 960px;
  border-left: 1px solid #e0e0e0;
  border-right: 1px solid #e0e0e0;
  overflow: hidden; /* Prevent container overflow */
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background-color: #f6f6f6;
  border-bottom: 1px solid #e0e0e0;
  z-index: 10;
  flex-shrink: 0; /* Prevent header from shrinking */
  height: 50px; /* Fixed height */
}

.back-button, .menu-button {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #07c160;
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
  color: #000;
}

.online-status {
  font-size: 12px;
  color: #07c160;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  -webkit-overflow-scrolling: touch;
  scroll-behavior: smooth;
  position: relative;
  height: calc(100vh - 110px); /* Adjusted height: viewport height minus header and input */
}

.message-list {
  display: flex;
  flex-direction: column;
  padding-bottom: 10px;
}

.message-wrapper {
  margin-bottom: 8px;
}

.date-divider {
  text-align: center;
  margin: 10px 0;
  color: #999;
  font-size: 12px;
  background: #cecece;
  padding: 4px 10px;
  border-radius: 10px;
  display: inline-block;
  margin-left: auto;
  margin-right: auto;
}

.time-display {
  text-align: center;
  margin: 10px 0;
  color: #999;
  font-size: 13px;
}

.message {
  display: flex;
  margin-bottom: 8px;
  align-items: flex-start;
  max-width: 85%;
}

.message-sent {
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-received {
  flex-direction: row;
  margin-right: auto;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  overflow: hidden;
  margin: 0 10px;
  flex-shrink: 0;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-content {
  padding: 10px 15px;
  border-radius: 16px;
  word-break: break-word;
  position: relative;
}

.message-sent .message-content {
  background-color: #95ec69;
  color: #000;
  border-top-right-radius: 4px;
  margin-right: 5px;
}

.message-received .message-content {
  background-color: #ffffff;
  color: #000;
  border-top-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  margin-left: 5px;
}

.message-bubble {
  background-color: #ffffff;
  border-radius: 16px;
  border-top-left-radius: 4px;
  padding: 10px 15px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  position: relative;
  max-width: calc(100% - 60px);
  word-break: break-word;
}

.message-bubble.own-bubble {
  background-color: #95ec69;
  border-top-right-radius: 4px;
  border-top-left-radius: 16px;
}

/* Failed message styling */
.message-bubble.own-bubble.failed {
  background-color: #ffeeee;
  border: 1px dashed #ff6b6b;
}

.message-status {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
  text-align: right;
}

.message-status.failed {
  color: #ff6b6b;
}

.retry-button {
  background: none;
  border: none;
  color: #ff6b6b;
  font-size: 12px;
  padding: 2px 8px;
  margin-left: 5px;
  cursor: pointer;
  text-decoration: underline;
}

.message-sender {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.message-time {
  font-size: 11px;
  color: #999;
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

/* Updated chat input styles for desktop and mobile */
.chat-input-bar {
  position: relative; /* Change from sticky to relative */
  bottom: 0;
  padding: 8px;
  border-top: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  background-color: #f6f6f6;
  z-index: 10;
  box-sizing: border-box;
  flex-shrink: 0; /* Prevent input from shrinking */
  width: 100%; /* Full width */
  height: 60px; /* Fixed height */
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
  background-color: #07c160;
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.send-button:disabled {
  background-color: #c9c9c9;
}

/* Empty chat state styling */
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

/* Remove fixed positioning on mobile */
@media (max-width: 768px) {
  .chat-input-bar {
    position: relative;
    bottom: auto;
    padding: 8px;
  }
  
  .chat-body {
    padding-bottom: 8px; /* Reduced padding */
    height: calc(100vh - 110px); /* Adjusted height calculation */
  }
  
  .chat-container {
    height: 100vh;
    max-height: 100vh;
  }
}

@media (min-width: 769px) {
  /* Desktop specific styles */
  .chat-container {
    height: 100vh;
    max-height: 100vh;
  }
  
  .chat-body {
    height: calc(100vh - 110px); /* Adjusted height */
  }
}

.loading-indicator {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.loader {
  border: 4px solid #f3f3f3;
  border-radius: 50%;
  border-top: 4px solid #07c160;
  width: 30px;
  height: 30px;
  animation: spin 1.5s linear infinite;
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
  display: inline-block;
  border: 2px solid #f3f3f3;
  border-radius: 50%;
  border-top: 2px solid #07c160;
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
  color: #07C160;
}

/* Send button - make it more prominent */
.send-button {
  width: 30px;
  height: 30px;
  border: none;
  background-color: #07C160;
  border-radius: 50%;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 5px;
  cursor: pointer;
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
</style> 