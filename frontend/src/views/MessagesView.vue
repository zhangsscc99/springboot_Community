<template>
  <div class="messages-page min-h-screen bg-white">
    <div class="container mx-auto py-4 px-4 lg:px-8">
      <div class="messages-container bg-white rounded-lg shadow-lg border overflow-hidden">
        <div class="flex flex-col md:flex-row h-[80vh]">
          <!-- 会话列表 -->
          <div class="conversation-list-container w-full md:w-1/3 md:border-r">
            <ConversationList 
              ref="conversationList"
              :selected-conversation="selectedConversation"
              @conversation-selected="handleConversationSelected"
            />
          </div>
          
          <!-- 消息窗口 -->
          <div class="message-window-container w-full md:w-2/3">
            <MessageWindow 
              ref="messageWindow"
              :conversation="selectedConversation"
              :current-user-id="currentUserId"
              @message-sent="handleMessageSent"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import ConversationList from '@/components/Messages/ConversationList.vue';
import MessageWindow from '@/components/Messages/MessageWindow.vue';
import MessageService from '@/services/message.service';
import { mapState } from 'vuex';

export default {
  name: 'MessagesView',
  components: {
    ConversationList,
    MessageWindow
  },
  data() {
    return {
      selectedConversation: null,
      eventSource: null,
      unreadPollingInterval: null
    };
  },
  computed: {
    ...mapState({
      user: state => state.auth?.user
    }),
    currentUserId() {
      return this.user ? this.user.id : null;
    },
    isAuthenticated() {
      return !!this.user;
    }
  },
  created() {
    // 如果用户未登录，重定向到登录页
    if (!this.isAuthenticated) {
      this.$router.push('/login');
      return;
    }
    
    // 检查URL中是否有partnerId参数
    const partnerId = parseInt(this.$route.query.partnerId);
    if (partnerId) {
      this.loadConversationWithUser(partnerId);
    }
  },
  mounted() {
    // 确保用户已登录再设置连接
    if (this.isAuthenticated) {
      this.setupSSEConnection();
      this.startUnreadPolling();
    }
  },
  beforeUnmount() {
    this.closeSSEConnection();
    this.stopUnreadPolling();
  },
  methods: {
    handleConversationSelected(conversation) {
      this.selectedConversation = conversation;
      
      // 更新URL，但不重新加载页面
      this.$router.replace({ 
        query: { 
          partnerId: conversation.partnerId 
        }
      });
    },
    handleMessageSent(event) {
      // 更新会话列表
      if (this.$refs.conversationList) {
        const { partnerId, message } = event;
        
        // 如果是新会话，需要获取会话详情
        if (!this.selectedConversation || this.selectedConversation.partnerId !== partnerId) {
          this.loadConversationWithUser(partnerId);
        } else {
          // 更新现有会话的最后一条消息
          const updatedConversation = { ...this.selectedConversation };
          updatedConversation.lastMessageContent = message.content;
          updatedConversation.lastMessageTime = message.createdAt;
          
          this.$refs.conversationList.updateConversation(updatedConversation);
        }
      }
    },
    // 根据用户ID加载或创建会话
    async loadConversationWithUser(partnerId) {
      try {
        // 先尝试获取现有会话
        const response = await MessageService.getConversationDetails(partnerId);
        let conversation = response.data;
        
        // 如果没有现有会话，创建一个临时会话对象
        if (!conversation) {
          // 这里需要先获取用户信息
          // 简化处理，仅创建基本会话对象
          conversation = {
            id: null,
            partnerId: partnerId,
            partnerUsername: `用户 ${partnerId}`, // 理想情况下应该获取真实用户名
            partnerAvatar: null,
            lastMessageContent: null,
            lastMessageTime: null,
            unreadCount: 0
          };
        }
        
        this.selectedConversation = conversation;
        
        // 如果会话列表组件已加载，更新选中状态
        if (this.$refs.conversationList) {
          const existingConversation = this.$refs.conversationList.conversations.find(
            c => c.partnerId === partnerId
          );
          
          if (existingConversation) {
            this.$refs.conversationList.selectConversation(existingConversation);
          } else {
            // 如果会话不在列表中，添加到列表
            this.$refs.conversationList.updateConversation(conversation);
          }
        }
        
      } catch (error) {
        console.error('加载会话失败:', error);
      }
    },
    // 设置SSE连接
    setupSSEConnection() {
      if (!this.isAuthenticated) return;
      
      try {
        this.eventSource = MessageService.createSseConnection();
        
        // 监听私信事件
        this.eventSource.addEventListener('privateMessage', event => {
          const message = JSON.parse(event.data);
          
          // 如果当前正在查看与发送者的对话，则添加消息
          if (this.$refs.messageWindow && 
              this.selectedConversation && 
              message.senderId === this.selectedConversation.partnerId) {
            this.$refs.messageWindow.addReceivedMessage(message);
          }
          
          // 更新会话列表
          if (this.$refs.conversationList) {
            this.updateConversationWithMessage(message);
          }
        });
        
        // 监听未读计数更新事件
        this.eventSource.addEventListener('unreadCount', event => {
          const count = parseInt(event.data);
          if (count === -1) {
            // 需要查询最新未读数
            this.fetchUnreadCount();
          }
        });
        
        // 错误处理
        this.eventSource.onerror = error => {
          console.error('SSE连接错误:', error);
          this.closeSSEConnection();
          
          // 5秒后尝试重新连接
          setTimeout(() => {
            this.setupSSEConnection();
          }, 5000);
        };
        
      } catch (error) {
        console.error('建立SSE连接失败:', error);
      }
    },
    closeSSEConnection() {
      if (this.eventSource) {
        this.eventSource.close();
        this.eventSource = null;
      }
    },
    // 定期轮询未读消息数
    startUnreadPolling() {
      this.unreadPollingInterval = setInterval(() => {
        this.fetchUnreadCount();
      }, 60000); // 每分钟查询一次
      
      // 立即查询一次
      this.fetchUnreadCount();
    },
    stopUnreadPolling() {
      if (this.unreadPollingInterval) {
        clearInterval(this.unreadPollingInterval);
        this.unreadPollingInterval = null;
      }
    },
    async fetchUnreadCount() {
      if (!this.isAuthenticated) return;
      
      try {
        const response = await MessageService.getUnreadCount();
        const totalUnread = response.data;
        
        // 通知全局消息未读数（可以通过Vuex存储）
        this.$store.commit('setUnreadMessageCount', totalUnread);
      } catch (error) {
        console.error('获取未读消息数失败:', error);
      }
    },
    // 根据新消息更新会话
    async updateConversationWithMessage(message) {
      // 获取对方ID
      const partnerId = message.senderId === this.currentUserId 
        ? message.receiverId 
        : message.senderId;
      
      try {
        // 获取最新的会话信息
        const response = await MessageService.getConversationDetails(partnerId);
        const conversation = response.data;
        
        if (conversation && this.$refs.conversationList) {
          this.$refs.conversationList.updateConversation(conversation);
        }
      } catch (error) {
        console.error('更新会话失败:', error);
      }
    }
  }
};
</script>

<style scoped>
.messages-container {
  height: calc(100vh - 120px);
  min-height: 500px;
}

@media (max-width: 768px) {
  .conversation-list-container {
    height: 40vh;
    overflow-y: auto;
  }
  
  .message-window-container {
    height: 60vh;
  }
}
</style> 
