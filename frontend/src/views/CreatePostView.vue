<template>
  <div class="create-post-view">
    <div class="create-post-header">
      <button class="back-button" @click="goBack">
        <i class="fas fa-times"></i>
      </button>
      <h2 class="create-post-title">创建帖子</h2>
      <button 
        class="publish-button" 
        :disabled="!isFormValid || loading"
        @click="publishPost"
      >
        发布
      </button>
    </div>
    
    <div class="create-post-form">
      <div class="form-group">
        <input 
          type="text" 
          v-model="title" 
          placeholder="标题（必填）" 
          class="form-control title-input"
          maxlength="100"
        >
        <div class="char-count">{{ title.length }}/100</div>
      </div>
      
      <div class="form-group">
        <textarea 
          v-model="content" 
          placeholder="分享你的想法..." 
          class="form-control content-input"
          rows="10"
        ></textarea>
      </div>
      
      <div class="form-actions">
        <div class="form-action">
          <i class="fas fa-image"></i>
          <span>图片</span>
        </div>
        <div class="form-action">
          <i class="fas fa-at"></i>
          <span>提及</span>
        </div>
        <div class="form-action">
          <i class="fas fa-hashtag"></i>
          <span>话题</span>
        </div>
      </div>
    </div>
    
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';

export default {
  name: 'CreatePostView',
  data() {
    return {
      title: '',
      content: ''
    };
  },
  computed: {
    ...mapGetters({
      loading: 'isLoading',
      error: 'error'
    }),
    isFormValid() {
      return this.title.trim().length > 0 && this.content.trim().length > 0;
    }
  },
  methods: {
    goBack() {
      this.$router.go(-1);
    },
    async publishPost() {
      if (!this.isFormValid) return;
      
      try {
        const newPost = await this.$store.dispatch('createPost', {
          title: this.title,
          content: this.content
        });
        
        this.$router.push({ 
          name: 'post-detail', 
          params: { id: newPost.id } 
        });
      } catch (error) {
        console.error('Failed to create post:', error);
      }
    }
  }
}
</script>

<style scoped>
.create-post-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: white;
}

.create-post-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px;
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  background-color: white;
  z-index: 10;
}

.back-button {
  border: none;
  background: none;
  font-size: 18px;
  cursor: pointer;
  color: var(--text-color);
  padding: 5px;
}

.create-post-title {
  font-size: 18px;
  font-weight: 500;
  margin: 0;
}

.publish-button {
  padding: 8px 16px;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
}

.publish-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.create-post-form {
  flex: 1;
  padding: 15px;
}

.form-group {
  margin-bottom: 20px;
  position: relative;
}

.form-control {
  width: 100%;
  border: none;
  font-size: 16px;
  padding: 10px 0;
  outline: none;
  resize: none;
  font-family: inherit;
}

.title-input {
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid var(--border-color);
}

.char-count {
  position: absolute;
  right: 0;
  bottom: 5px;
  font-size: 12px;
  color: var(--light-text-color);
}

.content-input {
  line-height: 1.6;
}

.form-actions {
  display: flex;
  gap: 20px;
  padding: 15px 0;
  border-top: 1px solid var(--border-color);
}

.form-action {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--light-text-color);
  font-size: 14px;
  cursor: pointer;
}

.form-action:hover {
  color: var(--primary-color);
}

.error-message {
  color: var(--error-color);
  padding: 15px;
  text-align: center;
}

@media (max-width: 768px) {
  .create-post-header {
    padding: 12px;
  }
  
  .create-post-title {
    font-size: 16px;
  }
  
  .publish-button {
    padding: 6px 12px;
    font-size: 13px;
  }
  
  .form-control {
    font-size: 15px;
  }
  
  .title-input {
    font-size: 16px;
  }
}
</style> 