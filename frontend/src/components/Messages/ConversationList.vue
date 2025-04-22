<template>
  <div class="conversation-list">
    <h2 class="text-xl font-bold mb-4">我的消息</h2>
    
    <div v-if="loading" class="flex justify-center py-4">
      <div class="loader"></div>
    </div>
    
    <div v-else-if="conversations.length === 0" class="text-center py-8 text-gray-500">
      还没有任何私信会话
    </div>
    
    <div v-else class="space-y-2">
      <div 
        v-for="conversation in conversations" 
        :key="conversation.id"
        class="conversation-item p-3 rounded-lg cursor-pointer hover:bg-gray-100 transition-colors"
        :class="{ 'bg-blue-50': selectedConversation && selectedConversation.id === conversation.id }"
        @click="selectConversation(conversation)"
      >
        <div class="flex items-center">
          <div class="avatar mr-3">
            <img 
              :src="conversation.partnerAvatar || '/default-avatar.png'" 
              :alt="conversation.partnerUsername" 
              class="w-10 h-10 rounded-full"
            >
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex justify-between">
              <h3 class="font-medium truncate">{{ conversation.partnerUsername }}</h3>
              <span class="text-xs text-gray-500">{{ formatTime(conversation.lastMessageTime) }}</span>
            </div>
            <p class="text-sm text-gray-600 truncate">{{ conversation.lastMessageContent }}</p>
          </div>
          <div v-if="conversation.unreadCount > 0" class="unread-badge ml-2">
            {{ conversation.unreadCount }}
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="hasMoreConversations && !loading" class="text-center mt-4">
      <button 
        @click="loadMoreConversations"
        class="text-blue-600 hover:text-blue-800 text-sm"
      >
        加载更多
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
  border-bottom: 1px solid #eee;
}

.conversation-item:last-child {
  border-bottom: none;
}

.unread-badge {
  background-color: #3490dc;
  color: white;
  border-radius: 50%;
  font-size: 0.75rem;
  min-width: 1.25rem;
  height: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 0.25rem;
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