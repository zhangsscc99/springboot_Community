<template>
  <div class="notification-item" @click="$emit('click')">
    <div class="icon-container" :class="typeClass">
      <i :class="iconClass"></i>
    </div>
    
    <div class="content">
      <div class="title">{{ notification.title }}</div>
      <div class="description">{{ notification.content }}</div>
    </div>
    
    <div class="badge" v-if="notification.unreadCount > 0">
      {{ notification.unreadCount > 99 ? '99+' : notification.unreadCount }}
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotificationItem',
  props: {
    notification: {
      type: Object,
      required: true
    }
  },
  computed: {
    typeClass() {
      const types = {
        'like': 'type-like',
        'follow': 'type-follow',
        'comment': 'type-comment',
        'system': 'type-system'
      };
      return types[this.notification.type] || 'type-system';
    },
    iconClass() {
      const icons = {
        'like': 'iconfont icon-like',
        'follow': 'iconfont icon-user-add',
        'comment': 'iconfont icon-comment',
        'system': 'iconfont icon-notification'
      };
      return icons[this.notification.type] || 'iconfont icon-notification';
    }
  }
};
</script>

<style scoped>
.notification-item {
  display: flex;
  align-items: center;
  padding: 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background-color: #f5f5f5;
}

.icon-container {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
}

.icon-container i {
  font-size: 20px;
  color: #fff;
}

.type-like {
  background-color: #ff3b30;
}

.type-follow {
  background-color: #34c759;
}

.type-comment {
  background-color: #007aff;
}

.type-system {
  background-color: #ff9500;
}

.content {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.description {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.badge {
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background-color: #ff3b30;
  color: white;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
  margin-left: 12px;
}
</style> 
