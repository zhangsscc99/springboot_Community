<template>
  <div class="notifications-container">
    <!-- Header -->
    <div class="notifications-header">
      <div class="back-button" @click="goBack">
        <i class="fas fa-chevron-left"></i>
      </div>
      <div class="title">{{ categoryTitle }}</div>
      <div class="action-button" @click="markAllAsRead">
        <i class="fas fa-check-double"></i>
      </div>
    </div>

    <!-- Notification List -->
    <div class="notification-content">
      <div v-if="loading" class="loading-state">
        <div class="loader"></div>
      </div>

      <div v-else-if="notifications.length === 0" class="empty-state">
        <div class="icon-placeholder">
          <i :class="emptyCategoryIcon"></i>
        </div>
        <div class="text">{{ emptyText }}</div>
      </div>

      <div v-else class="notification-list">
        <div v-for="notification in notifications" :key="notification.id" 
             class="notification-item" 
             :class="{ 'unread': !notification.read }"
             @click="handleNotificationClick(notification)">
          <div class="notification-avatar">
            <img :src="notification.senderAvatar || '/default-avatar.png'" :alt="notification.senderUsername">
          </div>
          <div class="notification-body">
            <div class="notification-header">
              <div class="notification-sender">{{ notification.senderUsername }}</div>
              <div class="notification-time">{{ formatTime(notification.createdAt) }}</div>
            </div>
            <div class="notification-message">
              {{ renderNotificationContent(notification) }}
            </div>
            <div v-if="notification.entityType === 'POST' && notification.postTitle" class="notification-reference">
              {{ notification.postTitle }}
            </div>
          </div>
        </div>
      </div>

      <div v-if="hasMore && !loadingMore" class="load-more">
        <button @click="loadMoreNotifications" class="load-more-button">加载更多</button>
      </div>
      <div v-if="loadingMore" class="loading-more">
        <div class="small-loader"></div>
      </div>
    </div>
  </div>
</template>

<script>
import NotificationService from '@/services/notification.service';
import { formatDistanceToNow } from 'date-fns';
import { zhCN } from 'date-fns/locale';

export default {
  name: 'NotificationsView',
  data() {
    return {
      category: '',
      notifications: [],
      loading: true,
      loadingMore: false,
      page: 0,
      size: 20,
      hasMore: false,
      categoryInfo: {
        likes: {
          title: '赞和收藏',
          emptyText: '暂无赞和收藏通知',
          icon: 'fas fa-heart'
        },
        follows: {
          title: '新增关注',
          emptyText: '暂无关注通知',
          icon: 'fas fa-user-plus'
        },
        comments: {
          title: '评论和@',
          emptyText: '暂无评论和@通知',
          icon: 'fas fa-comment'
        },
        system: {
          title: '系统通知',
          emptyText: '暂无系统通知',
          icon: 'fas fa-bell'
        }
      }
    };
  },
  computed: {
    categoryTitle() {
      return this.categoryInfo[this.category]?.title || '通知';
    },
    emptyText() {
      return this.categoryInfo[this.category]?.emptyText || '暂无通知';
    },
    emptyCategoryIcon() {
      return this.categoryInfo[this.category]?.icon || 'fas fa-bell';
    }
  },
  created() {
    this.category = this.$route.params.category || 'all';
    this.fetchNotifications();
  },
  watch: {
    '$route.params.category'(newCategory) {
      if (newCategory !== this.category) {
        this.category = newCategory;
        this.notifications = [];
        this.page = 0;
        this.hasMore = false;
        this.fetchNotifications();
      }
    }
  },
  methods: {
    async fetchNotifications() {
      try {
        this.loading = true;
        let response;

        if (this.category === 'all') {
          response = await NotificationService.getNotifications(this.page, this.size);
        } else {
          response = await NotificationService.getNotificationsByType(this.category, this.page, this.size);
        }

        if (response.data) {
          this.notifications = [...this.notifications, ...response.data.content];
          this.hasMore = !response.data.last;
        }
      } catch (error) {
        console.error('Failed to fetch notifications:', error);
      } finally {
        this.loading = false;
      }
    },
    async loadMoreNotifications() {
      if (this.loadingMore || !this.hasMore) return;
      
      this.page += 1;
      this.loadingMore = true;
      
      try {
        let response;
        
        if (this.category === 'all') {
          response = await NotificationService.getNotifications(this.page, this.size);
        } else {
          response = await NotificationService.getNotificationsByType(this.category, this.page, this.size);
        }

        if (response.data) {
          this.notifications = [...this.notifications, ...response.data.content];
          this.hasMore = !response.data.last;
        }
      } catch (error) {
        console.error('Failed to load more notifications:', error);
      } finally {
        this.loadingMore = false;
      }
    },
    async markAllAsRead() {
      try {
        if (this.category === 'all') {
          await NotificationService.markAllAsRead();
        } else {
          await NotificationService.markAllAsReadByType(this.category);
        }
        
        // Update UI to show all notifications as read
        this.notifications.forEach(notification => {
          notification.read = true;
        });
        
        this.$message.success('已全部标记为已读');
      } catch (error) {
        console.error('Failed to mark notifications as read:', error);
        this.$message.error('操作失败，请重试');
      }
    },
    async handleNotificationClick(notification) {
      if (!notification.read) {
        try {
          await NotificationService.markAsRead(notification.id);
          notification.read = true;
        } catch (error) {
          console.error('Failed to mark notification as read:', error);
        }
      }
      
      // Navigate based on notification type and entity
      if (notification.entityType === 'POST' && notification.entityId) {
        this.$router.push(`/post/${notification.entityId}`);
      } else if (notification.entityType === 'USER' && notification.entityId) {
        this.$router.push(`/profile/${notification.entityId}`);
      } else if (notification.type === 'PRIVATE_MESSAGE' && notification.senderId) {
        this.$router.push(`/chat/${notification.senderId}`);
      }
    },
    renderNotificationContent(notification) {
      switch (notification.type) {
        case 'LIKE':
          return notification.content || '赞了你的内容';
        case 'FOLLOW':
          return notification.content || '关注了你';
        case 'COMMENT':
          return notification.content || '评论了你的内容';
        case 'MENTION':
          return notification.content || '在内容中提及了你';
        case 'SYSTEM':
          return notification.content || '系统通知';
        default:
          return notification.content || '发送了一条通知';
      }
    },
    formatTime(time) {
      if (!time) return '';
      
      return formatDistanceToNow(new Date(time), {
        addSuffix: true,
        locale: zhCN
      });
    },
    goBack() {
      this.$router.push('/messages');
    }
  }
};
</script>

<style scoped>
.notifications-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f7f7f7;
}

.notifications-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #eaeaea;
}

.back-button, .action-button {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #07c160;
}

.title {
  flex: 1;
  text-align: center;
  font-size: 18px;
  font-weight: 500;
  color: #000;
}

.notification-content {
  flex: 1;
  overflow-y: auto;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100px;
  margin-top: 20px;
}

.loader {
  border: 4px solid #f3f3f3;
  border-radius: 50%;
  border-top: 4px solid #07c160;
  width: 30px;
  height: 30px;
  animation: spin 1s linear infinite;
}

.small-loader {
  border: 2px solid #f3f3f3;
  border-radius: 50%;
  border-top: 2px solid #07c160;
  width: 20px;
  height: 20px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-top: 100px;
  color: #999;
}

.icon-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 40px;
  background-color: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.icon-placeholder i {
  font-size: 36px;
  color: #ccc;
}

.notification-list {
  padding: 8px 0;
}

.notification-item {
  display: flex;
  padding: 16px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.notification-item.unread {
  background-color: #f0f8ff;
}

.notification-item:active {
  background-color: #f5f5f5;
}

.notification-avatar {
  width: 48px;
  height: 48px;
  border-radius: 24px;
  overflow: hidden;
  margin-right: 12px;
}

.notification-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.notification-body {
  flex: 1;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.notification-sender {
  font-weight: 500;
  color: #333;
}

.notification-time {
  font-size: 12px;
  color: #999;
}

.notification-message {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
  line-height: 1.4;
}

.notification-reference {
  font-size: 13px;
  color: #999;
  background-color: #f7f7f7;
  padding: 8px;
  border-radius: 4px;
  border-left: 3px solid #ddd;
  margin-top: 8px;
}

.load-more {
  text-align: center;
  padding: 16px;
}

.load-more-button {
  padding: 8px 24px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
  background-color: #fff;
  cursor: pointer;
}

.loading-more {
  display: flex;
  justify-content: center;
  padding: 16px;
}
</style> 