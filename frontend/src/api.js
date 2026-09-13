const API_BASE = '/api';

export async function fetchConfig() {
  try {
    const res = await fetch(`${API_BASE}/config`);
    if (!res.ok) throw new Error('Failed to fetch config');
    return await res.json();
  } catch (err) {
    console.error(err);
    return { hasBackendApiKey: false, model: 'gemini-2.5-flash', status: 'ONLINE (Mock)' };
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

async function extractErrorMessage(res, defaultMsg = '請求失敗') {
  try {
    const errorData = await res.json();
    if (errorData) {
      if (errorData.message) return errorData.message;
      if (errorData.rawError) return errorData.rawError;
      if (errorData.error) return errorData.error;
      return JSON.stringify(errorData);
    }
  } catch (e) {
    try {
      const text = await res.text();
      if (text && text.trim()) return text;
    } catch {}
  }
  return `${defaultMsg} (HTTP ${res.status}: ${res.statusText})`;
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
    const errorMsg = await extractErrorMessage(res, '修飾失敗');
    throw new Error(errorMsg);
  }
  return await res.json();
}

/**
 * 輔助函數：解析 SSE 串流 (Server-Sent Events)
 */
async function parseSseStream(response, { onMetadata, onDelta, onDone, onError }) {
  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  let currentEvent = 'message';

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop(); // 保留最後尚未換行的片段

      for (let line of lines) {
        line = line.trim();
        if (!line) {
          currentEvent = 'message';
          continue;
        }

        if (line.startsWith('event:')) {
          currentEvent = line.substring(6).trim();
        } else if (line.startsWith('data:')) {
          const rawData = line.substring(5).trim();
          try {
            const data = JSON.parse(rawData);
            if (currentEvent === 'metadata' && onMetadata) {
              onMetadata(data);
            } else if (currentEvent === 'delta' && onDelta) {
              if (data.text) onDelta(data.text);
            } else if (currentEvent === 'done' && onDone) {
              onDone(data);
            } else if (currentEvent === 'error') {
              const errMsg = data.error || data.message || (typeof data === 'string' ? data : JSON.stringify(data));
              if (onError) onError(new Error(errMsg || '串流傳輸錯誤'));
              return;
            }
          } catch (e) {
            console.warn('Failed to parse SSE JSON data:', rawData);
          }
        }
      }
    }
  } finally {
    reader.releaseLock();
  }
}

/**
 * 第一次修飾 (端到端真流式輸出 SSE)
 */
export async function createPolishConversationStream({ rawText, customApiKey, onMetadata, onDelta, signal }) {
  const res = await fetch(`${API_BASE}/conversations/polish/stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      rawText,
      apiKey: customApiKey || undefined
    }),
    signal
  });

  if (!res.ok) {
    const errorMsg = await extractErrorMessage(res, '修飾請求失敗');
    throw new Error(errorMsg);
  }

  return new Promise((resolve, reject) => {
    let metadata = null;
    let fullText = '';
    let isCompleted = false;

    parseSseStream(res, {
      onMetadata: (data) => {
        metadata = data;
        if (onMetadata) onMetadata(data);
      },
      onDelta: (chunk) => {
        fullText += chunk;
        if (onDelta) onDelta(chunk, fullText);
      },
      onDone: (data) => {
        isCompleted = true;
        resolve({ metadata, fullText, ...data });
      },
      onError: (err) => {
        reject(err);
      }
    }).then(() => {
      if (!isCompleted) {
        resolve({ metadata, fullText, status: 'completed' });
      }
    }).catch(reject);
  });
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
    const errorMsg = await extractErrorMessage(res, '發送追加訊息失敗');
    throw new Error(errorMsg);
  }
  return await res.json();
}

/**
 * 追加發問 (端到端真流式輸出 SSE)
 */
export async function sendFollowUpMessageStream({ conversationId, message, customApiKey, onMetadata, onDelta, signal }) {
  const res = await fetch(`${API_BASE}/conversations/${conversationId}/messages/stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      message,
      apiKey: customApiKey || undefined
    }),
    signal
  });

  if (!res.ok) {
    const errorMsg = await extractErrorMessage(res, '發送追加訊息失敗');
    throw new Error(errorMsg);
  }

  return new Promise((resolve, reject) => {
    let metadata = null;
    let fullText = '';
    let isCompleted = false;

    parseSseStream(res, {
      onMetadata: (data) => {
        metadata = data;
        if (onMetadata) onMetadata(data);
      },
      onDelta: (chunk) => {
        fullText += chunk;
        if (onDelta) onDelta(chunk, fullText);
      },
      onDone: (data) => {
        isCompleted = true;
        resolve({ metadata, fullText, ...data });
      },
      onError: (err) => {
        reject(err);
      }
    }).then(() => {
      if (!isCompleted) {
        resolve({ metadata, fullText, status: 'completed' });
      }
    }).catch(reject);
  });
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
