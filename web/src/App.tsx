import React, { useState } from 'react'
import { 
  Plus, 
  Settings, 
  Info, 
  FileText, 
  ArrowRight, 
  CheckCircle2, 
  Check, 
  Home, 
  ShieldCheck, 
  BarChart2, 
  Trash2, 
  X, 
  AlertTriangle, 
  ExternalLink,
  Shield,
  Zap,
  Lock,
  RefreshCw,
  Sliders,
  ChevronRight,
  Sparkles,
  Wifi,
  Battery
} from 'lucide-react'

interface Mandate {
  id: string
  merchant: string
  umn: string
  amount: number
  frequency: 'Monthly' | 'Quarterly' | 'Annual'
  bank: string
  date: string
  status: 'ACTIVE' | 'REVOKED'
}

export default function App() {
  const [activeTab, setActiveTab] = useState<'home' | 'mandates' | 'settings'>('home')
  const [mandates, setMandates] = useState<Mandate[]>([])
  
  // Modals
  const [showDebugModal, setShowDebugModal] = useState(false)
  const [showPaywallDemo, setShowPaywallDemo] = useState(false)
  const [showRevokeGuide, setShowRevokeGuide] = useState<Mandate | null>(null)
  
  // Custom Debug Form State
  const [customMerchant, setCustomMerchant] = useState('')
  const [customAmount, setCustomAmount] = useState('')
  const [customBank, setCustomBank] = useState('HDFC Bank')
  const [customFreq, setCustomFreq] = useState<'Monthly' | 'Quarterly' | 'Annual'>('Monthly')

  // Calculate total monthly recurring liability
  const totalMonthlyLiability = mandates
    .filter(m => m.status === 'ACTIVE')
    .reduce((sum, m) => {
      if (m.frequency === 'Monthly') return sum + m.amount
      if (m.frequency === 'Quarterly') return sum + Math.round(m.amount / 3)
      if (m.frequency === 'Annual') return sum + Math.round(m.amount / 12)
      return sum
    }, 0)

  // Quick Inject Presets
  const injectPreset = (preset: { merchant: string; amount: number; frequency: 'Monthly' | 'Quarterly' | 'Annual'; bank: string }) => {
    const randomUmn = `UMN:${preset.merchant.slice(0, 3).toUpperCase()}${Math.floor(100000 + Math.random() * 900000)}@${preset.bank.split(' ')[0].toLowerCase()}`
    const newMandate: Mandate = {
      id: Date.now().toString(),
      merchant: preset.merchant,
      umn: randomUmn,
      amount: preset.amount,
      frequency: preset.frequency,
      bank: preset.bank,
      date: new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }),
      status: 'ACTIVE'
    }
    setMandates(prev => [newMandate, ...prev])
    setShowDebugModal(false)
  }

  const handleCustomInject = (e: React.FormEvent) => {
    e.preventDefault()
    if (!customMerchant || !customAmount) return
    const randomUmn = `UMN:${customMerchant.slice(0, 3).toUpperCase()}${Math.floor(100000 + Math.random() * 900000)}@${customBank.split(' ')[0].toLowerCase()}`
    const newMandate: Mandate = {
      id: Date.now().toString(),
      merchant: customMerchant,
      umn: randomUmn,
      amount: parseFloat(customAmount),
      frequency: customFreq,
      bank: customBank,
      date: new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }),
      status: 'ACTIVE'
    }
    setMandates(prev => [newMandate, ...prev])
    setCustomMerchant('')
    setCustomAmount('')
    setShowDebugModal(false)
  }

  const deleteMandate = (id: string) => {
    setMandates(prev => prev.filter(m => m.id !== id))
  }

  const markRevoked = (id: string) => {
    setMandates(prev => prev.map(m => m.id === id ? { ...m, status: 'REVOKED' } : m))
    setShowRevokeGuide(null)
  }

  return (
    <div className="min-h-screen bg-black text-white flex flex-col items-center justify-start p-2 sm:p-6 relative overflow-x-hidden bg-dot-grid selection:bg-[#FF2A54] selection:text-white">
      
      {/* Background ambient refraction lights */}
      <div className="absolute top-1/6 -left-36 w-80 h-80 bg-rose-600/10 rounded-full blur-[140px] pointer-events-none" />
      <div className="absolute top-1/2 -right-36 w-80 h-80 bg-blue-600/10 rounded-full blur-[140px] pointer-events-none" />

      {/* Main Container - Phone Mockup matching Reference Image */}
      <main className="w-full max-w-[420px] rounded-[44px] bg-[#07080D]/90 border border-white/10 shadow-[0_25px_80px_rgba(0,0,0,0.95)] p-5 pb-6 flex flex-col relative z-10 backdrop-blur-2xl">
        
        {/* Top Status Bar (9:41, Icons) */}
        <div className="flex items-center justify-between pt-1 pb-3 px-2 text-xs font-semibold tracking-tight text-white">
          <span>9:41</span>
          <div className="flex items-center gap-2">
            {/* Cellular Signal 4 bars */}
            <div className="flex items-end gap-0.5 h-3">
              <span className="w-0.5 h-1 bg-white rounded-full" />
              <span className="w-0.5 h-1.5 bg-white rounded-full" />
              <span className="w-0.5 h-2 bg-white rounded-full" />
              <span className="w-0.5 h-2.5 bg-white rounded-full" />
            </div>
            {/* Wifi Icon */}
            <Wifi className="size-3.5" />
            {/* Battery Pill with 100 */}
            <div className="flex items-center justify-center px-1.5 py-0.5 rounded-full bg-white text-black font-extrabold text-[9px] leading-none">
              100
            </div>
          </div>
        </div>

        {/* Header: Title, Subtitle, Debug Inject & Settings */}
        <header className="flex items-center justify-between pt-2 pb-5 px-1">
          <div className="flex flex-col">
            <h1 className="text-2xl font-extrabold tracking-tight flex items-center">
              <span className="text-white">SUB</span>
              <span className="text-[#818CF8]">ZERO</span>
            </h1>
            <span className="text-xs text-zinc-400 font-medium tracking-wide">
              On-device UPI & Paywall Audit
            </span>
          </div>

          <div className="flex items-center gap-2">
            {/* + Debug Inject Button */}
            <button 
              onClick={() => setShowDebugModal(true)}
              className="px-3.5 py-2 rounded-2xl liquid-glass-pill liquid-glass-interactive flex items-center gap-1.5 text-xs font-semibold text-zinc-200"
            >
              <Plus className="size-3.5 text-zinc-300" />
              <span>Debug Inject</span>
            </button>

            {/* Settings Gear Button */}
            <button 
              onClick={() => setActiveTab(activeTab === 'settings' ? 'home' : 'settings')}
              className={`size-10 rounded-2xl liquid-glass-pill liquid-glass-interactive flex items-center justify-center text-zinc-300 ${activeTab === 'settings' ? 'border-[#818CF8]/60 bg-white/15' : ''}`}
              aria-label="Settings"
            >
              <Settings className="size-4" />
            </button>
          </div>
        </header>

        {activeTab === 'home' && (
          <div className="flex flex-col gap-4">
            
            {/* CARD 1: Total Monthly Recurring Liability (Pink Glowing Card) */}
            <section className="p-5 rounded-3xl liquid-glass-liability flex flex-col justify-between relative overflow-hidden">
              {/* Header inside card */}
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-[10.5px] font-bold tracking-wider uppercase text-zinc-400 block mb-1">
                    TOTAL MONTHLY RECURRING LIABILITY
                  </span>
                  <div className="flex items-baseline gap-1 mt-1">
                    <span className="text-4xl font-extrabold text-white tracking-tight">
                      ₹{totalMonthlyLiability.toLocaleString('en-IN')}
                    </span>
                    <span className="text-sm font-medium text-zinc-400">
                      / month
                    </span>
                  </div>
                </div>

                {/* Right Analytics Icon Badge */}
                <div className="size-11 rounded-2xl bg-gradient-to-b from-[#ff2a54]/30 to-[#9f1239]/40 border border-pink-500/40 flex items-center justify-center shadow-[0_0_15px_rgba(255,42,84,0.3)]">
                  <BarChart2 className="size-5 text-[#ff4b72]" />
                </div>
              </div>

              {/* Info Pill Banner */}
              <div className="mt-5 p-3 rounded-2xl bg-black/40 border border-white/5 flex items-center gap-3 backdrop-blur-md">
                <div className="size-7 rounded-full bg-[#ff2a54] flex items-center justify-center shrink-0 shadow-[0_0_10px_rgba(255,42,84,0.5)]">
                  <span className="text-white font-serif font-bold text-xs italic">i</span>
                </div>
                <span className="text-[11.5px] text-zinc-300 font-medium leading-snug">
                  Aggregated locally from registered UPI AutoPay e-mandates
                </span>
              </div>
            </section>

            {/* CARD 2: Live Paywall Demo (Blue Glowing Card) */}
            <section className="p-4 rounded-3xl liquid-glass-demo flex items-center justify-between">
              <div className="flex items-center gap-3.5">
                {/* Left Document Icon Badge */}
                <div className="size-11 rounded-2xl bg-gradient-to-b from-blue-500/30 to-blue-700/40 border border-blue-400/40 flex items-center justify-center shrink-0 shadow-[0_0_15px_rgba(59,130,246,0.3)]">
                  <FileText className="size-5 text-blue-300" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-white">Live Paywall Demo</h3>
                  <p className="text-[11px] text-zinc-400 font-medium">
                    Test on-device AI detection with DocuScan Pro
                  </p>
                </div>
              </div>

              {/* Launch Mock -> Button */}
              <button 
                onClick={() => setShowPaywallDemo(true)}
                className="px-4 py-2.5 rounded-2xl liquid-glass-launch-btn text-xs font-bold text-white flex items-center gap-1.5 shrink-0"
              >
                <span>Launch Mock</span>
                <ArrowRight className="size-3.5" />
              </button>
            </section>

            {/* SECTION HEADER: Audited Mandates & 100% Local Storage */}
            <div className="flex items-center justify-between px-1 pt-1">
              <span className="text-xs font-bold tracking-wider uppercase text-zinc-300">
                AUDITED MANDATES ({mandates.length})
              </span>
              <div className="flex items-center gap-1.5 text-xs font-semibold text-[#00E599]">
                <CheckCircle2 className="size-4 text-[#00E599]" />
                <span>100% LOCAL STORAGE</span>
              </div>
            </div>

            {/* CARD 3: Empty State or Mandates List */}
            <section className="rounded-3xl liquid-glass-mandates p-6 flex flex-col items-center justify-center min-h-[220px]">
              {mandates.length === 0 ? (
                /* Empty State (Pixel-matched with Reference Image) */
                <div className="flex flex-col items-center text-center py-2">
                  {/* Glowing Teal Circular Badge with Checkmark */}
                  <div className="size-16 rounded-full liquid-glass-badge-teal flex items-center justify-center mb-4">
                    <Check className="size-8 text-[#5EEAD4] stroke-[3.5]" />
                  </div>

                  <h3 className="text-base font-bold text-white mb-1.5">
                    No Active Mandates Detected
                  </h3>
                  <p className="text-xs text-zinc-400 max-w-[280px] leading-relaxed mb-5">
                    SubZero monitors bank & UPI AutoPay notifications locally on device. No mandate records found yet.
                  </p>

                  {/* Inject Demo Mandate Button */}
                  <button 
                    onClick={() => injectPreset({ merchant: 'Netflix Premium', amount: 649, frequency: 'Monthly', bank: 'HDFC Bank' })}
                    className="px-5 py-2.5 rounded-full liquid-glass-pill liquid-glass-interactive flex items-center gap-2 text-xs font-semibold text-zinc-200"
                  >
                    <Plus className="size-3.5 text-zinc-300" />
                    <span>Inject Demo Mandate</span>
                  </button>
                </div>
              ) : (
                /* Populated Mandate List */
                <div className="w-full flex flex-col gap-3">
                  {mandates.map(mandate => (
                    <div 
                      key={mandate.id}
                      className="p-3.5 rounded-2xl bg-black/40 border border-white/10 flex justify-between items-center hover:border-white/20 transition backdrop-blur-md"
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <h4 className="text-sm font-bold text-white">{mandate.merchant}</h4>
                          <span className={`text-[9px] px-2 py-0.5 rounded-full font-bold uppercase ${
                            mandate.status === 'ACTIVE' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-zinc-500/20 text-zinc-400'
                          }`}>
                            {mandate.status}
                          </span>
                        </div>
                        <p className="text-[11px] text-zinc-400 font-mono mt-0.5">{mandate.umn}</p>
                        <span className="text-[10px] text-zinc-500">{mandate.bank} • Added {mandate.date}</span>
                      </div>

                      <div className="flex flex-col items-end gap-1.5">
                        <div className="text-right">
                          <span className="text-sm font-extrabold text-white">₹{mandate.amount}</span>
                          <span className="text-[10px] text-zinc-400 block capitalize">/{mandate.frequency}</span>
                        </div>
                        
                        <div className="flex items-center gap-1.5">
                          {mandate.status === 'ACTIVE' && (
                            <button 
                              onClick={() => setShowRevokeGuide(mandate)}
                              className="px-2 py-1 rounded-lg bg-[#FF2A54]/20 hover:bg-[#FF2A54]/30 border border-[#FF2A54]/40 text-[10px] font-bold text-[#FF4B72] transition"
                            >
                              Revoke
                            </button>
                          )}
                          <button 
                            onClick={() => deleteMandate(mandate.id)}
                            className="p-1 rounded-lg bg-white/5 hover:bg-white/10 text-zinc-400 hover:text-rose-400 transition"
                            aria-label="Delete"
                          >
                            <Trash2 className="size-3.5" />
                          </button>
                        </div>
                      </div>
                    </div>
                  ))}

                  <button 
                    onClick={() => setShowDebugModal(true)}
                    className="w-full py-2.5 mt-1 rounded-2xl liquid-glass-pill liquid-glass-interactive text-xs font-semibold text-zinc-300 flex items-center justify-center gap-1.5"
                  >
                    <Plus className="size-3.5" />
                    <span>Add Another Mandate</span>
                  </button>
                </div>
              )}
            </section>

          </div>
        )}

        {activeTab === 'mandates' && (
          <section className="flex flex-col gap-4 py-1">
            <div className="flex justify-between items-center px-1">
              <div>
                <h2 className="text-base font-bold text-white">Mandate Management</h2>
                <p className="text-xs text-zinc-400">Offline UPI e-mandate directory</p>
              </div>
              <button 
                onClick={() => setShowDebugModal(true)}
                className="px-3 py-1.5 rounded-xl liquid-glass-pill text-xs font-semibold text-zinc-200 flex items-center gap-1"
              >
                <Plus className="size-3" /> Add
              </button>
            </div>

            {mandates.length === 0 ? (
              <div className="p-8 rounded-3xl liquid-glass-mandates text-center flex flex-col items-center">
                <FileText className="size-10 text-zinc-600 mb-2" />
                <p className="text-sm font-semibold text-zinc-300">No Mandates Found</p>
                <p className="text-xs text-zinc-500 mb-4">Use Debug Inject to simulate bank notification intake</p>
                <button 
                  onClick={() => injectPreset({ merchant: 'Spotify Individual', amount: 119, frequency: 'Monthly', bank: 'SBI' })}
                  className="px-4 py-2 rounded-full liquid-glass-pill text-xs font-semibold text-zinc-200"
                >
                  Inject Spotify Mandate
                </button>
              </div>
            ) : (
              <div className="flex flex-col gap-2.5">
                {mandates.map(mandate => (
                  <div 
                    key={mandate.id}
                    className="p-4 rounded-2xl bg-black/50 border border-white/10 flex justify-between items-center backdrop-blur-md"
                  >
                    <div>
                      <h4 className="text-sm font-bold text-white">{mandate.merchant}</h4>
                      <p className="text-xs text-zinc-400 font-mono">{mandate.umn}</p>
                      <p className="text-[11px] text-zinc-500 mt-1">Bank: {mandate.bank}</p>
                    </div>
                    <div className="text-right flex flex-col items-end gap-2">
                      <div>
                        <span className="text-base font-extrabold text-[#FFD000]">₹{mandate.amount}</span>
                        <span className="text-[10px] text-zinc-400 block">{mandate.frequency}</span>
                      </div>
                      {mandate.status === 'ACTIVE' ? (
                        <button 
                          onClick={() => setShowRevokeGuide(mandate)}
                          className="px-3 py-1 rounded-xl bg-[#FF2A54]/20 border border-[#FF2A54]/40 text-xs font-bold text-[#FF4B72]"
                        >
                          Revoke
                        </button>
                      ) : (
                        <span className="text-[11px] text-zinc-500 font-semibold">Revoked</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        )}

        {activeTab === 'settings' && (
          <section className="flex flex-col gap-4 py-1">
            <div className="px-1">
              <h2 className="text-base font-bold text-white">Zero-Cloud Security</h2>
              <p className="text-xs text-zinc-400">Offline privacy policies & guarantees</p>
            </div>

            {/* Zero Network Guarantee Card */}
            <div className="p-4 rounded-2xl liquid-glass-pill flex items-start gap-3">
              <ShieldCheck className="size-6 text-emerald-400 shrink-0 mt-0.5" />
              <div>
                <h4 className="text-sm font-bold text-white">0 Network Permissions</h4>
                <p className="text-xs text-zinc-400 leading-relaxed mt-0.5">
                  SubZero contains no internet permissions in its manifest and cannot transmit any telemetry, SMS content, or screen text to any cloud servers.
                </p>
              </div>
            </div>

            {/* Banking Shield Card */}
            <div className="p-4 rounded-2xl liquid-glass-pill flex items-start gap-3">
              <Lock className="size-6 text-blue-400 shrink-0 mt-0.5" />
              <div>
                <h4 className="text-sm font-bold text-white">Fail-Closed Banking Shield</h4>
                <p className="text-xs text-zinc-400 leading-relaxed mt-0.5">
                  PhonePe, GPay, Paytm, BHIM, and net banking apps are strictly excluded from screen scraping and text inspection.
                </p>
              </div>
            </div>

            {/* On-Device AI Engine */}
            <div className="p-4 rounded-2xl liquid-glass-pill flex items-start gap-3">
              <Zap className="size-6 text-yellow-400 shrink-0 mt-0.5" />
              <div>
                <h4 className="text-sm font-bold text-white">Offline Pattern Analysis</h4>
                <p className="text-xs text-zinc-400 leading-relaxed mt-0.5">
                  SubZero uses an on-device quantized model engine with a 400ms circuit breaker timeout for instantaneous paywall detection.
                </p>
              </div>
            </div>

            {/* Data reset */}
            <button 
              onClick={() => { setMandates([]); setActiveTab('home'); }}
              className="w-full py-3 rounded-2xl bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 text-xs font-bold text-rose-400 transition"
            >
              Reset Mandate Database
            </button>
          </section>
        )}

        {/* Bottom Floating Navigation Dock (Pixel matched to Reference Image) */}
        <footer className="mt-5 p-1.5 rounded-full liquid-glass-dock grid grid-cols-3 gap-1">
          {/* Home Tab */}
          <button 
            onClick={() => setActiveTab('home')}
            className={`py-2 px-3 rounded-full flex flex-col items-center justify-center gap-1 transition-all ${
              activeTab === 'home' 
                ? 'bg-gradient-to-b from-[#ff2a54]/25 to-[#ff2a54]/10 border border-[#ff2a54]/30 text-[#ff4b72] shadow-[0_0_15px_rgba(255,42,84,0.2)]' 
                : 'text-zinc-400 hover:text-zinc-200'
            }`}
          >
            <Home className="size-4" />
            <span className="text-[10px] font-bold">Home</span>
          </button>

          {/* Mandates Tab */}
          <button 
            onClick={() => setActiveTab('mandates')}
            className={`py-2 px-3 rounded-full flex flex-col items-center justify-center gap-1 transition-all ${
              activeTab === 'mandates' 
                ? 'bg-gradient-to-b from-blue-500/25 to-blue-500/10 border border-blue-500/30 text-blue-400 shadow-[0_0_15px_rgba(59,130,246,0.2)]' 
                : 'text-zinc-400 hover:text-zinc-200'
            }`}
          >
            <FileText className="size-4" />
            <span className="text-[10px] font-bold">Mandates</span>
          </button>

          {/* Settings Tab */}
          <button 
            onClick={() => setActiveTab('settings')}
            className={`py-2 px-3 rounded-full flex flex-col items-center justify-center gap-1 transition-all ${
              activeTab === 'settings' 
                ? 'bg-gradient-to-b from-purple-500/25 to-purple-500/10 border border-purple-500/30 text-purple-400 shadow-[0_0_15px_rgba(168,85,247,0.2)]' 
                : 'text-zinc-400 hover:text-zinc-200'
            }`}
          >
            <Shield className="size-4" />
            <span className="text-[10px] font-bold">Settings</span>
          </button>
        </footer>

      </main>

      {/* ========================================================================= */}
      {/* MODAL 1: DEBUG INJECTOR MODAL */}
      {/* ========================================================================= */}
      {showDebugModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="w-full max-w-[380px] rounded-3xl liquid-glass-card p-5 border border-white/20 shadow-2xl relative animate-in fade-in zoom-in duration-200">
            <div className="flex justify-between items-center mb-4">
              <div className="flex items-center gap-2">
                <Plus className="size-4 text-[#FF2A54]" />
                <h3 className="text-sm font-bold text-white">Debug Mandate Injector</h3>
              </div>
              <button 
                onClick={() => setShowDebugModal(false)}
                className="size-7 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-zinc-400 hover:text-white"
              >
                <X className="size-4" />
              </button>
            </div>

            <p className="text-xs text-zinc-400 mb-3">
              Simulate incoming SMS and UPI AutoPay bank notifications parsed offline.
            </p>

            {/* Quick Presets */}
            <div className="flex flex-col gap-2 mb-4">
              <span className="text-[10px] font-bold uppercase tracking-wider text-zinc-500">Quick Presets</span>
              <div className="grid grid-cols-2 gap-2">
                <button 
                  onClick={() => injectPreset({ merchant: 'Netflix Premium', amount: 649, frequency: 'Monthly', bank: 'HDFC Bank' })}
                  className="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-left transition"
                >
                  <p className="text-xs font-bold text-white">Netflix Premium</p>
                  <p className="text-[10px] text-yellow-400 font-mono">₹649 /mo • HDFC</p>
                </button>
                <button 
                  onClick={() => injectPreset({ merchant: 'Spotify Individual', amount: 119, frequency: 'Monthly', bank: 'SBI' })}
                  className="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-left transition"
                >
                  <p className="text-xs font-bold text-white">Spotify Individual</p>
                  <p className="text-[10px] text-yellow-400 font-mono">₹119 /mo • SBI</p>
                </button>
                <button 
                  onClick={() => injectPreset({ merchant: 'DocuScan Pro', amount: 899, frequency: 'Annual', bank: 'ICICI Bank' })}
                  className="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-left transition"
                >
                  <p className="text-xs font-bold text-white">DocuScan Pro</p>
                  <p className="text-[10px] text-yellow-400 font-mono">₹899 /yr • ICICI</p>
                </button>
                <button 
                  onClick={() => injectPreset({ merchant: 'Cult.Fit Elite', amount: 2499, frequency: 'Monthly', bank: 'Axis Bank' })}
                  className="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-left transition"
                >
                  <p className="text-xs font-bold text-white">Cult.Fit Elite</p>
                  <p className="text-[10px] text-yellow-400 font-mono">₹2,499 /mo • Axis</p>
                </button>
              </div>
            </div>

            {/* Custom Input Form */}
            <form onSubmit={handleCustomInject} className="flex flex-col gap-2.5 pt-2 border-t border-white/10">
              <span className="text-[10px] font-bold uppercase tracking-wider text-zinc-500">Custom Mandate</span>
              <input 
                type="text" 
                placeholder="Merchant Name (e.g. Amazon Prime)" 
                value={customMerchant}
                onChange={e => setCustomMerchant(e.target.value)}
                className="px-3 py-2 rounded-xl bg-black/50 border border-white/10 text-xs text-white placeholder:text-zinc-500 focus:outline-none focus:border-[#FF2A54]"
                required
              />
              <div className="grid grid-cols-2 gap-2">
                <input 
                  type="number" 
                  placeholder="Amount (₹)" 
                  value={customAmount}
                  onChange={e => setCustomAmount(e.target.value)}
                  className="px-3 py-2 rounded-xl bg-black/50 border border-white/10 text-xs text-white placeholder:text-zinc-500 focus:outline-none focus:border-[#FF2A54]"
                  required
                />
                <select 
                  value={customFreq}
                  onChange={e => setCustomFreq(e.target.value as any)}
                  className="px-3 py-2 rounded-xl bg-black/50 border border-white/10 text-xs text-white focus:outline-none focus:border-[#FF2A54]"
                >
                  <option value="Monthly">Monthly</option>
                  <option value="Quarterly">Quarterly</option>
                  <option value="Annual">Annual</option>
                </select>
              </div>
              <button 
                type="submit"
                className="w-full py-2.5 rounded-xl liquid-glass-launch-btn text-xs font-bold text-white mt-1"
              >
                Inject Custom Mandate
              </button>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 2: LIVE PAYWALL DEMO (DOCUSCAN PRO + ON-DEVICE AI HUD) */}
      {/* ========================================================================= */}
      {showPaywallDemo && (
        <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-lg flex items-center justify-center p-3">
          <div className="w-full max-w-[400px] rounded-[38px] bg-[#0c0e14] border border-white/20 shadow-2xl p-5 flex flex-col relative overflow-hidden animate-in fade-in zoom-in duration-200">
            
            {/* Close Button */}
            <button 
              onClick={() => setShowPaywallDemo(false)}
              className="absolute top-4 right-4 size-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-zinc-400 hover:text-white z-20"
            >
              <X className="size-4" />
            </button>

            {/* SUBZERO REAL-TIME ON-DEVICE AI OVERLAY HUD (Neon Red Warning) */}
            <div className="mb-4 p-3.5 rounded-2xl bg-[#FF2A54]/15 border-2 border-[#FF2A54] shadow-[0_0_25px_rgba(255,42,84,0.4)] flex flex-col gap-1.5 animate-pulse">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <AlertTriangle className="size-4 text-[#FF2A54]" />
                  <span className="text-xs font-black tracking-wider text-[#FF2A54] uppercase">
                    85/100 DECEPTIVE PAYWALL DETECTED
                  </span>
                </div>
                <span className="text-[10px] font-mono font-bold bg-[#FF2A54] text-white px-1.5 py-0.5 rounded">
                  Local AI
                </span>
              </div>
              <p className="text-[11px] text-zinc-200 font-medium">
                Warning: Hidden annual auto-charge of ₹899 triggered after 3-day trial. Cancel button is disguised.
              </p>
            </div>

            {/* MOCK DECEPTIVE APP UI: DocuScan Pro */}
            <div className="flex flex-col items-center text-center py-2">
              <div className="size-14 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center text-white mb-2 shadow-lg">
                <FileText className="size-7" />
              </div>
              <h2 className="text-lg font-extrabold text-white">DocuScan Pro Premium</h2>
              <p className="text-xs text-zinc-400 mb-4">Unlimited OCR, Cloud Sync & PDF Signing</p>

              {/* Deceptive Pricing Cards */}
              <div className="w-full flex flex-col gap-2.5 mb-4">
                {/* Pre-selected Annual Card */}
                <div className="p-3.5 rounded-2xl bg-blue-500/10 border-2 border-blue-500 flex justify-between items-center text-left relative">
                  <div className="absolute -top-2.5 right-3 bg-blue-500 text-white text-[9px] font-black px-2 py-0.5 rounded-full uppercase">
                    Best Value (Pre-Selected)
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-white">Annual VIP Plan</h4>
                    <p className="text-[10px] text-zinc-400">3-Day Free Trial, then billed ₹899/year</p>
                  </div>
                  <div className="text-right">
                    <span className="text-xs font-bold text-blue-400">₹17 /wk</span>
                    <span className="text-[9px] text-zinc-500 block">Billed annually</span>
                  </div>
                </div>

                {/* Hidden Monthly Option */}
                <div className="p-3 rounded-2xl bg-white/5 border border-white/10 flex justify-between items-center text-left opacity-75">
                  <div>
                    <h4 className="text-xs font-bold text-white">Monthly Plan</h4>
                    <p className="text-[10px] text-zinc-400">Billed ₹199 each month</p>
                  </div>
                  <span className="text-xs font-bold text-zinc-300">₹199 /mo</span>
                </div>
              </div>

              {/* Deceptive Action Button */}
              <button 
                onClick={() => {
                  injectPreset({ merchant: 'DocuScan Pro', amount: 899, frequency: 'Annual', bank: 'ICICI Bank' })
                  setShowPaywallDemo(false)
                }}
                className="w-full py-3 rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-extrabold text-xs shadow-lg flex items-center justify-center gap-2 mb-2"
              >
                <span>Start 3-Day Free Trial (Test AutoPay)</span>
                <ChevronRight className="size-4" />
              </button>

              <span className="text-[9px] text-zinc-500">
                Clicking will simulate registration of ₹899/yr mandate in SubZero
              </span>
            </div>

          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 3: ASSISTED REVOCATION GUIDE */}
      {/* ========================================================================= */}
      {showRevokeGuide && (
        <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-md flex items-center justify-center p-4">
          <div className="w-full max-w-[380px] rounded-3xl liquid-glass-card p-5 border border-white/20 shadow-2xl relative animate-in fade-in zoom-in duration-200">
            <div className="flex justify-between items-center mb-3">
              <div className="flex items-center gap-2">
                <Shield className="size-4 text-[#FF2A54]" />
                <h3 className="text-sm font-bold text-white">Assisted Revocation Guide</h3>
              </div>
              <button 
                onClick={() => setShowRevokeGuide(null)}
                className="size-7 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-zinc-400 hover:text-white"
              >
                <X className="size-4" />
              </button>
            </div>

            <div className="p-3 rounded-2xl bg-white/5 border border-white/10 mb-4">
              <h4 className="text-xs font-bold text-white">{showRevokeGuide.merchant}</h4>
              <p className="text-[11px] text-yellow-400 font-mono">Amount: ₹{showRevokeGuide.amount} / {showRevokeGuide.frequency}</p>
              <p className="text-[10px] text-zinc-400 font-mono mt-0.5">{showRevokeGuide.umn}</p>
            </div>

            <h5 className="text-xs font-bold text-zinc-300 mb-2">Step-by-Step Revocation in UPI Apps:</h5>
            <ol className="text-xs text-zinc-300 space-y-2 mb-5 list-decimal list-inside leading-relaxed">
              <li>Open your UPI App (PhonePe, Google Pay, or Paytm).</li>
              <li>Go to <strong className="text-white">Profile &gt; AutoPay / Autopay Mandates</strong>.</li>
              <li>Locate <strong className="text-white">{showRevokeGuide.merchant}</strong> with UMN matching above.</li>
              <li>Tap <strong className="text-[#FF2A54]">Pause</strong> or <strong className="text-[#FF2A54]">Revoke / Cancel Mandate</strong> and authenticate with your UPI PIN.</li>
            </ol>

            <div className="flex gap-2">
              <button 
                onClick={() => markRevoked(showRevokeGuide.id)}
                className="flex-1 py-2.5 rounded-xl bg-emerald-500/20 hover:bg-emerald-500/30 border border-emerald-500/40 text-xs font-bold text-emerald-400"
              >
                Mark as Revoked
              </button>
              <button 
                onClick={() => setShowRevokeGuide(null)}
                className="py-2.5 px-4 rounded-xl bg-white/10 hover:bg-white/20 text-xs font-medium text-zinc-300"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Footer Branding */}
      <footer className="mt-4 text-center text-xs text-zinc-600 font-mono">
        SubZero Privacy Engine • Zero-Cloud Offline UPI & Paywall Audit
      </footer>
    </div>
  )
}


