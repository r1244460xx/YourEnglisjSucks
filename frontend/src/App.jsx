import React, { useState, useEffect, useRef } from 'react';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import ChatArea from './components/ChatArea';
import InputBox from './components/InputBox';
import SettingsModal from './components/SettingsModal';
import {
  fetchConfig,
  fetchConversations,
  fetchConversationDetails,
  createPolishConversation,
  createPolishConversationStream,
  sendFollowUpMessage,
  sendFollowUpMessageStream,
  updateConversationTitle,
  deleteConversation
} from './api';

export default function App() {
  const [activeTab, setActiveTab] = useState('polish');
  const [conversations, setConversations] = useState([]);
  const [currentConversationId, setCurrentConversationId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [originalDraftText, setOriginalDraftText] = useState('');
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  // Option B: Drafts map per conversation / session
  const [drafts, setDrafts] = useState({});
  const lastPendingTextRef = useRef('');
  const lastActiveMessagesRef = useRef(null);
  const abortControllerRef = useRef(null);

  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [apiKey, setApiKey] = useState('');
  const [hasBackendApiKey, setHasBackendApiKey] = useState(false);
  const [modelName, setModelName] = useState('gemini-3.6-flash');
  const [isDark, setIsDark] = useState(false);

  useEffect(() => {
    const savedKey = localStorage.getItem('yourenglishsucks_gemini_api_key') || '';
    setApiKey(savedKey);

    const savedDark = localStorage.getItem('yourenglishsucks_dark') === 'true';
    setIsDark(savedDark);
    if (savedDark) {
      document.documentElement.classList.add('dark');
    }

    fetchConfig().then((cfg) => {
      setHasBackendApiKey(cfg.hasBackendApiKey);
      if (cfg.model) setModelName(cfg.model);
    });

    loadConversations();
  }, []);

  const toggleDark = () => {
    const nextDark = !isDark;
    setIsDark(nextDark);
    localStorage.setItem('yourenglishsucks_dark', String(nextDark));
    if (nextDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  };

  const loadConversations = async () => {
    try {
      const data = await fetchConversations();
      setConversations(data || []);
    } catch (err) {
      console.error('Failed to load conversations:', err);
    }
  };

  // Option B: Save and restore drafts on session switch
  const saveDraftForCurrent = () => {
    const key = currentConversationId || 'new_chat';
    setDrafts((prev) => ({ ...prev, [key]: input }));
  };

  const handleNewChat = () => {
    saveDraftForCurrent();
    setCurrentConversationId(null);
    setMessages([]);
    setOriginalDraftText('');
    setErrorInfo(null);
    setInput(drafts['new_chat'] || '');
  };

  const handleSelectConversation = async (id) => {
    if (id === currentConversationId) return;
    saveDraftForCurrent();
    setIsLoading(true);
    setErrorInfo(null);
    try {
      const data = await fetchConversationDetails(id);
      setCurrentConversationId(id);
      setMessages(getValidConversationMessages(data.messages || []));
      const draft = data.rawSubmissionText || (data.messages && data.messages.find((m) => m.senderType === 'USER' && m.roundNumber === 1)?.content) || '';
      setOriginalDraftText(draft);
      setInput(drafts[id] || '');
    } catch (err) {
      alert(err.message || '讀取對話失敗');
    } finally {
      setIsLoading(false);
    }
  };

  // Option C: Rename conversation title
  const handleRenameConversation = async (id, newTitle) => {
    try {
      const updated = await updateConversationTitle(id, newTitle);
      setConversations((prev) =>
        prev.map((c) => (c.id === id ? { ...c, title: updated.title } : c))
      );
    } catch (err) {
      alert('更新標題失敗');
    }
  };

  const handleDeleteConversation = async (id) => {
    try {
      await deleteConversation(id);
      setConversations((prev) => prev.filter((c) => c.id !== id));
      setDrafts((prev) => {
        const next = { ...prev };
        delete next[id];
        return next;
      });
      if (currentConversationId === id) {
        handleNewChat();
      }
    } catch (err) {
      alert('刪除失敗');
    }
  };

  // 執行第 1 輪英文修飾 (Polish)
  const executeSendPolish = async (text) => {
    lastPendingTextRef.current = text;
    lastActiveMessagesRef.current = [];
    setOriginalDraftText(text);
    setErrorInfo(null);
    setIsLoading(true);

    const controller = new AbortController();
    abortControllerRef.current = controller;

    const userMsgId = 'user-r1-' + Date.now();
    const aiStreamingId = 'ai-streaming-' + Date.now();

    const tempUserMsg = {
      id: userMsgId,
      senderType: 'USER',
      content: text,
      roundNumber: 1,
      createdAt: new Date().toISOString()
    };
    const tempAiMsg = {
      id: aiStreamingId,
      senderType: 'AI',
      content: '',
      roundNumber: 1,
      createdAt: new Date().toISOString()
    };
    setMessages([tempUserMsg, tempAiMsg]);
    setInput('');

    try {
      const streamResult = await createPolishConversationStream({
        rawText: text,
        customApiKey: apiKey,
        onMetadata: (metadata) => {
          setCurrentConversationId(metadata.conversationId);
          const newConv = {
            id: metadata.conversationId,
            title: metadata.title,
            mode: metadata.mode || 'POLISH',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          };
          setConversations((prev) => [newConv, ...prev.filter((c) => c.id !== metadata.conversationId)]);

          // 若後端返回了持久化 User 訊息 ID，立即更新 ID
          if (metadata.userMessageId) {
            setMessages((prev) =>
              prev.map((m) =>
                m.id === userMsgId ? { ...m, id: metadata.userMessageId } : m
              )
            );
          }
        },
        onDelta: (chunk, accumulatedText) => {
          setMessages((prev) =>
            prev.map((m) =>
              m.id === aiStreamingId ? { ...m, content: accumulatedText } : m
            )
          );
        },
        signal: controller.signal
      });

      lastActiveMessagesRef.current = null;
      setMessages((prev) =>
        prev.map((m) =>
          m.id === aiStreamingId
            ? { ...m, id: streamResult.aiMessageId || ('ai-' + Date.now()), content: streamResult.fullText }
            : m
        )
      );

      setDrafts((prev) => {
        const next = { ...prev };
        delete next['new_chat'];
        return next;
      });
    } catch (err) {
      if (err.name === 'AbortError') {
        return;
      }
      // 僅移除未完成的串流中 AI 訊息，保留使用者輸入草稿與錯誤原文提示
      setMessages((prev) => prev.filter((m) => m.id !== aiStreamingId));
      setErrorInfo({ message: err.message, text });
    } finally {
      setIsLoading(false);
      abortControllerRef.current = null;
    }
  };

  // 執行後續追加問答 (Follow-up)
  const executeSendFollowUp = async (text, convId, baseMessages) => {
    lastPendingTextRef.current = text;
    lastActiveMessagesRef.current = baseMessages;
    setErrorInfo(null);
    setIsLoading(true);

    const controller = new AbortController();
    abortControllerRef.current = controller;

    const nextRound = Math.floor(baseMessages.length / 2) + 1;
    const userMsgId = 'user-r' + nextRound + '-' + Date.now();
    const aiStreamingId = 'ai-streaming-' + Date.now();
    const tempUserMsg = {
      id: userMsgId,
      senderType: 'USER',
      content: text,
      roundNumber: nextRound,
      createdAt: new Date().toISOString()
    };
    const tempAiMsg = {
      id: aiStreamingId,
      senderType: 'AI',
      content: '',
      roundNumber: nextRound,
      createdAt: new Date().toISOString()
    };

    setMessages([...baseMessages, tempUserMsg, tempAiMsg]);
    setInput('');

    try {
      const streamResult = await sendFollowUpMessageStream({
        conversationId: convId,
        message: text,
        customApiKey: apiKey,
        onMetadata: (metadata) => {
          if (metadata && metadata.userMessageId) {
            setMessages((prev) =>
              prev.map((m) =>
                m.id === userMsgId ? { ...m, id: metadata.userMessageId } : m
              )
            );
          }
        },
        onDelta: (chunk, accumulatedText) => {
          setMessages((prev) =>
            prev.map((m) =>
              m.id === aiStreamingId ? { ...m, content: accumulatedText } : m
            )
          );
        },
        signal: controller.signal
      });

      lastActiveMessagesRef.current = null;
      setMessages((prev) =>
        prev.map((m) =>
          m.id === aiStreamingId
            ? { ...m, id: streamResult.aiMessageId || ('ai-' + Date.now()), content: streamResult.fullText }
            : m
        )
      );
      setConversations((prev) =>
        prev.map((c) =>
          c.id === convId ? { ...c, updatedAt: new Date().toISOString() } : c
        )
      );
    } catch (err) {
      if (err.name === 'AbortError') {
        return;
      }
      // 僅移除未成功的串流中 AI 訊息，保留該輪使用者問題與錯誤原文提示
      setMessages([...baseMessages, tempUserMsg]);
      setErrorInfo({ message: err.message, text });
    } finally {
      setIsLoading(false);
      abortControllerRef.current = null;
    }
  };

  // 輔助函式：取得乾淨且完全成功的問答列表
  // 若最後一則是未獲得 AI 成功回覆的使用者發問 (例如遇到 4xx 錯誤)，將其剔除，保證畫面只保留完全成功的問答
  const getValidConversationMessages = (msgs) => {
    const withoutStreaming = (msgs || []).filter((m) => !String(m.id).includes('streaming'));
    if (withoutStreaming.length > 0 && withoutStreaming[withoutStreaming.length - 1].senderType === 'USER') {
      return withoutStreaming.slice(0, -1);
    }
    return withoutStreaming;
  };

  // Send message with End-to-End Streaming
  const handleSend = async (overrideText) => {
    const text = (overrideText || input).trim();
    if (!text || isLoading) return;

    // 清除任何先前的錯誤提示
    setErrorInfo(null);

    // 取得乾淨且完全成功的問答訊息 (若上一則發問遭遇 4xx 錯誤，自畫面徹底移除)
    const cleanMsgs = getValidConversationMessages(messages);
    const hasCompletedAiMsg = cleanMsgs.some((m) => m.senderType === 'AI');

    if (!currentConversationId || !hasCompletedAiMsg) {
      setMessages([]);
      executeSendPolish(text);
    } else {
      setMessages(cleanMsgs);
      executeSendFollowUp(text, currentConversationId, cleanMsgs);
    }
  };

  // Requirement: 強制中斷思緒停止按鈕，原本的文本要保留回輸入欄
  const handleStopThinking = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    setIsLoading(false);
    // Restore text back to input
    setInput(lastPendingTextRef.current || '');
    // 乾淨回復至本次發問前的狀態 (避免留下未回覆的孤立 User 發問或佔位 AI 訊息)
    if (lastActiveMessagesRef.current !== null) {
      setMessages(lastActiveMessagesRef.current);
      lastActiveMessagesRef.current = null;
    } else {
      setMessages((prev) => getValidConversationMessages(prev));
    }
  };

  const handleRetry = () => {
    if (!errorInfo || !errorInfo.text) return;
    const retryText = errorInfo.text;
    // 立即清除錯誤狀態，使紅色錯誤框與重試按鈕即刻消失
    setErrorInfo(null);

    // 取得排除失敗問題後的乾淨基礎訊息
    const cleanMsgs = getValidConversationMessages(messages);
    const hasCompletedAiMsg = cleanMsgs.some((m) => m.senderType === 'AI');

    if (!currentConversationId || !hasCompletedAiMsg) {
      setCurrentConversationId(null);
      setMessages([]);
      executeSendPolish(retryText);
    } else {
      setMessages(cleanMsgs);
      executeSendFollowUp(retryText, currentConversationId, cleanMsgs);
    }
  };

  const handleSampleClick = (sample) => {
    setInput(sample);
  };

  const currentConv = conversations.find((c) => c.id === currentConversationId);
  const isFollowUp = !!currentConversationId && messages.some((m) => m.senderType === 'AI' && !String(m.id).includes('streaming'));

  return (
    <div className="h-screen flex flex-col font-sans overflow-hidden bg-[var(--background)] text-[var(--foreground)]">
      <Header
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onOpenSettings={() => setIsSettingsOpen(true)}
        apiKeyConfigured={hasBackendApiKey || !!apiKey}
        modelName={modelName}
        isDark={isDark}
        toggleDark={toggleDark}
      />

      <div className="flex flex-1 overflow-hidden">
        <Sidebar
          conversations={conversations}
          currentConversationId={currentConversationId}
          onSelectConversation={handleSelectConversation}
          onNewChat={handleNewChat}
          onDeleteConversation={handleDeleteConversation}
          onRenameConversation={handleRenameConversation}
        />

        <main className="flex-1 flex flex-col bg-[var(--background)] overflow-hidden relative">
          <ChatArea
            currentConversation={currentConv}
            originalDraftText={originalDraftText}
            messages={messages}
            isLoading={isLoading}
            errorInfo={errorInfo}
            apiKeyConfigured={hasBackendApiKey || !!apiKey}
            modelName={modelName}
            onOpenSettings={() => setIsSettingsOpen(true)}
            onSampleClick={handleSampleClick}
            onRenameTitle={handleRenameConversation}
            onStopThinking={handleStopThinking}
            onRetry={handleRetry}
          />

          <InputBox
            input={input}
            setInput={setInput}
            onSend={() => handleSend()}
            onStopThinking={handleStopThinking}
            isLoading={isLoading}
            isFollowUp={isFollowUp}
          />
        </main>
      </div>

      <SettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        apiKey={apiKey}
        setApiKey={setApiKey}
        hasBackendApiKey={hasBackendApiKey}
        modelName={modelName}
        setModelName={setModelName}
      />
    </div>
  );
}
