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
  sendFollowUpMessage,
  updateConversationTitle,
  deleteConversation
} from './api';

export default function App() {
  const [activeTab, setActiveTab] = useState('polish');
  const [conversations, setConversations] = useState([]);
  const [currentConversationId, setCurrentConversationId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorInfo, setErrorInfo] = useState(null);

  // Option B: Drafts map per conversation / session
  const [drafts, setDrafts] = useState({});
  const lastPendingTextRef = useRef('');
  const abortControllerRef = useRef(null);

  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [apiKey, setApiKey] = useState('');
  const [hasBackendApiKey, setHasBackendApiKey] = useState(false);
  const [modelName, setModelName] = useState('gemini-1.5-flash');
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
      setMessages(data.messages || []);
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

  // Send message
  const handleSend = async (overrideText) => {
    const text = (overrideText || input).trim();
    if (!text || isLoading) return;

    lastPendingTextRef.current = text;
    setErrorInfo(null);
    setIsLoading(true);

    const controller = new AbortController();
    abortControllerRef.current = controller;

    if (!currentConversationId || messages.length === 0) {
      // First round: Polish
      const tempUserMsg = {
        id: 'temp-user',
        senderType: 'USER',
        content: text,
        roundNumber: 1,
        createdAt: new Date().toISOString()
      };
      setMessages([tempUserMsg]);
      setInput('');

      try {
        const res = await createPolishConversation(text, apiKey, controller.signal);
        setCurrentConversationId(res.conversation.id);
        setMessages(res.messages);
        setConversations((prev) => [res.conversation, ...prev]);
        setDrafts((prev) => {
          const next = { ...prev };
          delete next['new_chat'];
          return next;
        });
      } catch (err) {
        if (err.name === 'AbortError') {
          // Handled in handleStopThinking
          return;
        }
        setErrorInfo({ message: err.message, text });
      } finally {
        setIsLoading(false);
        abortControllerRef.current = null;
      }
    } else {
      // Follow-up
      const nextRound = Math.floor(messages.length / 2) + 1;
      const tempUserMsg = {
        id: 'temp-user-' + Date.now(),
        senderType: 'USER',
        content: text,
        roundNumber: nextRound,
        createdAt: new Date().toISOString()
      };

      setMessages((prev) => [...prev, tempUserMsg]);
      setInput('');

      try {
        const aiMsg = await sendFollowUpMessage(currentConversationId, text, apiKey, controller.signal);
        setMessages((prev) => [...prev, aiMsg]);
        setConversations((prev) =>
          prev.map((c) =>
            c.id === currentConversationId ? { ...c, updatedAt: new Date().toISOString() } : c
          )
        );
      } catch (err) {
        if (err.name === 'AbortError') {
          return;
        }
        setErrorInfo({ message: err.message, text });
      } finally {
        setIsLoading(false);
        abortControllerRef.current = null;
      }
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
    // Remove last unreplied user message from view
    setMessages((prev) => prev.filter((m) => !String(m.id).startsWith('temp-user')));
  };

  const handleRetry = () => {
    if (errorInfo && errorInfo.text) {
      const retryText = errorInfo.text;
      setErrorInfo(null);
      handleSend(retryText);
    }
  };

  const handleSampleClick = (sample) => {
    setInput(sample);
  };

  const currentConv = conversations.find((c) => c.id === currentConversationId);
  const isFollowUp = !!currentConversationId && messages.length > 0;

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
            messages={messages}
            isLoading={isLoading}
            errorInfo={errorInfo}
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
