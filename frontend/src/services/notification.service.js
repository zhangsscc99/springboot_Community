import axios from 'axios';
import authHeader from './auth-header';
import { API_URL } from '@/config/api.config';

class NotificationService {
  /**
   * 获取所有通知
   * @param {number} page - 页码，从0开始
   * @param {number} size - 每页通知数量
   * @returns {Promise} 包含通知列表的Promise
   */
  getNotifications(page = 0, size = 20) {
    return axios.get(`${API_URL}/notifications`, {
      headers: authHeader(),
      params: { page, size }
    });
  }

  /**
   * 获取特定类型的通知
   * @param {string} type - 通知类型 (like, follow, comment, mention)
   * @param {number} page - 页码，从0开始
   * @param {number} size - 每页通知数量
   * @returns {Promise} 包含特定类型通知列表的Promise
   */
  getNotificationsByType(type, page = 0, size = 20) {
    return axios.get(`${API_URL}/notifications/type/${type}`, {
      headers: authHeader(),
      params: { page, size }
    });
  }

  /**
   * 获取未读通知数量
   * @returns {Promise} 包含未读通知数量的Promise
   */
  getUnreadCount() {
    return axios.get(`${API_URL}/notifications/unread-count`, { headers: authHeader() });
  }

  /**
   * 获取各类通知未读数
   * @returns {Promise} 包含各类通知未读数的Promise
   */
  getUnreadCountsByType() {
    return axios.get(`${API_URL}/notifications/unread-counts-by-type`, { headers: authHeader() });
  }

  /**
   * 将特定通知标记为已读
   * @param {number} notificationId - 通知ID
   * @returns {Promise} 包含操作结果的Promise
   */
  markAsRead(notificationId) {
    return axios.put(`${API_URL}/notifications/${notificationId}/read`, {}, { headers: authHeader() });
  }

  /**
   * 将所有通知标记为已读
   * @returns {Promise} 包含操作结果的Promise
   */
  markAllAsRead() {
    return axios.put(`${API_URL}/notifications/read-all`, {}, { headers: authHeader() });
  }

  /**
   * 将特定类型的所有通知标记为已读
   * @param {string} type - 通知类型 (like, follow, comment, mention)
   * @returns {Promise} 包含操作结果的Promise
   */
  markAllAsReadByType(type) {
    return axios.put(`${API_URL}/notifications/read-all/${type}`, {}, { headers: authHeader() });
  }

  /**
   * 删除特定通知
   * @param {number} notificationId - 通知ID
   * @returns {Promise} 包含操作结果的Promise
   */
  deleteNotification(notificationId) {
    return axios.delete(`${API_URL}/notifications/${notificationId}`, { headers: authHeader() });
  }

  /**
   * 删除所有通知
   * @returns {Promise} 包含操作结果的Promise
   */
  deleteAllNotifications() {
    return axios.delete(`${API_URL}/notifications/all`, { headers: authHeader() });
  }
}

export default new NotificationService(); 