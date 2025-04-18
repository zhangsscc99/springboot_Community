/* eslint-disable */
import { createStore } from 'vuex'

export default createStore({
  state: {
    user: null,
    isAuthenticated: false,
    posts: [],
    currentPost: null,
    loading: false,
    error: null
  },
  getters: {
    isAuthenticated: state => state.isAuthenticated,
    currentUser: state => state.user,
    allPosts: state => state.posts,
    currentPost: state => state.currentPost,
    isLoading: state => state.loading,
    error: state => state.error
  },
  mutations: {
    SET_USER(state, user) {
      state.user = user;
      state.isAuthenticated = !!user;
    },
    SET_POSTS(state, posts) {
      state.posts = posts;
    },
    ADD_POST(state, post) {
      state.posts.unshift(post);
    },
    SET_CURRENT_POST(state, post) {
      state.currentPost = post;
    },
    SET_LOADING(state, status) {
      state.loading = status;
    },
    SET_ERROR(state, error) {
      state.error = error;
    }
  },
  actions: {
    // User authentication actions
    async login({ commit }, credentials) {
      commit('SET_LOADING', true);
      try {
        // Here would be an API call to authenticate
        const user = { id: 1, username: credentials.email };
        commit('SET_USER', user);
        commit('SET_ERROR', null);
      } catch (error) {
        commit('SET_ERROR', error.message);
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    logout({ commit }) {
      commit('SET_USER', null);
    },
    
    // Post related actions
    async fetchPosts({ commit }) {
      commit('SET_LOADING', true);
      try {
        // Here would be an API call to fetch posts
        const posts = [
          {
            id: 1,
            title: "为什么大厂程序员的另一半全职太太比例特别多?",
            content: "我说出了一些实话可能涉及全图 aoe。因为程序员，尤其是大厂程序员，在择偶上普遍是经济向下兼容模...",
            author: {
              id: 1,
              username: "李珠",
              avatar: "https://via.placeholder.com/40"
            },
            likes: 848,
            comments: 219,
            created_at: "2023-05-15T10:30:00Z"
          },
          {
            id: 2,
            title: "在雅思中你明白了哪些规则后，分数便开始直线飙升？",
            content: "在一次偶然的国际学术会议上，遇到了\"素未蒙面但经常email沟通\"的国外某导，在得知我最近在备考雅思...",
            author: {
              id: 2,
              username: "日辛说",
              avatar: "https://via.placeholder.com/40"
            },
            likes: 4912,
            comments: 12000,
            created_at: "2023-05-14T08:15:00Z"
          },
          {
            id: 3,
            title: "执行间隙为什么会毁掉一个人？",
            content: "刚刚读到一篇好文：一个人真正废掉的核心原因，往往不是能力不足或资源匮乏，而是从想法到行动的「执行间隙」太大，想做的事情迟迟不能落地。",
            author: {
              id: 3,
              username: "咫尺燃灯",
              avatar: "https://via.placeholder.com/40"
            },
            likes: 1488,
            comments: 3468,
            created_at: "2023-05-13T14:45:00Z"
          }
        ];
        commit('SET_POSTS', posts);
        commit('SET_ERROR', null);
      } catch (error) {
        commit('SET_ERROR', error.message);
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    async fetchPostById({ commit }, postId) {
      commit('SET_LOADING', true);
      try {
        // Here would be an API call to fetch a specific post
        const post = {
          id: postId,
          title: "执行间隙为什么会毁掉一个人？",
          content: "刚刚读到一篇好文：一个人真正废掉的核心原因，往往不是能力不足或资源匮乏，而是从想法到行动的「执行间隙」太大，想做的事情迟迟不能落地。\n\n我们常常高估了自己的意志力，认为光靠想就能改变自己，却忽视了建立习惯和系统对于长期成功的重要性。执行间隙本质上是我们的行动系统出了问题，而不是单纯的懒惰或拖延。\n\n要解决执行间隙问题，我们需要：\n1. 设立明确且可量化的目标\n2. 将大目标分解成小步骤\n3. 建立固定的行动流程\n4. 创造有利于行动的环境\n5. 培养正向反馈循环",
          author: {
            id: 3,
            username: "咫尺燃灯",
            avatar: "https://via.placeholder.com/40"
          },
          likes: 1488,
          comments: 3468,
          created_at: "2023-05-13T14:45:00Z",
          commentList: [
            {
              id: 1,
              content: "这篇文章说到我心坎里了，我就是有严重的执行间隙问题",
              author: {
                id: 4,
                username: "Reader01",
                avatar: "https://via.placeholder.com/40"
              },
              created_at: "2023-05-13T15:30:00Z"
            },
            {
              id: 2,
              content: "建立习惯真的很重要，我用番茄工作法后效率提高了不少",
              author: {
                id: 5,
                username: "Efficiency99",
                avatar: "https://via.placeholder.com/40"
              },
              created_at: "2023-05-13T16:20:00Z"
            }
          ]
        };
        commit('SET_CURRENT_POST', post);
        commit('SET_ERROR', null);
      } catch (error) {
        commit('SET_ERROR', error.message);
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    async createPost({ commit }, postData) {
      commit('SET_LOADING', true);
      try {
        // Here would be an API call to create a post
        const newPost = {
          id: Date.now(),
          title: postData.title,
          content: postData.content,
          author: {
            id: 1,
            username: "User123",
            avatar: "https://via.placeholder.com/40"
          },
          likes: 0,
          comments: 0,
          created_at: new Date().toISOString()
        };
        commit('ADD_POST', newPost);
        commit('SET_ERROR', null);
        return newPost;
      } catch (error) {
        commit('SET_ERROR', error.message);
        throw error;
      } finally {
        commit('SET_LOADING', false);
      }
    }
  },
  modules: {
  }
}) 