import { useEffect, useRef, useState } from 'react';
import { AudioLines, Mic, Send, X } from 'lucide-react';

interface VoiceModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmitIdea: (text: string) => void;
}

interface SpeechRecognitionInstance {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  onresult: ((ev: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void) | null;
  onerror: (() => void) | null;
  onend: (() => void) | null;
  start: () => void;
  stop: () => void;
}

export function VoiceModal({ isOpen, onClose, onSubmitIdea }: VoiceModalProps) {
  const [transcript, setTranscript] = useState('');
  const [listening, setListening] = useState(false);
  const [speechSupported, setSpeechSupported] = useState(true);
  const recognitionRef = useRef<SpeechRecognitionInstance | null>(null);

  useEffect(() => {
    if (!isOpen) {
      recognitionRef.current?.stop();
      setListening(false);
      return;
    }

    const win = window as unknown as {
      SpeechRecognition?: new () => SpeechRecognitionInstance;
      webkitSpeechRecognition?: new () => SpeechRecognitionInstance;
    };
    const SpeechCtor = win.SpeechRecognition || win.webkitSpeechRecognition;

    if (!SpeechCtor) {
      setSpeechSupported(false);
      return;
    }

    setSpeechSupported(true);
    const rec = new SpeechCtor();
    rec.continuous = true;
    rec.interimResults = true;
    rec.lang = 'en-US';
    rec.onresult = (event) => {
      let text = '';
      for (let i = 0; i < event.results.length; i++) {
        text += event.results[i][0]?.transcript ?? '';
      }
      setTranscript(text.trim());
    };
    rec.onerror = () => setListening(false);
    rec.onend = () => setListening(false);
    recognitionRef.current = rec;

    try {
      rec.start();
      setListening(true);
    } catch {
      setListening(false);
    }

    return () => {
      try {
        rec.stop();
      } catch {
        // ignore
      }
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const toggleListening = () => {
    const rec = recognitionRef.current;
    if (!rec) return;
    if (listening) {
      rec.stop();
      setListening(false);
    } else {
      try {
        rec.start();
        setListening(true);
      } catch {
        setListening(false);
      }
    }
  };

  const handleSend = () => {
    const trimmed = transcript.trim();
    if (!trimmed) return;
    recognitionRef.current?.stop();
    onSubmitIdea(trimmed);
    setTranscript('');
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/40 backdrop-blur-[1px]"
      role="dialog"
      aria-modal="true"
      aria-label="AI Voice Idea Capture"
    >
      <div className="w-full sm:max-w-md bg-white rounded-t-3xl sm:rounded-3xl border border-[#E4E4E7] p-6 space-y-6 shadow-2xl">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AudioLines className="w-5 h-5 text-[#3B82F6]" aria-hidden="true" />
            <h2 className="text-base font-semibold text-[#0D0D0D]">
              AI Voice Mode
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close voice mode"
            className="w-10 h-10 rounded-full hover:bg-[#F4F4F5] flex items-center justify-center text-[#52525B]"
          >
            <X className="w-5 h-5" aria-hidden="true" />
          </button>
        </div>

        <div className="flex flex-col items-center text-center space-y-4 py-2">
          <button
            type="button"
            onClick={toggleListening}
            aria-label={listening ? 'Pause microphone' : 'Start microphone'}
            className={`w-20 h-20 rounded-full flex items-center justify-center transition-all ${
              listening
                ? 'bg-[#3B82F6] text-white shadow-[0_0_0_10px_rgba(59,130,246,0.16)]'
                : 'bg-[#EFF6FF] text-[#3B82F6] hover:bg-[#DBEAFE]'
            }`}
          >
            <Mic className="w-8 h-8" aria-hidden="true" />
          </button>

          <p className="text-sm text-[#52525B]">
            {listening
              ? 'Listening... Describe the blocks, items, or mobs you want.'
              : speechSupported
              ? 'Tap the microphone to speak your mod idea, or type below.'
              : 'Voice recognition is unavailable in this browser. Type your idea below.'}
          </p>

          <textarea
            rows={3}
            value={transcript}
            onChange={(e) => setTranscript(e.target.value)}
            placeholder="e.g. Make a Bedrock add-on with lightning spears and storm elementals..."
            aria-label="Spoken or typed mod idea"
            className="w-full rounded-2xl border border-[#E4E4E7] bg-[#FAFAFA] p-3 text-sm text-[#0D0D0D] placeholder:text-[#8E8E93] focus:outline-none focus:border-[#3B82F6] resize-none"
          />
        </div>

        <div className="flex items-center justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="min-h-[42px] px-4 rounded-xl border border-[#E4E4E7] hover:bg-[#F4F4F5] text-sm font-medium text-[#3F3F46]"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleSend}
            disabled={!transcript.trim()}
            className="min-h-[42px] px-4 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] disabled:opacity-50 text-white text-sm font-medium inline-flex items-center gap-1.5"
          >
            <Send className="w-4 h-4" aria-hidden="true" />
            <span>Generate Mod</span>
          </button>
        </div>
      </div>
    </div>
  );
}
