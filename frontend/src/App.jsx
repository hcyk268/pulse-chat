import AppRouter from "./routes/AppRouter";
import { ErrorBoundary } from "./components/system/ErrorBoundary";
import { AppSettingsProvider } from "./hooks/useAppSettings";
import { ChatProvider } from "./hooks/useChatStore";
import { ToastProvider } from "./hooks/useToast";

export default function App() {
  return (
    <ErrorBoundary>
      <AppSettingsProvider>
        <ToastProvider>
          <ChatProvider>
            <AppRouter />
          </ChatProvider>
        </ToastProvider>
      </AppSettingsProvider>
    </ErrorBoundary>
  );
}
