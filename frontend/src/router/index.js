import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/login',
    name: 'login',
    component: () => import(/* webpackChunkName: "login" */ '../views/LoginView.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import(/* webpackChunkName: "register" */ '../views/RegisterView.vue')
  },
  {
    path: '/post/:id',
    name: 'post-detail',
    component: () => import(/* webpackChunkName: "post-detail" */ '../views/PostDetailView.vue')
  },
  {
    path: '/create-post',
    name: 'create-post',
    component: () => import(/* webpackChunkName: "create-post" */ '../views/CreatePostView.vue')
  },
  {
    path: '/profile/:id',
    name: 'profile',
    component: () => import(/* webpackChunkName: "profile" */ '../views/ProfileView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router 