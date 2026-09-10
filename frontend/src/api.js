const API_BASE = '/api';

export async function fetchConfig() {
  try {
    const res = await fetch(`${API_BASE}/config`);
    if (!res.ok) throw new Error('Failed to fetch config');
    return await res.json();
  } catch (err) {
    console.error(err);
    return { hasBackendApiKey: false, model: 'gemini-1.5-flash', status: 'ONLINE (Mock)' };
  }
}

export async function fetchConversations() {
  const res = await fetch(`${API_BASE}/conversations`);
  if (!res.ok) {
    throw new Error('Failed to load conversations');
  }
  const data = await res.json();
  return Array.isArray(data) ? data : (data.content || []);
}

export async function fetchConversationDetails(id) {
  const res = await fetch(`${API_BASE}/conversations/${id}`);
  if (!res.ok) {
    throw new Error('Failed to load conversation details');
  }
  return await res.json();
}

export async function createPolishConversation(rawText, customApiKey, signal) {
  const res = await fetch(`${API_BASE}/conversations/polish`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      rawText,
      apiKey: customApiKey || undefined
    }),
    signal
  });
  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.message || '修飾失敗，請檢查輸入內容或後端日誌');
  }
  return await res.json();
}

export async function sendFollowUpMessage(conversationId, message, customApiKey, signal) {
  const res = await fetch(`${API_BASE}/conversations/${conversationId}/messages`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      message,
      apiKey: customApiKey || undefined
    }),
    signal
  });
  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.message || '發送追加訊息失敗');
  }
  return await res.json();
}

export async function updateConversationTitle(id, title) {
  const res = await fetch(`${API_BASE}/conversations/${id}/title`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title })
  });
  if (!res.ok) {
    throw new Error('更新標題失敗');
  }
  return await res.json();
}

export async function deleteConversation(id) {
  const res = await fetch(`${API_BASE}/conversations/${id}`, {
    method: 'DELETE'
  });
  if (!res.ok) {
    throw new Error('Failed to delete conversation');
  }
}
