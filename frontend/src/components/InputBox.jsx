import React, { useRef, useEffect } from 'react';
import { Send, Square } from 'lucide-react';

export default function InputBox({
  input,
  setInput,
  onSend,
  onStopThinking,
  isLoading,
  isFollowUp
}) {
  const textareaRef = useRef(null);

  // Auto-expand textarea height
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      const scrollHeight = textareaRef.current.scrollHeight;
      textareaRef.current.style.height = `${Math.min(scrollHeight, 220)}px`;
    }
  }, [input]);

  const handleKeyDown = (e) => {
    // 按 Shift + Enter 觸發送出
    if (e.key === 'Enter' && e.shiftKey) {
      e.preventDefault();
      if (!isLoading && input.trim()) {
        onSend();
      }
    }
    // 普通按 Enter：瀏覽器預設跳下一行，自動延伸高度
  };

  const charLength = input.length;
  const isNearLimit = charLength >= 900;

  return (
    <div className="border-t border-[var(--border)] bg-[var(--card)] p-3 sm:p-4 shrink-0 select-none">
      <div className="max-w-4xl mx-auto">
        <div className="relative bg-[var(--background)] border border-[var(--border)] focus-within:border-indigo-500 focus-within:ring-2 focus-within:ring-indigo-500/20 rounded-2xl p-2.5 transition shadow-xs">
          {/* Expandable Textarea with 1000 limit */}
          <textarea
            ref={textareaRef}
            rows={1}
            maxLength={1000}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={isLoading}
            placeholder={
              isFollowUp
                ? "針對這篇英文修飾追加發問... (例如：為什麼不用某詞？按 Enter 換行，Shift + Enter 送出)"
                : "輸入要被修飾的英文草稿... (按 Enter 換行，Shift + Enter 送出)"
            }
            className="w-full bg-transparent border-0 focus:outline-none resize-none text-[var(--foreground)] text-sm leading-relaxed max-h-56 overflow-y-auto px-2 py-1 placeholder:text-[var(--muted)]"
            style={{ minHeight: '28px' }}
          />

          {/* Bottom Toolbar */}
          <div className="flex items-center justify-between pt-2 border-t border-[var(--border)]/40 mt-1 px-1">
            <div className="text-[11px] text-[var(--muted)] flex items-center gap-2">
              <span className="inline-flex items-center gap-1">
                <kbd className="px-1.5 py-0.5 rounded bg-[var(--card)] border border-[var(--border)] font-mono text-[10px]">
                  Enter
                </kbd>
                <span>換行</span>
              </span>
              <span>•</span>
              <span className="inline-flex items-center gap-1 font-medium text-[var(--foreground)]">
                <kbd className="px-1.5 py-0.5 rounded bg-[var(--card)] border border-[var(--border)] font-mono text-[10px]">
                  Shift
                </kbd>
                +
                <kbd className="px-1.5 py-0.5 rounded bg-[var(--card)] border border-[var(--border)] font-mono text-[10px]">
                  Enter
                </kbd>
                <span>送出</span>
              </span>
              <span>•</span>
              <span className={`font-mono ${isNearLimit ? 'text-amber-500 font-bold' : ''}`}>
                {charLength} / 1000 字元
              </span>
            </div>

            <div className="flex items-center gap-2">
              {/* Thinking Stop Button */}
              {isLoading && (
                <button
                  onClick={onStopThinking}
                  className="flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-xl bg-amber-500 hover:bg-amber-600 text-white transition cursor-pointer shadow-xs animate-pulse"
                >
                  <Square className="w-3 h-3 fill-current" />
                  <span>停止生成</span>
                </button>
              )}

              {/* Submit Button */}
              <button
                onClick={onSend}
                disabled={isLoading || !input.trim()}
                className={`flex items-center gap-1.5 text-xs font-semibold px-4 py-1.5 rounded-xl transition cursor-pointer shadow-xs ${
                  isLoading || !input.trim()
                    ? 'bg-indigo-500/30 text-white/50 cursor-not-allowed'
                    : 'bg-indigo-600 hover:bg-indigo-700 active:scale-95 text-white'
                }`}
              >
                <span>{isFollowUp ? '追加發問' : '送出修飾'}</span>
                <Send className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        {/* Small guideline hint */}
        <div className="text-center mt-2 text-[11px] text-[var(--muted)]">
          {isFollowUp
            ? "💡 目前處於同 Session 上下文，追加問題直接攜帶脈絡向 AI 提問討論。"
            : "大原則：一次新對話只能使用一次英文文本修飾，送出後可持續深入討論。"}
        </div>
      </div>
    </div>
  );
}
