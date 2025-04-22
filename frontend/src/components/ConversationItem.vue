<template>
  <div class="conversation-item" :class="{ 'unread': conversation.unreadCount > 0 }">
    <div class="avatar">
      <img :src="conversation.avatar || defaultAvatar" alt="头像">
      <div class="badge" v-if="conversation.unreadCount > 0">
        {{ conversation.unreadCount > 99 ? '99+' : conversation.unreadCount }}
      </div>
    </div>
    <div class="content">
      <div class="top-row">
        <div class="name">{{ conversation.name }}</div>
        <div class="time">{{ formatTime(conversation.lastMessageTime) }}</div>
      </div>
      <div class="message-preview">{{ conversation.lastMessage }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ConversationItem',
  props: {
    conversation: {
      type: Object,
      required: true,
      default: () => ({
        id: '',
        name: '',
        avatar: '',
        lastMessage: '',
        lastMessageTime: new Date(),
        unreadCount: 0
      })
    }
  },
  data() {
    return {
      defaultAvatar: '/default-avatar.png'
    }
  },
  methods: {
    formatTime(time) {
      if (!time) return '';
      
      const date = new Date(time);
      const now = new Date();
      
      // If the message is from today, show only the time
      if (date.toDateString() === now.toDateString()) {
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      }
      
      // If the message is from this week, show the day of the week
      const diffDays = Math.round((now - date) / (1000 * 60 * 60 * 24));
      if (diffDays < 7) {
        const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
        return weekdays[date.getDay()];
      }
      
      // Otherwise, show the date
      return date.toLocaleDateString([], { month: 'numeric', day: 'numeric' });
    }
  }
}
</script>

<style scoped>
.conversation-item {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #eaeaea;
  cursor: pointer;
}

.conversation-item:hover {
  background-color: #f5f5f5;
}

.conversation-item.unread .name {
  font-weight: 600;
}

.avatar {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 4px;
  margin-right: 12px;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background-color: #ff3b30;
  color: white;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.top-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.name {
  font-size: 16px;
  color: #000;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 70%;
}

.time {
  font-size: 12px;
  color: #999;
}

.message-preview {
  font-size: 14px;
  color: #888;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style> 
