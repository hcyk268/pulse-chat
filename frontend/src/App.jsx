import AppRouter from "./routes/AppRouter";
import { AppSettingsProvider } from "./hooks/useAppSettings";
import { ChatProvider } from "./hooks/useChatStore";
import { ToastProvider } from "./hooks/useToast";

export default function App() {
  return (
    <AppSettingsProvider>
      <ToastProvider>
        <ChatProvider>
          <AppRouter />
        </ChatProvider>
      </ToastProvider>
    </AppSettingsProvider>
  );
}
