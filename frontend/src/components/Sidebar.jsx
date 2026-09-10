import React, { useState } from 'react';
import { Plus, MessageSquare, Trash2, Edit2, AlertTriangle } from 'lucide-react';

export default function Sidebar({
  conversations,
  currentConversationId,
  onSelectConversation,
  onNewChat,
  onDeleteConversation,
  onRenameConversation
}) {
  const [editingId, setEditingId] = useState(null);
  const [editingTitle, setEditingTitle] = useState('');
  const [deleteModalId, setDeleteModalId] = useState(null);

  const startRename = (conv, e) => {
    e.stopPropagation();
    setEditingId(conv.id);
    setEditingTitle(conv.title);
  };

  const handleSaveRename = (convId) => {
    if (editingTitle.trim()) {
      onRenameConversation(convId, editingTitle.trim());
    }
    setEditingId(null);
  };

  const handleKeyDownRename = (e, convId) => {
    if (e.key === 'Enter') {
      handleSaveRename(convId);
    } else if (e.key === 'Escape') {
      setEditingId(null);
    }
  };

  return (
    <aside className="w-64 sm:w-72 bg-[var(--card)] border-r border-[var(--border)] flex flex-col shrink-0 select-none">
      {/* New Chat Button */}
      <div className="p-3 border-b border-[var(--border)]">
        <button
          onClick={onNewChat}
          className="w-full flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-700 active:scale-[0.98] text-white font-medium py-2.5 px-4 rounded-xl shadow-xs transition cursor-pointer text-xs sm:text-sm"
        >
          <Plus className="w-4 h-4" />
          <span>新對話 (New Chat)</span>
        </button>
      </div>

      {/* Conversations List */}
      <div className="flex-1 overflow-y-auto p-2.5 space-y-1">
        {conversations.length === 0 ? (
          <div className="p-4 text-center text-xs text-[var(--muted)]">
            尚未有歷史對話紀錄。點選上方「新對話」開始修飾英文。
          </div>
        ) : (
          conversations.map((conv) => {
            const isSelected = conv.id === currentConversationId;
            const isEditing = editingId === conv.id;

            return (
              <div
                key={conv.id}
                onClick={() => !isEditing && onSelectConversation(conv.id)}
                className={`group relative flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-medium cursor-pointer transition ${
                  isSelected
                    ? 'bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800/60 shadow-xs'
                    : 'text-[var(--foreground)] hover:bg-[var(--background)]'
                }`}
              >
                {isEditing ? (
                  <input
                    type="text"
                    value={editingTitle}
                    onChange={(e) => setEditingTitle(e.target.value)}
                    onBlur={() => handleSaveRename(conv.id)}
                    onKeyDown={(e) => handleKeyDownRename(e, conv.id)}
                    autoFocus
                    className="w-full px-2 py-0.5 text-xs rounded border border-indigo-500 bg-[var(--background)] text-[var(--foreground)] focus:outline-none"
                  />
                ) : (
                  <>
                    <div className="flex items-center gap-2.5 min-w-0 flex-1 pr-1">
                      <MessageSquare className={`w-3.5 h-3.5 shrink-0 ${isSelected ? 'text-indigo-600 dark:text-indigo-400' : 'text-[var(--muted)]'}`} />
                      <span className="truncate">{conv.title || '無標題對話'}</span>
                    </div>

                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition shrink-0">
                      <button
                        onClick={(e) => startRename(conv, e)}
                        className="p-1 text-[var(--muted)] hover:text-indigo-600 rounded transition cursor-pointer"
                        title="重新命名標題"
                      >
                        <Edit2 className="w-3 h-3" />
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setDeleteModalId(conv.id);
                        }}
                        className="p-1 text-[var(--muted)] hover:text-red-500 rounded transition cursor-pointer"
                        title="刪除紀錄"
                      >
                        <Trash2 className="w-3 h-3" />
                      </button>
                    </div>
                  </>
                )}
              </div>
            );
          })
        )}
      </div>

      {/* Footer Info */}
      <div className="p-2.5 border-t border-[var(--border)] text-[11px] text-[var(--muted)] flex items-center justify-between">
        <span>歷史紀錄：{conversations.length} 筆</span>
        <span className="text-emerald-600 dark:text-emerald-400 font-mono">全部已載入</span>
      </div>

      {/* Delete Confirmation Modal (Option A) */}
      {deleteModalId && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-[var(--card)] border border-[var(--border)] rounded-2xl max-w-sm w-full p-5 shadow-xl space-y-4">
            <div className="flex items-center gap-2 text-red-500">
              <AlertTriangle className="w-5 h-5" />
              <h3 className="font-bold text-sm text-[var(--foreground)]">確認刪除對話</h3>
            </div>
            <p className="text-xs text-[var(--muted)] leading-relaxed">
              確定要刪除這筆歷史紀錄嗎？此動作將會一併移除該次修飾與所有追加問答，且無法復原。
            </p>
            <div className="flex items-center justify-end gap-2 pt-2 border-t border-[var(--border)]">
              <button
                onClick={() => setDeleteModalId(null)}
                className="px-3 py-1.5 rounded-xl text-xs border border-[var(--border)] hover:bg-[var(--background)] transition cursor-pointer"
              >
                取消
              </button>
              <button
                onClick={() => {
                  onDeleteConversation(deleteModalId);
                  setDeleteModalId(null);
                }}
                className="px-3.5 py-1.5 rounded-xl text-xs bg-red-600 hover:bg-red-700 text-white font-medium transition cursor-pointer"
              >
                確認刪除
              </button>
            </div>
          </div>
        </div>
      )}
    </aside>
  );
}
