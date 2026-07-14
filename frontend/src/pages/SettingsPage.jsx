import {
  ArrowLeft,
  Check,
  Eye,
  Gauge,
  Keyboard,
  MessageCircle,
  RotateCcw,
  Settings2,
  Sparkles,
  UsersRound,
} from "lucide-react";
import { Link } from "react-router-dom";
import { useAppSettings } from "../hooks/useAppSettings";

const DENSITY_OPTIONS = [
  { label: "Comfortable", value: "comfortable" },
  { label: "Compact", value: "compact" },
];

export default function SettingsPage() {
  const { resetSettings, settings, updateSetting } = useAppSettings();

  return (
    <main className="settings-page h-[100dvh] overflow-y-auto bg-[#0a0f1a] text-white">
      <div className="pointer-events-none fixed inset-0 overflow-hidden" aria-hidden="true">
        <div className="absolute left-[12%] top-[-12rem] h-96 w-96 rounded-full bg-indigo-500/10 blur-3xl" />
        <div className="absolute bottom-[-14rem] right-[8%] h-[28rem] w-[28rem] rounded-full bg-purple-500/10 blur-3xl" />
      </div>

      <div className="relative mx-auto w-full max-w-5xl px-4 py-5 sm:px-6 sm:py-8 lg:px-8">
        <header className="flex items-center justify-between gap-4">
          <Link
            to="/chat"
            className="press flex min-h-11 items-center gap-2 rounded-xl border border-white/[0.06] bg-white/[0.025] px-3.5 text-sm font-semibold text-slate-300 hover:bg-white/[0.05] hover:text-white"
          >
            <ArrowLeft size={18} />
            Back to chat
          </Link>
          <span className="hidden items-center gap-2 text-xs font-medium text-emerald-300 sm:flex">
            <span className="h-2 w-2 rounded-full bg-emerald-400" />
            Saved automatically
          </span>
        </header>

        <section className="mt-8 border-b border-white/[0.06] pb-7 sm:mt-10 sm:pb-9">
          <div className="flex items-start gap-4">
            <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 to-purple-600 text-white shadow-send">
              <Settings2 size={23} />
            </span>
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.18em] text-indigo-300">
                App preferences
              </p>
              <h1 className="mt-2 text-3xl font-bold tracking-[-0.035em] text-white sm:text-4xl">
                Settings
              </h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-400 sm:text-[15px]">
                Tune the interface and messaging behavior to match how you use Pulse.
                Changes are applied immediately on this device.
              </p>
            </div>
          </div>
        </section>

        <div className="mt-6 grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_280px]">
          <div className="space-y-6">
            <SettingsSection
              description="Control spacing and animation across the application."
              icon={Sparkles}
              title="Appearance"
            >
              <SettingRow
                control={
                  <div className="grid w-full grid-cols-2 rounded-xl border border-white/[0.06] bg-[#0a0f1a] p-1 sm:w-auto">
                    {DENSITY_OPTIONS.map((option) => {
                      const active = settings.density === option.value;
                      return (
                        <button
                          key={option.value}
                          type="button"
                          aria-pressed={active}
                          onClick={() => updateSetting("density", option.value)}
                          className={[
                            "press min-h-10 rounded-lg px-3 text-xs font-semibold transition-colors sm:min-w-28",
                            active
                              ? "bg-indigo-500 text-white shadow-[0_6px_18px_rgba(99,102,241,0.25)]"
                              : "text-slate-400 hover:bg-white/[0.04] hover:text-white",
                          ].join(" ")}
                        >
                          {option.label}
                        </button>
                      );
                    })}
                  </div>
                }
                description="Choose between relaxed spacing and a denser conversation list."
                icon={Gauge}
                title="Interface density"
              />
              <SettingRow
                control={
                  <SettingToggle
                    checked={settings.reduceMotion}
                    label="Reduce interface motion"
                    onChange={(checked) => updateSetting("reduceMotion", checked)}
                  />
                }
                description="Minimize transitions, floating effects and animated feedback."
                icon={Sparkles}
                title="Reduce motion"
              />
            </SettingsSection>

            <SettingsSection
              description="Adjust how messages are composed and displayed."
              icon={MessageCircle}
              title="Chat behavior"
            >
              <SettingRow
                control={
                  <SettingToggle
                    checked={settings.enterToSend}
                    label="Use Enter to send messages"
                    onChange={(checked) => updateSetting("enterToSend", checked)}
                  />
                }
                description={
                  settings.enterToSend
                    ? "Enter sends. Use Shift + Enter for a new line."
                    : "Enter creates a new line. Use Ctrl or Cmd + Enter to send."
                }
                icon={Keyboard}
                title="Enter to send"
              />
              <SettingRow
                control={
                  <SettingToggle
                    checked={settings.showMessagePreviews}
                    label="Show conversation previews"
                    onChange={(checked) => updateSetting("showMessagePreviews", checked)}
                  />
                }
                description="Show the latest message under each conversation name."
                icon={Eye}
                title="Message previews"
              />
              <SettingRow
                control={
                  <SettingToggle
                    checked={settings.showOnlineStatus}
                    label="Show online indicators"
                    onChange={(checked) => updateSetting("showOnlineStatus", checked)}
                  />
                }
                description="Display presence dots and the online count in chat."
                icon={UsersRound}
                title="Online status"
              />
            </SettingsSection>
          </div>

          <aside className="space-y-4 lg:sticky lg:top-8">
            <div className="rounded-2xl border border-white/[0.07] bg-[#111827]/90 p-5 shadow-panel-soft backdrop-blur-xl">
              <p className="text-sm font-semibold text-white">Current preferences</p>
              <div className="mt-4 space-y-3 text-sm">
                <SummaryItem label="Density" value={settings.density === "compact" ? "Compact" : "Comfortable"} />
                <SummaryItem label="Enter key" value={settings.enterToSend ? "Send" : "New line"} />
                <SummaryItem label="Previews" value={settings.showMessagePreviews ? "Visible" : "Hidden"} />
                <SummaryItem label="Presence" value={settings.showOnlineStatus ? "Visible" : "Hidden"} />
              </div>
            </div>

            <div className="rounded-2xl border border-white/[0.07] bg-[#111827]/70 p-5">
              <p className="text-sm font-semibold text-white">Reset preferences</p>
              <p className="mt-2 text-xs leading-5 text-slate-500">
                Restore the original interface and chat behavior. Account data is not affected.
              </p>
              <button
                type="button"
                onClick={resetSettings}
                className="press mt-4 flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-white/[0.07] bg-white/[0.025] px-4 text-sm font-semibold text-slate-300 hover:bg-white/[0.06] hover:text-white"
              >
                <RotateCcw size={17} />
                Restore defaults
              </button>
            </div>

            <p className="px-1 text-xs leading-5 text-slate-600">
              Preferences are stored locally in this browser.
            </p>
          </aside>
        </div>
      </div>
    </main>
  );
}

function SettingsSection({ children, description, icon: Icon, title }) {
  return (
    <section className="overflow-hidden rounded-2xl border border-white/[0.07] bg-[#111827]/90 shadow-panel-soft backdrop-blur-xl">
      <header className="flex items-start gap-3 border-b border-white/[0.06] px-4 py-4 sm:px-5">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-indigo-500/10 text-indigo-300">
          <Icon size={19} />
        </span>
        <div>
          <h2 className="font-semibold text-white">{title}</h2>
          <p className="mt-1 text-xs leading-5 text-slate-500">{description}</p>
        </div>
      </header>
      <div className="divide-y divide-white/[0.05]">{children}</div>
    </section>
  );
}

function SettingRow({ control, description, icon: Icon, title }) {
  return (
    <div className="flex flex-col gap-4 px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-5">
      <div className="flex min-w-0 items-start gap-3">
        <Icon size={18} className="mt-0.5 shrink-0 text-slate-500" />
        <div>
          <h3 className="text-sm font-semibold text-slate-100">{title}</h3>
          <p className="mt-1 max-w-md text-xs leading-5 text-slate-500">{description}</p>
        </div>
      </div>
      <div className="shrink-0 pl-0 sm:pl-4">{control}</div>
    </div>
  );
}

function SettingToggle({ checked, label, onChange }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={() => onChange(!checked)}
      className={[
        "press relative flex h-7 w-12 items-center rounded-full border transition-colors",
        checked
          ? "border-indigo-400/60 bg-indigo-500"
          : "border-white/10 bg-slate-800 hover:bg-slate-700",
      ].join(" ")}
    >
      <span
        className={[
          "flex h-5 w-5 items-center justify-center rounded-full bg-white text-indigo-600 shadow-md transition-transform duration-200",
          checked ? "translate-x-[22px]" : "translate-x-[3px]",
        ].join(" ")}
      >
        {checked ? <Check size={12} strokeWidth={3} /> : null}
      </span>
    </button>
  );
}

function SummaryItem({ label, value }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <span className="text-slate-500">{label}</span>
      <span className="font-medium text-slate-200">{value}</span>
    </div>
  );
}
