<template>
  <div class="post-list">
    <template v-for="post in posts" :key="post.id">
      <PostCard 
        :id="post.id"
        :title="post.title"
        :content="post.content"
        :author="post.author"
        :created-at="post.createdAt"
        :likes="post.likes"
        :comments="post.comments"
      />
    </template>
    
    <div v-if="posts.length === 0" class="no-posts">
      <p>暂无帖子</p>
    </div>
  </div>
</template>

<script>
import PostCard from '@/components/PostCard.vue';

export default {
  name: 'PostList',
  components: {
    PostCard
  },
  props: {
    posts: {
      type: Array,
      default: () => []
    }
  }
}
</script>

<style scoped>
.post-list {
  width: 100%;
}

.no-posts {
  text-align: center;
  padding: 20px;
  color: #999;
}
</style> 