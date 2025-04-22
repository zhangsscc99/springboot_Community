<template>
  <div class="message-window h-full flex flex-col">
    <!-- 对话头部 -->
    <div class="message-header bg-white p-4 border-b flex items-center">
      <div v-if="conversation" class="flex items-center">
        <div class="avatar mr-3">
          <img 
            :src="conversation.partnerAvatar || '/default-avatar.png'" 
            :alt="conversation.partnerUsername" 
            class="w-10 h-10 rounded-full"
          >
        </div>
        <div>
          <h3 class="font-medium text-lg">{{ conversation.partnerUsername }}</h3>
        </div>
      </div>
      <div v-else class="text-gray-500">
        请选择一个对话
      </div>
    </div>
    
    <!-- 消息列表 -->
    <div 
      ref="messageList" 
      class="message-list flex-grow p-4 overflow-y-auto"
      :class="{ 'bg-gray-50': !conversation }"
    >
      <div v-if="!conversation" class="h-full flex items-center justify-center text-gray-400">
        <div class="text-center">
          <div class="text-5xl mb-4">💬</div>
          <p>选择一个对话或开始新的对话</p>
        </div>
      </div>
      
      <template v-else>
        <div v-if="loading" class="flex justify-center py-8">
          <div class="loader"></div>
        </div>
        
        <div v-else-if="messages.length === 0" class="text-center py-8 text-gray-500">
          还没有消息，发送第一条消息开始对话吧！
        </div>
        
        <template v-else>
          <div v-if="hasMoreMessages" class="text-center mb-4">
            <button 
              @click="loadMoreMessages"
              class="text-blue-600 hover:text-blue-800 text-sm"
              :disabled="loadingMore"
            >
              {{ loadingMore ? '加载中...' : '加载更多' }}
            </button>
          </div>
          
          <div v-for="(message, index) in displayMessages" :key="message.id" class="mb-4">
            <div class="flex items-end" :class="{'justify-end': isSelf(message)}">
              <div v-if="!isSelf(message)" class="avatar mr-2 flex-shrink-0">
                <img 
                  :src="message.senderAvatar || '/default-avatar.png'" 
                  :alt="message.senderUsername" 
                  class="w-8 h-8 rounded-full"
                >
              </div>
              
              <div 
                class="message-bubble py-2 px-3 rounded-lg max-w-xs sm:max-w-md break-words"
                :class="messageClass(message)"
              >
                <div v-if="showSender(message, index)" class="text-xs text-gray-500 mb-1">
                  {{ message.senderUsername }}
                </div>
                <div>{{ message.content }}</div>
                <div class="text-xs text-gray-500 text-right mt-1">
                  {{ formatTime(message.createdAt) }}
                </div>
              </div>
              
              <div v-if="isSelf(message)" class="avatar ml-2 flex-shrink-0">
                <img 
                  :src="message.senderAvatar || '/default-avatar.png'" 
                  :alt="message.senderUsername" 
                  class="w-8 h-8 rounded-full"
                >
              </div>
            </div>
          </div>
        </template>
      </template>
    </div>
    
    <!-- 输入框 -->
    <div v-if="conversation" class="message-composer bg-white border-t p-3">
      <div class="flex">
        <textarea 
          v-model="newMessage" 
          @keydown.enter.prevent="sendMessage"
          class="flex-grow border rounded-lg p-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="输入消息..."
          rows="2"
        ></textarea>
        <button 
          @click="sendMessage"
          class="ml-2 px-4 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:outline-none"
          :disabled="!newMessage.trim()"
        >
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import MessageService from '@/services/message.service';
import { formatDistance } from 'date-fns';
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
      page: 0,
      size: 20,
      hasMoreMessages: false
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
      if (!this.newMessage.trim() || !this.partnerId) return;
      
      try {
        const response = await MessageService.sendMessage(this.partnerId, this.newMessage.trim());
        
        // 添加发送的消息到列表
        this.messages.push(response.data);
        
        // 清空输入框
        this.newMessage = '';
        
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
    messageClass(message) {
      return {
        'bg-blue-500 text-white': this.isSelf(message),
        'bg-gray-200': !this.isSelf(message)
      };
    },
    formatTime(timestamp) {
      if (!timestamp) return '';
      
      const date = new Date(timestamp);
      return formatDistance(date, new Date(), { 
        addSuffix: true,
        locale: zhCN
      });
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
    }
  }
};
</script>

<style scoped>
.message-list {
  background-color: #f5f5f5;
  /* 渐变背景 */
  background-image: linear-gradient(to bottom, rgba(255,255,255,0.8), rgba(255,255,255,0.5));
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
</style> 