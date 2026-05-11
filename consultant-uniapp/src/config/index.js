export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://10.101.0.168:8087'

export const STORAGE_KEYS = {
  auth: 'consultant_user_auth',
  guestSessions: 'consultant_chat_sessions_guest',
  currentSessionId: 'consultant_current_session_id'
}
