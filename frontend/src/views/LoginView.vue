<template>
  <div class="login-view">
    <div class="auth-container">
      <div class="auth-header">
        <button class="back-button" @click="goBack">
          <i class="fas fa-arrow-left"></i>
        </button>
        <h2 class="auth-title">登录</h2>
        <div class="spacer"></div>
      </div>
      
      <form class="auth-form" @submit.prevent="login">
        <div class="form-group">
          <label for="email">邮箱或用户名</label>
          <input 
            type="text" 
            id="email" 
            v-model="email" 
            placeholder="请输入邮箱或用户名"
            required
          >
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <div class="password-input-wrapper">
            <input 
              :type="showPassword ? 'text' : 'password'" 
              id="password" 
              v-model="password" 
              placeholder="请输入密码"
              required
            >
            <i 
              :class="['password-toggle', showPassword ? 'fas fa-eye-slash' : 'fas fa-eye']" 
              @click="togglePasswordVisibility"
            ></i>
          </div>
        </div>
        
        <div class="form-options">
          <label class="remember-me">
            <input type="checkbox" v-model="rememberMe">
            <span>记住我</span>
          </label>
          
          <a href="#" class="forgot-password">忘记密码？</a>
        </div>
        
        <button 
          type="submit" 
          class="auth-submit-btn" 
          :disabled="loading || !isFormValid"
        >
          <span v-if="loading">
            <i class="fas fa-spinner fa-spin"></i> 登录中...
          </span>
          <span v-else>登录</span>
        </button>
        
        <div v-if="error" class="auth-error">
          {{ error }}
        </div>
      </form>
      
      <div class="auth-footer">
        <div class="auth-divider">
          <span>其他登录方式</span>
        </div>
        
        <div class="social-logins">
          <button class="social-btn">
            <i class="fab fa-weixin"></i>
          </button>
          <button class="social-btn">
            <i class="fab fa-qq"></i>
          </button>
          <button class="social-btn">
            <i class="fab fa-weibo"></i>
          </button>
        </div>
        
        <div class="register-link">
          还没有账号？ <router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';

export default {
  name: 'LoginView',
  data() {
    return {
      email: '',
      password: '',
      rememberMe: false,
      showPassword: false
    };
  },
  computed: {
    ...mapGetters({
      loading: 'isLoading',
      error: 'error'
    }),
    isFormValid() {
      return this.email.trim() && this.password.trim();
    }
  },
  methods: {
    togglePasswordVisibility() {
      this.showPassword = !this.showPassword;
    },
    goBack() {
      this.$router.go(-1);
    },
    async login() {
      if (!this.isFormValid) return;
      
      try {
        await this.$store.dispatch('login', {
          email: this.email,
          password: this.password,
          rememberMe: this.rememberMe
        });
        
        // Redirect to home page on successful login
        this.$router.push({ name: 'home' });
      } catch (error) {
        console.error('Login failed:', error);
      }
    }
  }
}
</script>

<style scoped>
.login-view {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--background-color);
}

.auth-container {
  flex: 1;
  max-width: 500px;
  margin: 0 auto;
  padding: 20px;
  width: 100%;
}

.auth-header {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
}

.back-button {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 5px;
  color: var(--text-color);
}

.auth-title {
  flex: 1;
  text-align: center;
  font-size: 20px;
  font-weight: 500;
  margin: 0;
}

.spacer {
  width: 24px;
}

.auth-form {
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--text-color);
}

.form-group input {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 15px;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: var(--primary-color);
}

.password-input-wrapper {
  position: relative;
}

.password-toggle {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: var(--light-text-color);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 14px;
}

.remember-me {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.remember-me input {
  margin-right: 6px;
}

.forgot-password {
  color: var(--primary-color);
  text-decoration: none;
}

.forgot-password:hover {
  text-decoration: underline;
}

.auth-submit-btn {
  width: 100%;
  padding: 12px;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.auth-submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(231, 76, 60, 0.3);
}

.auth-submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.auth-error {
  margin-top: 15px;
  color: var(--error-color);
  text-align: center;
  font-size: 14px;
}

.auth-footer {
  text-align: center;
}

.auth-divider {
  position: relative;
  margin: 30px 0;
  text-align: center;
}

.auth-divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background-color: var(--border-color);
  z-index: 0;
}

.auth-divider span {
  position: relative;
  padding: 0 15px;
  background-color: var(--background-color);
  color: var(--light-text-color);
  font-size: 14px;
  z-index: 1;
}

.social-logins {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 30px;
}

.social-btn {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: 1px solid var(--border-color);
  background-color: white;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.social-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
  border-color: transparent;
}

.social-btn i {
  color: #333;
}

.social-btn .fa-weixin {
  color: #07C160;
}

.social-btn .fa-qq {
  color: #12B7F5;
}

.social-btn .fa-weibo {
  color: #E6162D;
}

.register-link {
  font-size: 14px;
  color: var(--light-text-color);
}

.register-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.register-link a:hover {
  text-decoration: underline;
}

@media (max-width: 576px) {
  .auth-container {
    padding: 15px;
  }
  
  .auth-title {
    font-size: 18px;
  }
  
  .form-group input {
    padding: 10px 12px;
    font-size: 14px;
  }
  
  .auth-submit-btn {
    padding: 10px;
    font-size: 15px;
  }
  
  .social-btn {
    width: 45px;
    height: 45px;
    font-size: 18px;
  }
}
</style> 