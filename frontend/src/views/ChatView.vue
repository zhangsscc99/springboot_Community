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
    <div class="messages-area" ref="messagesContainer">
      <div class="loading-indicator" v-if="loading">
        <div class="loader"></div>
      </div>

      <div v-else-if="messages.length === 0" class="empty-state">
        <div class="empty-message">
          <i class="fas fa-comments"></i>
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

          <!-- Message bubble -->
          <div class="message" :class="{ 'own-message': isOwnMessage(message) }">
            <div class="avatar" v-if="!isOwnMessage(message)">
              <img :src="message.senderAvatar || '/default-avatar.png'" :alt="message.senderUsername">
            </div>
            <div class="message-bubble" :class="{ 'own-bubble': isOwnMessage(message) }">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-time">{{ formatMessageTime(message.createdAt) }}</div>
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

    <!-- Input Area -->
    <div class="input-area">
      <div class="tools">
        <i class="fas fa-smile emoji-button" @click="toggleEmojiPicker"></i>
        <i class="fas fa-image image-button" @click="openImagePicker"></i>
      </div>
      <div class="input-wrapper">
        <textarea 
          class="message-input" 
          v-model="newMessage"
          placeholder="输入消息..." 
          @keyup.enter.exact="sendMessage"
          @focus="scrollToBottom"
          ref="messageInput"
        ></textarea>
      </div>
      <div class="send-button" :class="{ 'active': newMessage.trim().length > 0 }" @click="sendMessage">
        <i class="fas fa-paper-plane"></i>
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
    // Get partner ID from route params
    this.partnerId = parseInt(this.$route.params.id);
    this.loadUserInfo();
    this.fetchConversationDetails();
    this.fetchMessages();
    this.setupSseConnection();
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
        const response = await MessageService.sendMessage(this.partnerId, content);
        if (response.data) {
          // Add the sent message to the list
          this.messages.push(response.data);
          // Clear input
          this.newMessage = '';
          // Scroll to bottom
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        }
      } catch (error) {
        console.error('Failed to send message:', error);
        this.$message.error('发送失败，请重试');
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
      if (this.$refs.messagesContainer) {
        this.$refs.messagesContainer.scrollTop = this.$refs.messagesContainer.scrollHeight;
      }
    },
    shouldShowDateDivider(message, index) {
      if (index === 0) return true;
      
      const prevMessage = this.messages[index - 1];
      return !isSameDay(new Date(message.createdAt), new Date(prevMessage.createdAt));
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
      return format(date, 'HH:mm');
    },
    goBack() {
      this.$router.push('/messages');
    },
    showMenu() {
      // TODO: Show options menu (delete conversation, block user, etc.)
      this.$message.info('功能开发中');
    },
    toggleEmojiPicker() {
      // TODO: Implement emoji picker
      this.$message.info('表情选择器开发中');
    },
    openImagePicker() {
      // TODO: Implement image upload
      this.$message.info('图片上传功能开发中');
    }
  }
};
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f1f1f1;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background-color: #fff;
  border-bottom: 1px solid #eaeaea;
  z-index: 10;
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
  font-size: 18px;
  font-weight: 500;
  color: #000;
}

.online-status {
  font-size: 12px;
  color: #07c160;
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background-color: #f1f1f1;
}

.message-list {
  display: flex;
  flex-direction: column;
}

.message-wrapper {
  margin-bottom: 16px;
}

.date-divider {
  text-align: center;
  margin: 20px 0;
  color: #999;
  font-size: 12px;
  background: #f1f1f1;
  padding: 6px 12px;
  border-radius: 10px;
  display: inline-block;
  margin-left: auto;
  margin-right: auto;
}

.message {
  display: flex;
  margin-bottom: 8px;
  align-items: flex-start;
}

.own-message {
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  overflow: hidden;
  margin: 0 12px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-bubble {
  max-width: 70%;
  padding: 10px 16px;
  border-radius: 4px;
  background-color: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  position: relative;
}

.own-bubble {
  background-color: #95ec69;
}

.message-content {
  font-size: 16px;
  color: #333;
  word-break: break-word;
  white-space: pre-wrap;
}

.message-time {
  font-size: 11px;
  color: #999;
  text-align: right;
  margin-top: 4px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.empty-message {
  text-align: center;
  color: #999;
}

.empty-message i {
  font-size: 48px;
  margin-bottom: 16px;
  color: #ddd;
}

.empty-message p {
  margin: 4px 0;
}

.empty-message .hint {
  font-size: 14px;
  color: #aaa;
}

.input-area {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background-color: #fff;
  border-top: 1px solid #eaeaea;
}

.tools {
  display: flex;
  align-items: center;
}

.emoji-button, .image-button {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  cursor: pointer;
  margin-right: 8px;
}

.input-wrapper {
  flex: 1;
  border-radius: 20px;
  background-color: #f1f1f1;
  padding: 8px 16px;
  margin: 0 8px;
}

.message-input {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  font-size: 16px;
  resize: none;
  max-height: 100px;
  line-height: 20px;
}

.send-button {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ddd;
  color: #fff;
  cursor: not-allowed;
}

.send-button.active {
  background-color: #07c160;
  cursor: pointer;
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
  padding: 8px 16px;
  background-color: #f1f1f1;
  color: #666;
  border: 1px solid #ddd;
  border-radius: 16px;
  font-size: 14px;
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
</style> 