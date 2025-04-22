<template>
  <div class="messages-container">
    <!-- 顶部导航栏 -->
    <div class="messages-header">
      <div class="title">消息</div>
      <div class="action-buttons">
        <i class="iconfont icon-plus" @click="showActionMenu"></i>
      </div>
    </div>

    <!-- 系统分类通知 -->
    <div class="notification-categories">
      <div class="category-item" @click="navigateToCategory('likes')">
        <div class="category-icon like-icon">
          <i class="fas fa-heart"></i>
        </div>
        <div class="category-text">赞和收藏</div>
        <div class="badge-container" v-if="notifications.likes.unreadCount > 0">
          <div class="badge">{{ notifications.likes.unreadCount > 99 ? '99+' : notifications.likes.unreadCount }}</div>
        </div>
      </div>
      
      <div class="category-item" @click="navigateToCategory('follows')">
        <div class="category-icon follow-icon">
          <i class="fas fa-user"></i>
        </div>
        <div class="category-text">新增关注</div>
        <div class="badge-container" v-if="notifications.follows.unreadCount > 0">
          <div class="badge">{{ notifications.follows.unreadCount > 99 ? '99+' : notifications.follows.unreadCount }}</div>
        </div>
      </div>
      
      <div class="category-item" @click="navigateToCategory('comments')">
        <div class="category-icon comment-icon">
          <i class="fas fa-comment"></i>
        </div>
        <div class="category-text">评论和@</div>
        <div class="badge-container" v-if="notifications.comments.unreadCount > 0">
          <div class="badge">{{ notifications.comments.unreadCount > 99 ? '99+' : notifications.comments.unreadCount }}</div>
        </div>
      </div>
      
      <div class="category-item" @click="navigateToCategory('system')">
        <div class="category-icon system-icon">
          <i class="fas fa-bell"></i>
        </div>
        <div class="category-text">系统通知</div>
        <div class="badge-container" v-if="notifications.system.unreadCount > 0">
          <div class="badge">{{ notifications.system.unreadCount > 99 ? '99+' : notifications.system.unreadCount }}</div>
        </div>
      </div>
    </div>

    <!-- 消息会话列表 -->
    <div class="message-list-header">
      <span class="title-text">聊天</span>
      <!-- <span class="discover-group" @click="navigateToDiscoverGroups">
        <i class="iconfont icon-group"></i>
        发现群聊
      </span> -->
    </div>

    <!-- 会话列表 -->
    <div class="conversation-list">
      <div 
        v-for="conversation in conversations" 
        :key="conversation.id" 
        @click="navigateToConversation(conversation.id)"
        class="conversation-wrapper"
      >
        <conversation-item 
          :conversation="conversation"
        />
      </div>
      <div v-if="conversations.length === 0" class="empty-state">
        <div class="empty-icon">
          <i class="iconfont icon-message"></i>
        </div>
        <div class="empty-text">暂无聊天会话</div>
        <div class="empty-hint">点击右上角加号开始新的对话</div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue';
import ConversationItem from '@/components/ConversationItem.vue';
import MessageService from '@/services/message.service';
import NotificationService from '@/services/notification.service';

export default {
  name: 'MessagesView',
  components: {
    ConversationItem
  },
  data() {
    return {
      conversations: [],
      loading: false,
      notifications: {
        likes: {
          id: 'likes',
          title: '赞和收藏',
          content: '查看你收到的赞和收藏',
          type: 'like',
          unreadCount: 0
        },
        follows: {
          id: 'follows',
          title: '新增关注',
          content: '查看谁关注了你',
          type: 'follow',
          unreadCount: 0
        },
        comments: {
          id: 'comments',
          title: '评论和@',
          content: '查看收到的评论和提及',
          type: 'comment',
          unreadCount: 0
        },
        system: {
          id: 'system',
          title: '系统通知',
          content: '查看系统消息',
          type: 'system',
          unreadCount: 0
        }
      },
      eventSource: null
    };
  },
  created() {
    this.fetchConversations();
    this.fetchNotifications();
    this.setupSseConnection();
  },
  beforeUnmount() {
    this.closeSseConnection();
  },
  methods: {
    async fetchConversations() {
      try {
        this.loading = true;
        const response = await MessageService.getConversations();
        if (response.data && response.data.content) {
          // Transform backend data to match our component structure
          this.conversations = response.data.content.map(conv => ({
            id: conv.id,
            name: conv.partnerUsername,
            avatar: conv.partnerAvatar,
            lastMessage: conv.lastMessageContent,
            lastMessageTime: conv.lastMessageTime,
            unreadCount: conv.unreadCount
          }));
        }
      } catch (error) {
        console.error('Failed to fetch conversations:', error);
      } finally {
        this.loading = false;
      }
    },
    
    async fetchNotifications() {
      try {
        const response = await NotificationService.getUnreadCountsByType();
        if (response.data) {
          this.notifications.likes.unreadCount = response.data.likeCount || 0;
          this.notifications.follows.unreadCount = response.data.followCount || 0;
          this.notifications.comments.unreadCount = response.data.commentCount || 0;
          this.notifications.system.unreadCount = response.data.systemCount || 0;
        }
      } catch (error) {
        console.error('Failed to fetch notifications:', error);
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
        
        // Listen for unread count updates
        this.eventSource.addEventListener('unreadCount', event => {
          const unreadCount = JSON.parse(event.data);
          if (unreadCount === -1) {
            // Need to refresh our data
            this.fetchConversations();
          }
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
      // Find if conversation exists
      const conversationIndex = this.conversations.findIndex(
        conv => conv.name === message.senderUsername || conv.name === message.receiverUsername
      );
      
      if (conversationIndex !== -1) {
        // Update existing conversation
        const conversation = this.conversations[conversationIndex];
        conversation.lastMessage = message.content;
        conversation.lastMessageTime = message.createdAt;
        conversation.unreadCount += 1;
        
        // Move conversation to top
        this.conversations.splice(conversationIndex, 1);
        this.conversations.unshift(conversation);
      } else {
        // Add new conversation
        const isIncoming = message.receiverId !== this.getCurrentUserId();
        const newConversation = {
          id: Date.now(), // Temporary ID, will be replaced when fetching full list
          name: isIncoming ? message.senderUsername : message.receiverUsername,
          avatar: isIncoming ? message.senderAvatar : message.receiverAvatar,
          lastMessage: message.content,
          lastMessageTime: message.createdAt,
          unreadCount: 1
        };
        this.conversations.unshift(newConversation);
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
    
    navigateToConversation(conversationId) {
      this.$router.push(`/chat/${conversationId}`);
    },
    
    navigateToCategory(category) {
      this.$router.push(`/notifications/${category}`);
    },
    
    navigateToDiscoverGroups() {
      this.$router.push('/discover-groups');
    },
    
    showActionMenu() {
      this.$message.info('功能开发中');
    }
  }
};
</script>

<style scoped>
.messages-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #ededed;
}

.messages-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background-color: #fff;
  border-bottom: 1px solid #eaeaea;
  position: sticky;
  top: 0;
  z-index: 10;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #000;
}

.action-buttons i {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  color: #07c160;
  transition: background-color 0.2s;
}

.action-buttons i:active {
  background-color: #f0f0f0;
}

.notification-categories {
  display: flex;
  background-color: #fff;
  padding: 20px 0;
  justify-content: space-around;
  border-bottom: 1px solid #eaeaea;
  margin-bottom: 8px;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: transform 0.2s;
  padding: 0 8px;
  position: relative;
}

.category-item:active {
  transform: scale(0.95);
}

.category-icon {
  width: 60px;
  height: 60px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.category-icon i {
  font-size: 26px;
}

.like-icon {
  background-color: #ffeeee;
}

.like-icon i {
  color: #ff4757;
}

.follow-icon {
  background-color: #eef5ff;
}

.follow-icon i {
  color: #2e86de;
}

.comment-icon {
  background-color: #e8fff0;
}

.comment-icon i {
  color: #2ed573;
}

.system-icon {
  background-color: #fff8ee;
}

.system-icon i {
  color: #ff9f43;
}

.category-text {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.badge-container {
  position: absolute;
  top: -6px;
  right: 0px;
}

.badge {
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background-color: #ff3b30;
  color: white;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.message-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #fff;
  font-size: 14px;
  color: #333;
  border-bottom: 1px solid #eaeaea;
}

.title-text {
  font-weight: 500;
  color: #333;
}

.discover-group {
  display: flex;
  align-items: center;
  color: #07c160;
  font-size: 14px;
  padding: 6px 12px;
  border-radius: 16px;
  background-color: #f0f9f0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.discover-group i {
  font-size: 14px;
  margin-right: 4px;
}

.discover-group:active {
  background-color: #e0f3e0;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  background-color: #fff;
}

.conversation-wrapper {
  cursor: pointer;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  color: #ddd;
  margin-bottom: 16px;
  width: 80px;
  height: 80px;
  background-color: #f5f5f5;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-text {
  font-size: 16px;
  color: #666;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 14px;
  color: #999;
}
</style> 
