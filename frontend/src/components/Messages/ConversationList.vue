<template>
  <div class="conversation-list p-4">
    <div v-if="loading" class="flex justify-center py-8">
      <div class="loader"></div>
    </div>
    
    <div v-else-if="conversations.length === 0" class="text-center py-12 px-4">
      <div class="empty-state">
        <div class="icon-container mb-4">
          <i class="fas fa-comments text-4xl text-gray-300"></i>
        </div>
        <h3 class="text-lg font-medium text-gray-600 mb-2">还没有任何私信会话</h3>
        <p class="text-sm text-gray-500">开始与社区成员沟通吧</p>
      </div>
    </div>
    
    <div v-else class="space-y-2">
      <div 
        v-for="conversation in conversations" 
        :key="conversation.id"
        class="conversation-item p-3 rounded-xl cursor-pointer transition-all duration-200"
        :class="{ 
          'bg-gradient-to-r from-blue-50 to-indigo-50 border-l-4 border-blue-500 shadow-sm': selectedConversation && selectedConversation.id === conversation.id,
          'hover:bg-gray-100': !selectedConversation || selectedConversation.id !== conversation.id
        }"
        @click="selectConversation(conversation)"
      >
        <div class="flex items-center">
          <div class="avatar-container relative mr-3">
            <img 
              :src="conversation.partnerAvatar || '/default-avatar.png'" 
              :alt="conversation.partnerUsername" 
              class="w-10 h-10 rounded-full border border-gray-200 object-cover"
            >
            <div v-if="conversation.unreadCount > 0" class="status-indicator"></div>
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex justify-between items-center">
              <h3 class="font-medium truncate text-gray-800">{{ conversation.partnerUsername }}</h3>
              <span class="text-xs text-gray-500 whitespace-nowrap ml-2">{{ formatTime(conversation.lastMessageTime) }}</span>
            </div>
            <div class="flex items-center justify-between">
              <p class="text-sm text-gray-600 truncate pr-2">{{ conversation.lastMessageContent || '开始聊天...' }}</p>
              <div v-if="conversation.unreadCount > 0" class="unread-badge flex-shrink-0">
                {{ conversation.unreadCount > 99 ? '99+' : conversation.unreadCount }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="hasMoreConversations && !loading" class="text-center mt-6">
      <button 
        @click="loadMoreConversations"
        class="px-4 py-2 text-sm text-blue-600 hover:text-blue-800 rounded-full border border-blue-200 hover:border-blue-400 bg-white hover:bg-blue-50 transition-colors"
      >
        加载更多会话
      </button>
    </div>
  </div>
</template>

<script>
import MessageService from '@/services/message.service';
import { formatDistance } from 'date-fns';
import { zhCN } from 'date-fns/locale';

export default {
  name: 'ConversationList',
  props: {
    selectedConversation: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      conversations: [],
      loading: true,
      page: 0,
      size: 20,
      hasMoreConversations: false
    };
  },
  created() {
    this.fetchConversations();
  },
  methods: {
    async fetchConversations() {
      try {
        this.loading = true;
        const response = await MessageService.getConversations(this.page, this.size);
        
        this.conversations = [...this.conversations, ...response.data.content];
        this.hasMoreConversations = !response.data.last;
        
        if (this.page === 0 && this.conversations.length > 0 && !this.selectedConversation) {
          // 自动选择第一个会话
          this.selectConversation(this.conversations[0]);
        }
      } catch (error) {
        console.error('获取会话列表失败:', error);
      } finally {
        this.loading = false;
      }
    },
    loadMoreConversations() {
      this.page += 1;
      this.fetchConversations();
    },
    selectConversation(conversation) {
      this.$emit('conversation-selected', conversation);
    },
    formatTime(timestamp) {
      if (!timestamp) return '';
      
      const date = new Date(timestamp);
      return formatDistance(date, new Date(), { 
        addSuffix: true,
        locale: zhCN
      });
    },
    // 更新未读消息数
    updateUnreadCount(partnerId, count) {
      const conversation = this.conversations.find(c => c.partnerId === partnerId);
      if (conversation) {
        conversation.unreadCount = count;
      }
    },
    // 添加新会话或更新现有会话
    updateConversation(newConversation) {
      const index = this.conversations.findIndex(c => c.id === newConversation.id);
      
      if (index !== -1) {
        // 更新已有会话
        this.conversations.splice(index, 1);
      }
      
      // 添加到顶部
      this.conversations.unshift(newConversation);
    }
  }
};
</script>

<style scoped>
.conversation-item {
  border-bottom: 1px solid #f0f0f0;
  position: relative;
}

.conversation-item:last-child {
  border-bottom: none;
}

.unread-badge {
  background: linear-gradient(135deg, #3490dc, #6366f1);
  color: white;
  border-radius: 12px;
  font-size: 0.7rem;
  min-width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.status-indicator {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background-color: #48bb78;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.avatar-container {
  position: relative;
}

.loader {
  border: 3px solid #f3f3f3;
  border-radius: 50%;
  border-top: 3px solid #3490dc;
  width: 24px;
  height: 24px;
  animation: spin 1s linear infinite;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
  color: #718096;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style> 