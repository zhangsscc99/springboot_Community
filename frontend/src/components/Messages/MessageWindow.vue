<template>
  <div class="message-window h-full flex flex-col">
    <!-- 对话头部 -->
    <div class="message-header bg-white p-4 border-b flex items-center">
      <div v-if="conversation" class="flex items-center">
        <div class="avatar-container mr-3">
          <img 
            :src="conversation.partnerAvatar || '/default-avatar.png'" 
            :alt="conversation.partnerUsername" 
            class="w-10 h-10 rounded-full border border-gray-200 object-cover"
          >
        </div>
        <div>
          <h3 class="font-medium text-lg text-gray-800">{{ conversation.partnerUsername }}</h3>
          <div v-if="isTyping" class="typing-indicator text-xs text-gray-500">
            <span>正在输入</span>
            <span class="dots">...</span>
          </div>
        </div>
      </div>
      <div v-else class="empty-conversation-header flex flex-col items-center justify-center w-full py-8">
        <div class="icon-container mb-2">
          <i class="fas fa-comments text-3xl text-gray-300"></i>
        </div>
        <h3 class="text-lg text-gray-600 font-medium">请选择一个对话</h3>
        <p class="text-sm text-gray-500">或开始新的对话</p>
      </div>
    </div>
    
    <!-- 消息列表 -->
    <div 
      ref="messageList" 
      class="message-list flex-grow p-4 overflow-y-auto"
    >
      <div v-if="loading" class="flex justify-center py-8">
        <div class="loader"></div>
      </div>
      
      <div v-else-if="messages.length === 0" class="text-center py-8">
        <div class="empty-messages max-w-md mx-auto">
          <i class="far fa-comment-dots text-4xl text-gray-300 mb-4"></i>
          <h3 class="text-base font-medium text-gray-700 mb-2">{{ conversation.isNew ? '开始新对话' : '还没有消息' }}</h3>
          <p class="text-sm text-gray-500 mb-3">
            <template v-if="conversation.isNew">
              这是你与 {{ conversation.partnerUsername }} 的首次对话，发送一条消息开始聊天吧！
            </template>
            <template v-else>
              发送第一条消息开始对话吧！
            </template>
          </p>
          
          <!-- 快捷回复建议 -->
          <div v-if="conversation.isNew" class="mt-4 flex flex-col gap-2">
            <button 
              v-for="(suggestion, index) in messageSuggestions" 
              :key="index"
              @click="useMessageSuggestion(suggestion)"
              class="px-3 py-2 text-sm text-left text-gray-700 bg-gray-50 hover:bg-gray-100 rounded-lg transition-colors"
            >
              {{ suggestion }}
            </button>
          </div>
        </div>
      </div>
      
      <template v-else>
        <div v-if="hasMoreMessages" class="text-center mb-4">
          <button 
            @click="loadMoreMessages"
            class="px-3 py-1 text-xs text-gray-500 bg-gray-100 rounded-full"
            :disabled="loadingMore"
          >
            {{ loadingMore ? '加载中...' : '查看更多消息' }}
          </button>
        </div>
        
        <div v-for="(message, index) in displayMessages" :key="message.id">
          <!-- 日期分割线 -->
          <div class="message-date-divider" v-if="showDateDivider(message, index)">
            <span class="date-text">{{ formatDate(message.createdAt) }}</span>
          </div>
          
          <!-- 消息项 -->
          <div class="message-item mb-4" :class="{'self-message': isSelf(message)}">
            <!-- 用户头像和消息内容 -->
            <div class="flex" :class="{'justify-end': isSelf(message)}">
              <!-- 头像 - 非自己发送的消息显示在左侧 -->
              <div v-if="!isSelf(message)" class="message-avatar mr-2">
                <img 
                  :src="message.senderAvatar || '/default-avatar.png'" 
                  :alt="message.senderUsername" 
                  class="w-10 h-10 rounded-full object-cover"
                >
              </div>
              
              <!-- 消息内容 -->
              <div class="message-bubble-wrapper max-w-[70%]">
                <!-- 发送者名称 - 只有非自己发送且需要显示时才显示 -->
                <div v-if="showSender(message, index)" class="text-xs text-gray-500 mb-1 ml-2">
                  {{ message.senderUsername }}
                </div>
                
                <!-- 消息气泡 -->
                <div 
                  class="message-bubble py-2 px-3 inline-block break-words"
                  :class="messageClass(message)"
                >
                  <span class="message-content">{{ message.content }}</span>
                </div>
              </div>
              
              <!-- 头像 - 自己发送的消息显示在右侧 -->
              <div v-if="isSelf(message)" class="message-avatar ml-2">
                <img 
                  :src="message.senderAvatar || '/default-avatar.png'" 
                  :alt="message.senderUsername" 
                  class="w-10 h-10 rounded-full object-cover"
                >
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
    
    <!-- 输入框 -->
    <div class="message-composer bg-gray-50 border-t p-2">
      <div class="flex items-center bg-white rounded-full px-4 py-2">
        <textarea 
          ref="messageInput"
          v-model="newMessage" 
          @keydown.enter.prevent="sendMessage"
          class="flex-grow bg-transparent border-none outline-none resize-none text-sm h-8 leading-normal py-1"
          placeholder="输入消息..."
          rows="1"
        ></textarea>
        
        <button 
          @click="sendMessage"
          class="ml-2 w-8 h-8 rounded-full flex items-center justify-center"
          :class="[newMessage.trim() && !isSending ? 'bg-green-500 text-white' : 'bg-gray-200 text-gray-400']"
          :disabled="!newMessage.trim() || isSending"
        >
          <i v-if="isSending" class="fas fa-spinner fa-spin text-sm"></i>
          <i v-else class="fas fa-paper-plane text-sm"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import MessageService from '@/services/message.service';
import { formatDistance, format } from 'date-fns';
import { zhCN } from 'date-fns/locale';

export default {
  name: 'MessageWindow',
  props: {
    conversation: {
      type: Object,
      default: null
    },
    currentUserId: {
      type: Number,
      required: true
    }
  },
  data() {
    return {
      messages: [],
      newMessage: '',
      loading: false,
      loadingMore: false,
      isSending: false,
      isTyping: false,
      page: 0,
      size: 20,
      hasMoreMessages: false,
      lastTypingTimeout: null,
      messageSuggestions: [
        '你好，很高兴认识你！',
        '我看到了你的帖子，想和你聊聊。',
        '可以请教你一个问题吗？',
        '我对你分享的内容很感兴趣'
      ]
    };
  },
  computed: {
    partnerId() {
      return this.conversation ? this.conversation.partnerId : null;
    },
    // 按时间升序排序的消息（旧的在上，新的在下）
    displayMessages() {
      return [...this.messages].reverse();
    }
  },
  watch: {
    // 当选择的会话变化时，加载新的消息历史
    partnerId(newPartnerId) {
      if (newPartnerId) {
        this.resetMessages();
        this.fetchMessages();
      }
    }
  },
  methods: {
    resetMessages() {
      this.messages = [];
      this.page = 0;
      this.hasMoreMessages = false;
      this.newMessage = '';
    },
    async fetchMessages() {
      if (!this.partnerId) return;
      
      try {
        this.loading = true;
        const response = await MessageService.getMessageHistory(this.partnerId, this.page, this.size);
        
        // 分页数据处理
        const newMessages = response.data.content || [];
        this.messages = [...newMessages, ...this.messages];
        this.hasMoreMessages = !response.data.first; // 如果不是第一页，则表示有更多消息
        
        // 滚动到底部（第一次加载消息时）
        if (this.page === 0) {
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        }
      } catch (error) {
        console.error('获取消息历史失败:', error);
      } finally {
        this.loading = false;
      }
    },
    async loadMoreMessages() {
      if (this.loadingMore) return;
      
      try {
        this.loadingMore = true;
        this.page += 1;
        await this.fetchMessages();
      } finally {
        this.loadingMore = false;
      }
    },
    async sendMessage() {
      if (!this.newMessage.trim() || !this.partnerId || this.isSending) return;
      
      const messageContent = this.newMessage.trim();
      this.newMessage = ''; // 立即清空输入框
      this.isSending = true;
      
      try {
        const response = await MessageService.sendMessage(this.partnerId, messageContent);
        
        // 添加发送的消息到列表
        this.messages.push(response.data);
        
        // 滚动到底部
        this.$nextTick(() => {
          this.scrollToBottom();
        });
        
        // 通知父组件更新会话列表
        this.$emit('message-sent', {
          partnerId: this.partnerId,
          message: response.data
        });
      } catch (error) {
        console.error('发送消息失败:', error);
        // 发送失败，恢复消息内容
        this.newMessage = messageContent;
      } finally {
        this.isSending = false;
      }
    },
    scrollToBottom() {
      if (this.$refs.messageList) {
        this.$refs.messageList.scrollTop = this.$refs.messageList.scrollHeight;
      }
    },
    isSelf(message) {
      return message.senderId === this.currentUserId;
    },
    // 判断是否显示发送者名称（对于连续的同一发送者的消息，只显示第一条的名称）
    showSender(message, index) {
      if (this.isSelf(message)) return false;
      if (index === 0) return true;
      
      const prevMessage = this.displayMessages[index - 1];
      return prevMessage.senderId !== message.senderId;
    },
    // 判断是否显示日期分隔线
    showDateDivider(message, index) {
      if (index === 0) return true;
      
      const currentDate = new Date(message.createdAt).toLocaleDateString();
      const prevDate = new Date(this.displayMessages[index - 1].createdAt).toLocaleDateString();
      
      return currentDate !== prevDate;
    },
    messageClass(message) {
      if (this.isSelf(message)) {
        return 'self-bubble bg-green-500 text-white rounded-xl rounded-tr-none';
      } else {
        return 'other-bubble bg-white text-gray-800 rounded-xl rounded-tl-none';
      }
    },
    formatTime(timestamp) {
      if (!timestamp) return '';
      
      const date = new Date(timestamp);
      return format(date, 'HH:mm');
    },
    formatDate(timestamp) {
      if (!timestamp) return '';
      
      const date = new Date(timestamp);
      
      // 判断是否是今天
      const today = new Date();
      if (
        date.getDate() === today.getDate() &&
        date.getMonth() === today.getMonth() &&
        date.getFullYear() === today.getFullYear()
      ) {
        return '今天';
      }
      
      return format(date, 'yyyy年MM月dd日', { locale: zhCN });
    },
    // 添加一条新接收的消息
    addReceivedMessage(message) {
      if (message.senderId === this.partnerId) {
        this.messages.push(message);
        
        // 滚动到底部
        this.$nextTick(() => {
          this.scrollToBottom();
        });
        
        // 标记为已读
        MessageService.markAsRead(this.partnerId);
      }
    },
    
    // 聚焦到输入框
    focusInput() {
      this.$nextTick(() => {
        if (this.$refs.messageInput) {
          this.$refs.messageInput.focus();
        }
      });
    },
    
    // 使用消息建议
    useMessageSuggestion(suggestion) {
      this.newMessage = suggestion;
      this.focusInput();
    }
  }
};
</script>

<style scoped>
.message-list {
  background-color: #f0f0f0;
  background-image: url("data:image/svg+xml,%3Csvg width='64' height='64' viewBox='0 0 64 64' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M8 16c4.418 0 8-3.582 8-8s-3.582-8-8-8-8 3.582-8 8 3.582 8 8 8zm0-2c3.314 0 6-2.686 6-6s-2.686-6-6-6-6 2.686-6 6 2.686 6 6 6zm33.414-6l5.95-5.95L45.95.636 40 6.586 34.05.636 32.636 2.05 38.586 8l-5.95 5.95 1.414 1.414L40 9.414l5.95 5.95 1.414-1.414L41.414 8zM40 48c4.418 0 8-3.582 8-8s-3.582-8-8-8-8 3.582-8 8 3.582 8 8 8zm0-2c3.314 0 6-2.686 6-6s-2.686-6-6-6-6 2.686-6 6 2.686 6 6 6zM9.414 40l5.95-5.95-1.414-1.414L8 38.586l-5.95-5.95L.636 34.05 6.586 40l-5.95 5.95 1.414 1.414L8 41.414l5.95 5.95 1.414-1.414L9.414 40z' fill='%23dddddd' fill-opacity='0.1' fill-rule='evenodd'/%3E%3C/svg%3E");
}

.message-date-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 1rem 0;
}

.date-text {
  background-color: rgba(180, 180, 180, 0.5);
  color: #fff;
  font-size: 0.7rem;
  padding: 0.15rem 0.5rem;
  border-radius: 10px;
}

.message-item {
  position: relative;
  margin-bottom: 12px;
}

.message-bubble {
  position: relative;
  max-width: 100%;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.self-bubble {
  float: right;
}

.other-bubble {
  float: left;
}

.message-content {
  word-break: break-word;
}

.loader {
  border: 3px solid #f3f3f3;
  border-radius: 50%;
  border-top: 3px solid #3490dc;
  width: 24px;
  height: 24px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.message-composer {
  border-top: 1px solid #e5e5e5;
}
</style> 