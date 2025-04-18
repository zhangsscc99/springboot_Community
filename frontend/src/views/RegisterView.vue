<template>
  <div class="register-view">
    <div class="auth-container">
      <div class="auth-header">
        <button class="back-button" @click="goBack">
          <i class="fas fa-arrow-left"></i>
        </button>
        <h2 class="auth-title">注册</h2>
        <div class="spacer"></div>
      </div>
      
      <form class="auth-form" @submit.prevent="register">
        <div class="form-group">
          <label for="username">用户名</label>
          <input 
            type="text" 
            id="username" 
            v-model="username" 
            placeholder="请输入用户名"
            required
          >
        </div>
        
        <div class="form-group">
          <label for="email">邮箱</label>
          <input 
            type="email" 
            id="email" 
            v-model="email" 
            placeholder="请输入邮箱地址"
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
              placeholder="请设置密码"
              required
            >
            <i 
              :class="['password-toggle', showPassword ? 'fas fa-eye-slash' : 'fas fa-eye']" 
              @click="togglePasswordVisibility"
            ></i>
          </div>
          <div class="password-strength" v-if="password">
            <div class="strength-bar">
              <div 
                class="strength-level" 
                :class="passwordStrengthClass"
                :style="{ width: passwordStrengthWidth }"
              ></div>
            </div>
            <span>{{ passwordStrengthText }}</span>
          </div>
        </div>
        
        <div class="form-group">
          <label for="confirmPassword">确认密码</label>
          <div class="password-input-wrapper">
            <input 
              :type="showConfirmPassword ? 'text' : 'password'" 
              id="confirmPassword" 
              v-model="confirmPassword" 
              placeholder="请再次输入密码"
              required
            >
            <i 
              :class="['password-toggle', showConfirmPassword ? 'fas fa-eye-slash' : 'fas fa-eye']" 
              @click="toggleConfirmPasswordVisibility"
            ></i>
          </div>
          <div class="password-error" v-if="passwordMismatch">
            两次输入的密码不一致
          </div>
        </div>
        
        <div class="form-agreement">
          <label class="agreement-checkbox">
            <input type="checkbox" v-model="agreement">
            <span>我已阅读并同意 <a href="#">服务条款</a> 和 <a href="#">隐私政策</a></span>
          </label>
        </div>
        
        <button 
          type="submit" 
          class="auth-submit-btn" 
          :disabled="loading || !isFormValid"
        >
          <span v-if="loading">
            <i class="fas fa-spinner fa-spin"></i> 注册中...
          </span>
          <span v-else>注册</span>
        </button>
        
        <div v-if="error" class="auth-error">
          {{ error }}
        </div>
      </form>
      
      <div class="auth-footer">
        <div class="auth-divider">
          <span>或</span>
        </div>
        
        <div class="login-link">
          已有账号？ <router-link to="/login">去登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';

export default {
  name: 'RegisterView',
  data() {
    return {
      username: '',
      email: '',
      password: '',
      confirmPassword: '',
      showPassword: false,
      showConfirmPassword: false,
      agreement: false
    };
  },
  computed: {
    ...mapGetters({
      loading: 'isLoading',
      error: 'error'
    }),
    passwordMismatch() {
      return this.confirmPassword && this.password !== this.confirmPassword;
    },
    passwordStrength() {
      if (!this.password) return 0;
      
      let strength = 0;
      
      // Length check
      if (this.password.length >= 8) strength += 1;
      
      // Contains lowercase
      if (/[a-z]/.test(this.password)) strength += 1;
      
      // Contains uppercase
      if (/[A-Z]/.test(this.password)) strength += 1;
      
      // Contains number
      if (/[0-9]/.test(this.password)) strength += 1;
      
      // Contains special char
      if (/[^A-Za-z0-9]/.test(this.password)) strength += 1;
      
      return strength;
    },
    passwordStrengthClass() {
      const strength = this.passwordStrength;
      if (strength <= 1) return 'weak';
      if (strength <= 3) return 'medium';
      return 'strong';
    },
    passwordStrengthWidth() {
      const strength = this.passwordStrength;
      return `${(strength / 5) * 100}%`;
    },
    passwordStrengthText() {
      const strength = this.passwordStrength;
      if (strength <= 1) return '弱';
      if (strength <= 3) return '中';
      return '强';
    },
    isFormValid() {
      return (
        this.username.trim() &&
        this.email.trim() &&
        this.password.trim() &&
        this.password === this.confirmPassword &&
        this.agreement &&
        this.passwordStrength >= 3
      );
    }
  },
  methods: {
    togglePasswordVisibility() {
      this.showPassword = !this.showPassword;
    },
    toggleConfirmPasswordVisibility() {
      this.showConfirmPassword = !this.showConfirmPassword;
    },
    goBack() {
      this.$router.go(-1);
    },
    async register() {
      if (!this.isFormValid) return;
      
      try {
        // In a real app, would call an API endpoint
        // For now, simulate success and redirect
        setTimeout(() => {
          this.$router.push({ name: 'login' });
        }, 1000);
      } catch (error) {
        console.error('Registration failed:', error);
      }
    }
  }
}
</script>

<style scoped>
.register-view {
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

.password-strength {
  margin-top: 10px;
  font-size: 12px;
  color: var(--light-text-color);
}

.strength-bar {
  height: 5px;
  background-color: #e0e0e0;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 5px;
}

.strength-level {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;
}

.strength-level.weak {
  background-color: #ff3b30;
}

.strength-level.medium {
  background-color: #ff9500;
}

.strength-level.strong {
  background-color: #28cd41;
}

.password-error {
  color: var(--error-color);
  font-size: 12px;
  margin-top: 5px;
}

.form-agreement {
  margin-bottom: 20px;
  font-size: 14px;
}

.agreement-checkbox {
  display: flex;
  align-items: flex-start;
  cursor: pointer;
}

.agreement-checkbox input {
  margin-right: 8px;
  margin-top: 3px;
}

.agreement-checkbox a {
  color: var(--primary-color);
  text-decoration: none;
}

.agreement-checkbox a:hover {
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

.login-link {
  font-size: 14px;
  color: var(--light-text-color);
}

.login-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
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
}
</style> 